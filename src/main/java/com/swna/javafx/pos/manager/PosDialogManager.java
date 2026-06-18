package com.swna.javafx.pos.manager;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import org.springframework.stereotype.Component;

import com.swna.javafx.pos.dialog.BalanceDialogController;
import com.swna.javafx.pos.dialog.BarcodeDialogController;
import com.swna.javafx.pos.dialog.CashDialogController;
import com.swna.javafx.pos.dialog.CashoutDialogController;
import com.swna.javafx.pos.dialog.CancelDialogController;
import com.swna.javafx.pos.dialog.CreditDialogController;
import com.swna.javafx.pos.dialog.ItemDiscountDialogController;
import com.swna.javafx.pos.dialog.ItemPriceChangeDialogController;
import com.swna.javafx.pos.dialog.ItemUnregisteredDialogController;
import com.swna.javafx.pos.dialog.PrintReceiptDialogController;
import com.swna.javafx.pos.dialog.ReceiptDialogController;
import com.swna.javafx.pos.dialog.VolumeDiscountDialogController;
import com.swna.javafx.pos.functional.TriConsumer;
import com.swna.javafx.pos.model.PosItem;
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
public class PosDialogManager {

    private final FxWeaver fxWeaver;

    /**
     * 현금 결제 다이얼로그 표시
     */
    public void showCashDialog(PosViewModel viewModel, Consumer<DialogResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(DialogResult.failure("No items to pay"));
            return;
        }

        showDialog(CashDialogController.class, controller ->
            controller.initData(total, discount, 
                // 이 람다식이 CashDialogController의 onPaymentComplete 필드에 저장됨
                receivedCash -> {
                    // ★ 이 코드가 나중에 실행됨 ★
                    viewModel.processCashPayment(total, receivedCash, success -> {
                        if (Boolean.TRUE.equals(success)) {
                            callback.accept(DialogResult.success("Change: " + receivedCash.subtract(total)));
                        } else {
                            callback.accept(DialogResult.failure("Payment failed"));
                        }
                    });
                }
            )
        );
    }

    public void showCancelDialog(PosViewModel viewModel, Consumer<DialogResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(DialogResult.failure("No items to pay"));
            return;
        }

        showDialog(CancelDialogController.class, controller ->
            controller.initData(total, receivedCash -> 
                viewModel.processCancelPayment(total, receivedCash, success -> {
                    if (Boolean.TRUE.equals(success)) {
                        DialogResult result = DialogResult.success("Change: " + receivedCash.subtract(total));
                        callback.accept(result);
                    } else {
                        callback.accept(DialogResult.failure("Payment failed"));
                    }
                })
            )
        );
    }

    /**
     * 카드/현금 혼합 결제 다이얼로그 표시 (TriConsumer 사용)
     */
    public void showCreditDialog(PosViewModel viewModel, Consumer<DialogResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(DialogResult.failure("No items to pay"));
            return;
        }

        // TriConsumer로 3개의 파라미터(cash, card, cardNumber)를 모두 전달
        TriConsumer<BigDecimal, BigDecimal, String> paymentHandler = (cashPart, creditPart, cardNumber) -> {
            log.info("[PaymentDialogManager] Processing mixed payment - cash: ${}, credit: ${}, cardNumber: {}", 
                cashPart, creditPart, cardNumber);
            
            viewModel.processMixedPayment(cashPart, creditPart, cardNumber, success -> {
                if (Boolean.TRUE.equals(success)) {
                    DialogResult result = DialogResult.success(
                        String.format("Cash: $%.2f, Credit: $%.2f, Card: %s", 
                            cashPart, creditPart, maskCardNumber(cardNumber))
                    );
                    callback.accept(result);
                } else {
                    callback.accept(DialogResult.failure("Mixed payment failed"));
                }
            });
        };

        showDialog(CreditDialogController.class, controller ->
            controller.initData(total, discount, paymentHandler)
        );
    }

    /**
     * 현금 인출(Cashout) 결제 다이얼로그 표시
     */
    public void showCashoutDialog(PosViewModel viewModel, Consumer<DialogResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(DialogResult.failure("No items to pay for cashout"));
            return;
        }

        showDialog(CashoutDialogController.class, controller ->
            controller.initData(total, discount, (cashoutAmount, totalCredit, cardNumber) -> 
                viewModel.processCashoutPayment(cashoutAmount, totalCredit, cardNumber, success -> {
                    if (Boolean.TRUE.equals(success)) {
                        DialogResult result = DialogResult.success(
                            String.format("EFTPOS: $%.2f, Cashout: $%.2f, Card: %s", 
                                totalCredit, cashoutAmount, maskCardNumber(cardNumber))
                        );
                        callback.accept(result);
                    } else {
                        callback.accept(DialogResult.failure("Cashout failed"));
                    }
                })
            )
        );
    }

    /**
     * 할인 적용 다이얼로그 표시
     */
    public void showDiscountDialog(PosItem item, java.util.function.DoubleConsumer onDiscount, Runnable onFinish) {
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
    public void showPriceChangeDialog(PosItem item, java.util.function.DoubleConsumer onPriceChange, Runnable onFinish) {
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

    // PaymentDialogManager.java (추가할 부분)

    /**
     * 볼륨 할인 다이얼로그 표시
     */
    public void showVolumeDiscountDialog(PosViewModel viewModel, Consumer<DialogResult> callback) {
        BigDecimal total = viewModel.calculateActualTotal();
        @SuppressWarnings("unused")
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());
        
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(DialogResult.failure("No items to apply discount"));
            return;
        }
        
        showDialog(VolumeDiscountDialogController.class, controller -> {
            controller.initData(total, (amountAfterDC, percent) -> {
                if (amountAfterDC != null) {
                    // 금액 기준 할인
                    viewModel.applyVolumeDiscountByAmount(amountAfterDC, success -> {
                        if (Boolean.TRUE.equals(success)) {
                            callback.accept(DialogResult.success(
                                String.format("Amount discounted to: $%.2f", amountAfterDC)
                            ));
                        } else {
                            callback.accept(DialogResult.failure("Failed to apply amount discount"));
                        }
                    });
                } else if (percent != null) {
                    // 퍼센트 기준 할인
                    viewModel.applyVolumeDiscountByPercent(percent, success -> {
                        if (Boolean.TRUE.equals(success)) {
                            callback.accept(DialogResult.success(
                                String.format("%.2f%% discount applied", percent)
                            ));
                        } else {
                            callback.accept(DialogResult.failure("Failed to apply percent discount"));
                        }
                    });
                } else {
                    callback.accept(DialogResult.failure("Invalid discount parameters"));
                }
            });
        });
    }

    
    /**
     * 영수증 번호 검색 다이얼로그 표시
     */
    public void showReceiptDialog(Consumer<String> callback) {
        showDialog(ReceiptDialogController.class, controller ->
            controller.initData(receiptNumber -> {
                log.info("[PaymentDialogManager] Receipt search: {}", receiptNumber);
                if (callback != null) {
                    callback.accept(receiptNumber);
                }
            })
        );
    }

    /**
     * 바코드 번호 검색 다이얼로그 표시
     */
    public void showBarcodeDialog(Consumer<String> callback) {
        showDialog(BarcodeDialogController.class, controller ->
            controller.initData(barcodeNumber -> {
                log.info("[PaymentDialogManager] Barcode search: {}", barcodeNumber);
                if (callback != null) {
                    callback.accept(barcodeNumber);
                }
            })
        );
    }

    /**
     * 영수증 출력/검색 다이얼로그 표시
     */
    public void showPrintReceiptDialog(PrintReceiptDialogController.PrintReceiptCallback callback) {
        showDialog(PrintReceiptDialogController.class, controller -> {
            controller.initData(callback);
        });
    }

    /**
     * 미등록 상품 수동 등록 다이얼로그 표시
     */
    public void showManualRegisterDialog(String barcode, DoubleConsumer onRegister) {
        showDialog(ItemUnregisteredDialogController.class, controller -> {
            controller.initUnregisteredItem(barcode, amount -> {
                if (onRegister != null) {
                    onRegister.accept(amount);
                }
            });
        });
    }

    /**
     * 카드번호 마스킹 처리 (로깅용)
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * 잔액 확인 및 영수증 선택 다이얼로그 표시
     */
    public void showBalanceDialog(BigDecimal balance, Consumer<BalanceDialogController.BalanceResult> callback) {
        showDialog(BalanceDialogController.class, controller -> {
            controller.initData(balance, callback);
        });
        
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
    public static class DialogResult {
        boolean success;
        String message;
        
        public static DialogResult success(String message) {
            return new DialogResult(true, message);
        }
        
        public static DialogResult failure(String message) {
            return new DialogResult(false, message);
        }
    }
}