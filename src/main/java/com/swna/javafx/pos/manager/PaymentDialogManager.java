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
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;

@Component
@RequiredArgsConstructor
public class PaymentDialogManager {

    private final FxWeaver fxWeaver;

    public void showCashDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay"));
            return;
        }

        showDialog(CashDialogController.class, controller ->
            controller.initData(total, discount, receivedCash -> {
                boolean success = viewModel.processCashPayment(total, receivedCash);
                callback.accept(success ?
                    PaymentResult.success("Change: " + receivedCash.subtract(total)) :
                    PaymentResult.failure("Payment failed"));
            })
        );
    }

    public void showCreditDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay"));
            return;
        }

        showDialog(CreditDialogController.class, controller ->
            controller.initData(total, discount, (cashPart, creditPart) -> {
                boolean success = viewModel.processMixedPayment(cashPart, creditPart);
                callback.accept(success ?
                    PaymentResult.success(String.format("Cash: $%.2f, Credit: $%.2f", cashPart, creditPart)) :
                    PaymentResult.failure("Mixed payment failed"));
            })
        );
    }

    public void showCashoutDialog(PosViewModel viewModel, Consumer<PaymentResult> callback) {
        BigDecimal total = BigDecimal.valueOf(viewModel.totalAmountProperty().get());
        BigDecimal discount = BigDecimal.valueOf(viewModel.discountProperty().get());

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            callback.accept(PaymentResult.failure("No items to pay for cashout"));
            return;
        }

        showDialog(CashoutDialogController.class, controller ->
            controller.initData(total, discount, (cashoutAmount, totalCredit) -> {
                boolean success = viewModel.processCashoutPayment(cashoutAmount, totalCredit);
                callback.accept(success ?
                    PaymentResult.success(String.format("EFTPOS: $%.2f, Cashout: $%.2f", totalCredit, cashoutAmount)) :
                    PaymentResult.failure("Cashout failed"));
            })
        );
    }

    public void showDiscountDialog(PosItem item, Consumer<Double> onDiscount, Runnable onFinish) {
        showDialog(ItemDiscountDialogController.class, controller ->
            controller.initData(item, revisedPrice -> {
                onDiscount.accept(revisedPrice);
                onFinish.run();
            })
        );
    }

    public void showPriceChangeDialog(PosItem item, Consumer<Double> onPriceChange, Runnable onFinish) {
        showDialog(ItemPriceChangeDialogController.class, controller ->
            controller.initData(item, newPrice -> {
                onPriceChange.accept(newPrice);
                onFinish.run();
            })
        );
    }

    private <T> void showDialog(Class<T> controllerClass, Consumer<T> initializer) {
        FxControllerAndView<T, Parent> dialog = fxWeaver.load(controllerClass);
        dialog.getView().ifPresent(view -> {
            initializer.accept(dialog.getController());
            Stage stage = new Stage();
            stage.setScene(new Scene(view));
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        });
    }
}
