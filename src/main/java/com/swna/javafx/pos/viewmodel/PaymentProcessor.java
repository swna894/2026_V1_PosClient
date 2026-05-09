// PaymentProcessor.java
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

    // ========== Public API ==========

    /**
     * 현금 결제 처리
     */
    public void processCashPayment(BigDecimal totalAmount, BigDecimal receivedCash,
                                   Consumer<Boolean> onComplete,
                                   PaymentResultHandler resultHandler) {
        ValidationResult validation = validateCashPayment(totalAmount, receivedCash);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        BigDecimal change = receivedCash.subtract(totalAmount);
        String successMessage = "Cash payment success. Change: " + change;

        executePayment(
                List.of(new PaymentRequest(PAY_CASH, totalAmount, receivedCash, BigDecimal.ZERO, null)),
                PAYMENT_DESC_CASH,
                successMessage,
                onComplete,
                resultHandler
        );
    }

    /**
     * 현금 인출 결제 처리
     */
    public void processCashoutPayment(BigDecimal cashoutAmount, BigDecimal totalCardAmount,
                                      Consumer<Boolean> onComplete,
                                      PaymentResultHandler resultHandler) {
        ValidationResult validation = validateCashoutPayment(cashoutAmount);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        BigDecimal finalCardAmount = resolveCardAmount(totalCardAmount);
        String refNo = CASHOUT_REF_PREFIX + System.currentTimeMillis();

        executePayment(
                List.of(new PaymentRequest(PAY_CARD, finalCardAmount, finalCardAmount, cashoutAmount, refNo)),
                PAYMENT_DESC_CASHOUT,
                "Cashout payment success",
                onComplete,
                resultHandler
        );
    }

    /**
     * 혼합 결제 처리
     */
    public void processMixedPayment(BigDecimal cashPart, BigDecimal creditPart,
                                    Consumer<Boolean> onComplete,
                                    PaymentResultHandler resultHandler) {
        ValidationResult validation = validateMixedPayment(cashPart, creditPart);
        if (!validation.isValid()) {
            resultHandler.onFailure(validation.getErrorMessage());
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        List<PaymentRequest> payments = buildMixedPayments(cashPart, creditPart);

        executePayment(
                payments,
                PAYMENT_DESC_MIXED,
                "Mixed payment success",
                onComplete,
                resultHandler
        );
    }

    /**
     * 할인 적용된 최종 결제 금액 계산
     */
    public BigDecimal getTotalAfterDiscount() {
        BigDecimal total = BigDecimal.valueOf(cartManager.totalAmountProperty().get());
        BigDecimal disc = BigDecimal.valueOf(cartManager.totalDiscountProperty().get());
        return total.subtract(disc);
    }

    // ========== Private Validation Methods ==========

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

    private void executePayment(List<PaymentRequest> payments,
                                String paymentType,
                                String successMessage,
                                Consumer<Boolean> onComplete,
                                PaymentResultHandler resultHandler) {
        SaleRequest saleRequest = buildSaleRequest(payments);

        paymentService.executePayment(saleRequest, paymentType)
                .subscribe(result -> {
                    boolean success = resultHandler.handleResult(result, successMessage);
                    if (onComplete != null) {
                        onComplete.accept(success);
                    }
                });
    }

    private SaleRequest buildSaleRequest(List<PaymentRequest> payments) {
        List<SaleItemRequest> itemRequests = toItemRequests(cartManager.getItems());
        List<DiscountRequest> discounts = buildDiscountRequests();

        return new SaleRequest(itemRequests, payments, discounts);
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

    // ========== Inner Interface ==========

    @FunctionalInterface
    public interface PaymentResultHandler {
        /**
         * 결제 결과 처리
         * @param result 결제 결과
         * @param successMessage 성공 시 로그 메시지
         * @return 성공 여부
         */
        boolean handleResult(PaymentResult result, String successMessage);

        /**
         * 결제 실패 시 호출 (유효성 검증 실패 등)
         * @param message 실패 메시지
         */
        default void onFailure(String message) {
            log.warn("[PaymentProcessor] Payment failed: {}", message);
        }
    }
}