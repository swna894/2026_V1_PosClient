package com.swna.javafx.pos.viewmodel;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.DiscountRequest;
import com.swna.javafx.pos.dto.request.DiscountType;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.service.PaymentResult;
import com.swna.javafx.pos.service.PaymentService;
import com.swna.javafx.pos.viewmodel.manager.CartManager;
import javafx.collections.ObservableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.swna.javafx.pos.viewmodel.PosViewModelConstants.*;

@Slf4j
@RequiredArgsConstructor
public class PaymentProcessor {

    private final CartManager cartManager;
    private final PaymentService paymentService;

    // ========== 결과 객체 ==========
    
    /**
     * 결제 처리 결과를 담는 레코드
     * @param success 성공 여부
     * @param saleRequest 실제 결제에 사용된 SaleRequest (payments 포함)
     * @param paymentResult PaymentService로부터 받은 결제 결과
     */
    public record ProcessedPayment(
        boolean success,
        SaleRequest saleRequest,
        PaymentResult paymentResult
    ) {
        public boolean isSuccess() {
            return success;
        }
        
        public String getReceiptNo() {
            return paymentResult != null && paymentResult.getSaleResponse() != null 
                ? paymentResult.getSaleResponse().receiptNo() 
                : null;
        }
        
        public String getErrorMessage() {
            if (!success && paymentResult != null) {
                return paymentResult.getMessage();
            }
            return null;
        }
    }

    // ========== Public API ==========

    /**
     * 현금 결제 처리 (SaleRequest 함께 반환)
     */
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash,
                                   Consumer<ProcessedPayment> onComplete,
                                   PaymentResultHandler resultHandler) {
        ValidationResult validation = validateCashPayment(totalAmount, receivedCash);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) {
                onComplete.accept(new ProcessedPayment(false, null, null));
            }
            return;
        }

        BigDecimal change = receivedCash.subtract(totalAmount);
        String successMessage = "Cash payment success. Change: " + change;
        
        // 결제 전 현재 장바구니 상태로 SaleRequest 생성
        SaleRequest saleRequest = buildCurrentSaleRequest();

        executePaymentWithRequest(
                saleRequest,
                List.of(new PaymentRequest(PAY_CASH, totalAmount, receivedCash, BigDecimal.ZERO, null)),
                PAYMENT_DESC_CASH,
                successMessage,
                onComplete,
                resultHandler
        );
    }

    /**
     * 현금 인출 결제 처리 (SaleRequest 함께 반환)
     */
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      Consumer<ProcessedPayment> onComplete,
                                      PaymentResultHandler resultHandler) {
        ValidationResult validation = validateCashoutPayment(cashoutAmount);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) {
                onComplete.accept(new ProcessedPayment(false, null, null));
            }
            return;
        }

        BigDecimal finalCardAmount = resolveCardAmount(totalCardAmount);
        String refNo = CASHOUT_REF_PREFIX + System.currentTimeMillis();
        
        // 결제 전 현재 장바구니 상태로 SaleRequest 생성
        SaleRequest saleRequest = buildCurrentSaleRequest();

        executePaymentWithRequest(
                saleRequest,
                List.of(new PaymentRequest(PAY_CARD, finalCardAmount, finalCardAmount, cashoutAmount, refNo)),
                PAYMENT_DESC_CASHOUT,
                "Cashout payment success",
                onComplete,
                resultHandler
        );
    }

    /**
     * 혼합 결제 처리 (SaleRequest 함께 반환)
     */
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart,
                                    Consumer<ProcessedPayment> onComplete,
                                    PaymentResultHandler resultHandler) {

                                             // 디버깅 로그: 현재 상태 출력
        log.info("=== MIXED PAYMENT DEBUG ===");
        log.info("Cash part: {}", cashPart);
        log.info("Credit part: {}", creditPart);
        log.info("Cart total amount: {}", cartManager.totalAmountProperty().get());
        log.info("Cart total discount: {}", cartManager.totalDiscountProperty().get());
        log.info("Total after discount: {}", getTotalAfterDiscount());
        log.info("Cart is empty: {}", cartManager.isEmpty());
        log.info("============================");

        ValidationResult validation = validateMixedPayment(cashPart, creditPart);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) {
                onComplete.accept(new ProcessedPayment(false, null, null));
            }
            return;
        }

        List<PaymentRequest> payments = buildMixedPayments(cashPart, creditPart);
        
        // 결제 전 현재 장바구니 상태로 SaleRequest 생성
        SaleRequest saleRequest = buildCurrentSaleRequest();

        executePaymentWithRequest(
                saleRequest,
                payments,
                PAYMENT_DESC_MIXED,
                "Mixed payment success",
                onComplete,
                resultHandler
        );
    }

    /**
     * 현재 장바구니 상태로 SaleRequest 생성 (payments 제외)
     */
    public SaleRequest getCurrentSaleRequest() {
        return buildCurrentSaleRequest();
    }

    /**
     * 할인 적용된 최종 결제 금액 계산
     */
    public BigDecimal getTotalAfterDiscount() {
        return BigDecimal.valueOf(cartManager.totalAmountProperty().get());
    }

    // ========== Private Validation Methods ==========
    // ValidationResult 클래스는 com.swna.javafx.pos.viewmodel.ValidationResult 사용

    private ValidationResult validateCashPayment(BigDecimal totalAmount, BigDecimal receivedCash) {
        if (receivedCash == null || totalAmount == null) {
            return ValidationResult.invalid(STATUS_INVALID_AMOUNT);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid(STATUS_INVALID_AMOUNT);
        }

        if (receivedCash.subtract(totalAmount).compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.invalid(STATUS_INSUFFICIENT_CASH);
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateCashoutPayment(BigDecimal cashoutAmount) {
        if (cashoutAmount == null) {
            return ValidationResult.invalid("Cashout amount cannot be null");
        }

        if (cashoutAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.invalid("Cashout amount cannot be negative");
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateMixedPayment(BigDecimal cashPart, BigDecimal creditPart) {
        if (cashPart == null || creditPart == null) {
            return ValidationResult.invalid("Payment parts cannot be null");
        }

        if (cashPart.compareTo(BigDecimal.ZERO) < 0 || creditPart.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.invalid("Payment amounts cannot be negative");
        }

        BigDecimal totalPayment = cashPart.add(creditPart);
        BigDecimal originalTotalAmount = getTotalAfterDiscount();

        if (totalPayment.compareTo(originalTotalAmount) != 0) {
            return ValidationResult.invalid(STATUS_AMOUNT_MISMATCH);
        }

        if (cashPart.compareTo(BigDecimal.ZERO) == 0 && creditPart.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.invalid("At least one payment method required");
        }

        return ValidationResult.valid();
    }

    // ========== Private Helper Methods ==========

    private BigDecimal resolveCardAmount(BigDecimal totalCardAmount) {
        if (totalCardAmount == null || totalCardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return getTotalAfterDiscount();
        }
        return totalCardAmount;
    }

    private List<PaymentRequest> buildMixedPayments(BigDecimal cashPart, BigDecimal creditPart) {
        List<PaymentRequest> payments = new ArrayList<>();

        if (cashPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest(PAY_CASH, cashPart, cashPart, BigDecimal.ZERO, null));
        }

        if (creditPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest(PAY_CARD, creditPart, creditPart, BigDecimal.ZERO,
                    "CREDIT_" + System.currentTimeMillis()));
        }

        return payments;
    }

    /**
     * 현재 장바구니 상태로 SaleRequest 생성 (payments는 null)
     */
    private SaleRequest buildCurrentSaleRequest() {
        List<SaleItemRequest> itemRequests = toItemRequests(cartManager.getItems());
        List<DiscountRequest> discounts = buildDiscountRequests();
        return new SaleRequest(itemRequests, null, discounts);
    }

    private List<DiscountRequest> buildDiscountRequests() {
        BigDecimal totalDiscount = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());

        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        return List.of(DiscountRequest.fixed(totalDiscount, "Cart total discount"));
    }

    private List<SaleItemRequest> toItemRequests(ObservableList<PosItem> items) {
        return items.stream()
                .map(item -> new SaleItemRequest(
                        item.getBarcode(),
                        item.getQty(),
                        BigDecimal.valueOf(item.getOriginalPrice()),
                        BigDecimal.valueOf(item.getSellingPrice()),
                        BigDecimal.valueOf(item.getUnitDiscount()),
                        item.getUnitDiscount() > 0 ? item.getDiscountType() : DiscountType.NONE,
                        Objects.requireNonNullElse(item.getComment(), "")
                ))
                .toList();
    }

    /**
     * 결제 실행 (SaleRequest를 함께 전달하는 버전)
     * 중요: 성공/실패 모두 finalSaleRequest(payments 포함)를 전달
     */
    private void executePaymentWithRequest(SaleRequest baseSaleRequest,
                                           List<PaymentRequest> payments,
                                           String paymentType,
                                           String successMessage,
                                           Consumer<ProcessedPayment> onComplete,
                                           PaymentResultHandler resultHandler) {
        // payments를 포함한 최종 SaleRequest 생성
        SaleRequest finalSaleRequest = new SaleRequest(
            baseSaleRequest.items(), 
            payments, 
            baseSaleRequest.discounts()
        );

        paymentService.executePayment(finalSaleRequest, paymentType)
                .subscribe(result -> {
                    boolean success = result.isSuccess();
                    
                    if (success) {
                        log.info("[PaymentProcessor] {}", successMessage);
                        resultHandler.handleSuccess(successMessage);
                    } else {
                        log.warn("[PaymentProcessor] Payment failed: {}", result.getMessage());
                        resultHandler.handleFailure(result.getMessage());
                    }
                    
                    if (onComplete != null) {
                        // 중요: 성공/실패 모두 finalSaleRequest 사용 (payments 포함)
                        ProcessedPayment processed = new ProcessedPayment(
                            success,
                            finalSaleRequest,  // payments가 포함된 SaleRequest
                            result
                        );
                        onComplete.accept(processed);
                    }
                });
    }

    // ========== Inner Interface ==========

    /**
     * 결제 결과 핸들러 인터페이스
     */
    public interface PaymentResultHandler {
        /**
         * 결제 성공 시 호출
         */
        default void handleSuccess(String successMessage) {
            log.info("[PaymentProcessor] Payment success: {}", successMessage);
        }
        
        /**
         * 결제 실패 시 호출 (PaymentService 레벨 실패)
         */
        default void handleFailure(String errorMessage) {
            log.warn("[PaymentProcessor] Payment failed: {}", errorMessage);
        }
        
        /**
         * 유효성 검증 실패 시 호출
         */
        default void onFailure(String message) {
            log.warn("[PaymentProcessor] Validation failed: {}", message);
        }
    }
}