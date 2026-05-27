package com.swna.javafx.pos.viewmodel;

import com.swna.javafx.pos.api.PosApiService;
import com.swna.javafx.pos.dto.request.DiscountRequest;
import com.swna.javafx.pos.dto.request.DiscountType;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.model.PosItem;
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
public class PosProcessor {

    private final CartManager cartManager;
    private final PosApiService posApiService;

    // ========== Result Record ==========
    
    public record ProcessedPayment(
        boolean success,
        SaleRequest saleRequest,
        PaymentResult paymentResult
    ) {
        public String getReceiptNo() {
            return paymentResult != null && paymentResult.getSaleResponse() != null 
                ? paymentResult.getSaleResponse().receiptNo() 
                : null;
        }
        
        public String getErrorMessage() {
            return (success || paymentResult == null) ? null : paymentResult.getMessage();
        }
    }

    // ========== Public API ==========

    /**
     * 현금 결제 처리
     */
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash,
                                   Consumer<ProcessedPayment> onComplete,
                                   PaymentResultHandler resultHandler) {
        validateCashPayment(totalAmount, receivedCash)
            .ifInvalid(error -> {
                resultHandler.onFailure(error);
                notifyComplete(onComplete, false, null, null);
                return;
            });

        BigDecimal change = receivedCash.subtract(totalAmount);
        PaymentRequest payment = PaymentRequestBuilder.cash(totalAmount, receivedCash);
        
        executePayment(
            List.of(payment),
            PAYMENT_DESC_CASH,
            "Cash payment success. Change: " + change,
            onComplete,
            resultHandler
        );
    }

    /**
     * 현금 인출 결제 처리 (카드번호 포함)
     */
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      String cardNumber,
                                      Consumer<ProcessedPayment> onComplete,
                                      PaymentResultHandler resultHandler) {
        validateCashoutPayment(cashoutAmount)
            .ifInvalid(error -> {
                resultHandler.onFailure(error);
                notifyComplete(onComplete, false, null, null);
                return;
            });

        BigDecimal finalCardAmount = resolveCardAmount(totalCardAmount);
        String refNo = CASHOUT_REF_PREFIX + System.currentTimeMillis();
        PaymentRequest payment = PaymentRequestBuilder.cashout(finalCardAmount, cashoutAmount, refNo, cardNumber);
        
        executePayment(
            List.of(payment),
            PAYMENT_DESC_CASHOUT,
            "Cashout payment success",
            onComplete,
            resultHandler
        );
    }

    /**
     * 카드 결제 처리 (순수 카드 결제)
     */
    public void processCreditPayment(BigDecimal cardAmount, String cardNumber,
                                     Consumer<ProcessedPayment> onComplete,
                                     PaymentResultHandler resultHandler) {
        validateCreditPayment(cardAmount)
            .ifInvalid(error -> {
                resultHandler.onFailure(error);
                notifyComplete(onComplete, false, null, null);
                return;
            });

        PaymentRequest payment = PaymentRequestBuilder.credit(cardAmount, cardNumber);
        
        executePayment(
            List.of(payment),
            PAYMENT_DESC_CREDIT,
            "Credit payment success",
            onComplete,
            resultHandler
        );
    }

    /**
     * 혼합 결제 처리 (현금 + 카드)
     */
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart, String cardNumber,
                                    Consumer<ProcessedPayment> onComplete,
                                    PaymentResultHandler resultHandler) {
        logMixedPaymentDebug(cashPart, creditPart);
        log.info("[PaymentProcessor] Processing mixed payment with cardNumber: {}", cardNumber);
        
        validateMixedPayment(cashPart, creditPart)
            .ifInvalid(error -> {
                resultHandler.onFailure(error);
                notifyComplete(onComplete, false, null, null);
                return;
            });

        List<PaymentRequest> payments = buildMixedPayments(cashPart, creditPart, cardNumber);
        
        executePayment(
            payments,
            PAYMENT_DESC_MIXED,
            "Mixed payment success",
            onComplete,
            resultHandler
        );
    }
    /**
     * 현재 장바구니로 SaleRequest 생성 (payments 제외)
     */
    public SaleRequest getCurrentSaleRequest() {
        return buildCurrentSaleRequest();
    }

    /**
     * 할인 적용된 최종 결제 금액
     */
    public BigDecimal getTotalAfterDiscount() {
        return BigDecimal.valueOf(cartManager.totalAmountProperty().get());
    }

    // ========== Payment Request Builders ==========
    
    private static final class PaymentRequestBuilder {
        static PaymentRequest cash(BigDecimal amount, BigDecimal received) {
            return new PaymentRequest(PAY_CASH, amount, received, BigDecimal.ZERO, null, null);
        }
        
        static PaymentRequest credit(BigDecimal amount, String cardNumber) {
            return new PaymentRequest(PAY_CARD, amount, amount, BigDecimal.ZERO, generateRefNo("CREDIT"), cardNumber);
        }
        
        static PaymentRequest cashout(BigDecimal amount, BigDecimal cashoutAmount, String refNo, String cardNumber) {
            return new PaymentRequest(PAY_CASHOUT, amount, amount, cashoutAmount, refNo, cardNumber);
        }
        
        private static String generateRefNo(String prefix) {
            return prefix + "_" + System.currentTimeMillis();
        }
    }

    // ========== Validation ==========

    private ValidationResult validateCashPayment(BigDecimal totalAmount, BigDecimal receivedCash) {
        if (totalAmount == null || receivedCash == null) {
            return ValidationResult.invalid(STATUS_INVALID_AMOUNT);
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid(STATUS_INVALID_AMOUNT);
        }
        if (receivedCash.compareTo(totalAmount) < 0) {
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

    private ValidationResult validateCreditPayment(BigDecimal cardAmount) {
        if (cardAmount == null) {
            return ValidationResult.invalid("Card payment amount cannot be null");
        }
        if (cardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Card payment amount must be positive");
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
        BigDecimal totalAmount = getTotalAfterDiscount();
        
        if (totalPayment.compareTo(totalAmount) != 0) {
            return ValidationResult.invalid(STATUS_AMOUNT_MISMATCH);
        }
        if (cashPart.compareTo(BigDecimal.ZERO) == 0 && creditPart.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.invalid("At least one payment method required");
        }
        return ValidationResult.valid();
    }

    // ========== Private Helpers ==========

    private BigDecimal resolveCardAmount(BigDecimal totalCardAmount) {
        return (totalCardAmount == null || totalCardAmount.compareTo(BigDecimal.ZERO) <= 0) 
            ? getTotalAfterDiscount() 
            : totalCardAmount;
    }

    private List<PaymentRequest> buildMixedPayments(BigDecimal cashPart, BigDecimal creditPart, String cardNumber) {
        List<PaymentRequest> payments = new ArrayList<>();
        
        if (isPositive(cashPart)) {
            payments.add(PaymentRequestBuilder.cash(cashPart, cashPart));
        }
        if (isPositive(creditPart)) {
            payments.add(PaymentRequestBuilder.credit(creditPart, cardNumber));
        }
        
        return payments;
    }
    
    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private SaleRequest buildCurrentSaleRequest() {
        return new SaleRequest(
            toItemRequests(cartManager.getItems()),
            null,
            buildDiscountRequests()
        );
    }

    private List<DiscountRequest> buildDiscountRequests() {
        BigDecimal totalDiscount = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());
        return totalDiscount.compareTo(BigDecimal.ZERO) <= 0 
            ? List.of() 
            : List.of(DiscountRequest.fixed(totalDiscount, "Cart total discount"));
    }

    private List<SaleItemRequest> toItemRequests(ObservableList<PosItem> items) {
        return items.stream()
            .map(this::toSaleItemRequest)
            .toList();
    }

    private SaleItemRequest toSaleItemRequest(PosItem item) {
        return new SaleItemRequest(
            item.getBarcode(),
            item.getQty(),
            BigDecimal.valueOf(item.getOriginalPrice()),
            BigDecimal.valueOf(item.getSellingPrice()),
            BigDecimal.valueOf(item.getUnitDiscount()),
            item.getUnitDiscount() > 0 ? item.getDiscountType() : DiscountType.NONE,
            Objects.requireNonNullElse(item.getComment(), "")
        );
    }

    private void executePayment(List<PaymentRequest> payments,
                                String paymentType,
                                String successMessage,
                                Consumer<ProcessedPayment> onComplete,
                                PaymentResultHandler resultHandler) {
        SaleRequest finalSaleRequest = new SaleRequest(
            buildCurrentSaleRequest().items(),
            payments,
            buildCurrentSaleRequest().discounts()
        );

        posApiService.executePayment(finalSaleRequest, paymentType)
            .subscribe(result -> handlePaymentResult(result, finalSaleRequest, successMessage, onComplete, resultHandler));
    }

    private void handlePaymentResult(PaymentResult result,
                                     SaleRequest saleRequest,
                                     String successMessage,
                                     Consumer<ProcessedPayment> onComplete,
                                     PaymentResultHandler resultHandler) {
        boolean success = result.isSuccess();
        
        if (success) {
            log.info("[PaymentProcessor] {}", successMessage);
            resultHandler.handleSuccess(successMessage);
        } else {
            log.warn("[PaymentProcessor] Payment failed: {}", result.getMessage());
            resultHandler.handleFailure(result.getMessage());
        }
        
        notifyComplete(onComplete, success, saleRequest, result);
    }

    private void notifyComplete(Consumer<ProcessedPayment> onComplete, 
                                boolean success, 
                                SaleRequest saleRequest, 
                                PaymentResult paymentResult) {
        if (onComplete != null) {
            onComplete.accept(new ProcessedPayment(success, saleRequest, paymentResult));
        }
    }

    private void logMixedPaymentDebug(BigDecimal cashPart, BigDecimal creditPart) {
        log.info("=== MIXED PAYMENT DEBUG ===");
        log.info("Cash part: {}", cashPart);
        log.info("Credit part: {}", creditPart);
        log.info("Cart total amount: {}", cartManager.totalAmountProperty().get());
        log.info("Cart total discount: {}", cartManager.totalDiscountProperty().get());
        log.info("Total after discount: {}", getTotalAfterDiscount());
        log.info("Cart is empty: {}", cartManager.isEmpty());
        log.info("============================");
    }

    // ========== Inner Interface ==========

    public interface PaymentResultHandler {
        default void handleSuccess(String message) {
            log.info("[PaymentProcessor] Payment success: {}", message);
        }
        
        default void handleFailure(String errorMessage) {
            log.warn("[PaymentProcessor] Payment failed: {}", errorMessage);
        }
        
        default void onFailure(String message) {
            log.warn("[PaymentProcessor] Validation failed: {}", message);
        }
    }
}