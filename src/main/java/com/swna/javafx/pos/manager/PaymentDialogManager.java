package com.swna.javafx.pos.manager;

import java.math.BigDecimal;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.swna.javafx.pos.dialog.CashDialogController;
import com.swna.javafx.pos.dialog.CashoutDialogController;
import com.swna.javafx.pos.dialog.CreditDialogController;
import com.swna.javafx.pos.dialog.ItemDiscountDialogController;
import com.swna.javafx.pos.dialog.ItemPriceChangeDialogController;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.viewmodel.PosViewModel;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDialogManager {

    private final FxWeaver fxWeaver;

    /**
     * 현금 결제 다이얼로그 표시
     */
    public void showCashDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay"));
            return;
        }

        showDialog(CashDialogController.class, controller ->
            controller.initData(total, discount, receivedCash -> {
                // 비동기 결제 처리
                viewModel.processCashPayment(total, receivedCash, success -> {
                    if (success) {
                        PaymentResult result = PaymentResult.success("Change: " + receivedCash.subtract(total));
                        callback.accept(result);
                    } else {
                        callback.accept(PaymentResult.failure("Payment failed"));
                    }
                });
            })
        );
    }

    /**
     * 카드/현금 혼합 결제 다이얼로그 표시
     */
    public void showCreditDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay"));
            return;
        }

        showDialog(CreditDialogController.class, controller ->
            controller.initData(total, discount, (cashPart, creditPart) -> {
                // 비동기 결제 처리
                viewModel.processMixedPayment(cashPart, creditPart, success -> {
                    if (success) {
                        PaymentResult result = PaymentResult.success(
                            String.format("Cash: $%.2f, Credit: $%.2f", cashPart, creditPart)
                        );
                        callback.accept(result);
                    } else {
                        callback.accept(PaymentResult.failure("Mixed payment failed"));
                    }
                });
            })
        );
    }

    /**
     * 현금 인출(Cashout) 결제 다이얼로그 표시
     */
    public void showCashoutDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay for cashout"));
            return;
        }

        showDialog(CashoutDialogController.class, controller ->
            controller.initData(total, discount, (cashoutAmount, totalCredit) -> {
                // 비동기 결제 처리
                viewModel.processCashoutPayment(cashoutAmount, totalCredit, success -> {
                    if (success) {
                        PaymentResult result = PaymentResult.success(
                            String.format("EFTPOS: $%.2f, Cashout: $%.2f", totalCredit, cashoutAmount)
                        );
                        callback.accept(result);
                    } else {
                        callback.accept(PaymentResult.failure("Cashout failed"));
                    }
                });
            })
        );
    }

    /**
     * 할인 적용 다이얼로그 표시
     */
    public void showDiscountDialog(PosItem item, Consumer<Double> onDiscount, Runnable onFinish) {
        showDialog(ItemDiscountDialogController.class, controller ->
            controller.initData(item, revisedPrice -> {
                if (onDiscount != null) {
                    onDiscount.accept(revisedPrice);
                }
                if (onFinish != null) {
                    onFinish.run();
                }
            })
        );
    }

    /**
     * 가격 변경 다이얼로그 표시
     */
    public void showPriceChangeDialog(PosItem item, Consumer<Double> onPriceChange, Runnable onFinish) {
        showDialog(ItemPriceChangeDialogController.class, controller ->
            controller.initData(item, newPrice -> {
                if (onPriceChange != null) {
                    onPriceChange.accept(newPrice);
                }
                if (onFinish != null) {
                    onFinish.run();
                }
            })
        );
    }

    /**
     * 공통 다이얼로그 표시 메서드
     */
    private <T> void showDialog(Class<T> controllerClass, Consumer<T> initializer) {
        try {
            FxControllerAndView<T, Parent> dialog = fxWeaver.load(controllerClass);
            dialog.getView().ifPresent(view -> {
                if (initializer != null) {
                    initializer.accept(dialog.getController());
                }
                
                Stage stage = new Stage();
                stage.setScene(new Scene(view));
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
            });
        } catch (Exception e) {
            log.error("[Dialog] Failed to show dialog: {}", controllerClass.getSimpleName(), e);
        }
    }

    // ========== PaymentResult Inner Class ==========
    
    /**
     * 결제 결과를 담는 내부 클래스
     */
    @lombok.Value
    public static class PaymentResult {
        boolean success;
        String message;
        
        public static PaymentResult success(String message) {
            return new PaymentResult(true, message);
        }
        
        public static PaymentResult failure(String message) {
            return new PaymentResult(false, message);
        }
    }
}