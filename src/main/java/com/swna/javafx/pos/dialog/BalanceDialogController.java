package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import net.rgielen.fxweaver.core.FxmlView;

@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/BalanceDialog.fxml")
public class BalanceDialogController extends BasePosDialog {

    @FXML private Label lblBalance;
    @FXML private TextField dummyField;

    private static final DecimalFormat FMT = new DecimalFormat("$#,##0.00");
    private Consumer<BalanceResult> callback;

    public void initData(BigDecimal balance, Consumer<BalanceResult> callback) {
        this.callback = callback;
        lblBalance.setText(FMT.format(balance));
        
        // 1. 키 이벤트 핸들러 등록
        setupKeyEvents();
        
        // 2. 창 드래그 활성화
        enableFullWindowDrag();
    }

    private void setupKeyEvents() {
        Platform.runLater(() -> {
            if (lblBalance.getScene() != null) {
                lblBalance.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        handleComplete();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F8) {
                        handlePrint();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        handleCancel();
                        event.consume();
                    }
                });
            }
        });
    }

    @FXML private void handlePrint() { finish(new BalanceResult(true, false)); }
    @FXML private void handleComplete() { finish(new BalanceResult(false, true)); }

    @Override
    @FXML 
    public void handleCancel() { 
        finish(new BalanceResult(false, false)); 
    }

    private void finish(BalanceResult res) {
        if (callback != null) callback.accept(res);
        closeDialog();
    }

    @Override
    @FXML 
    protected void handleConfirm() { 
        handleComplete(); 
    }

    @Override
    protected TextField getFocusField() {
        return dummyField;
    }

    public record BalanceResult(boolean isPrint, boolean isComplete) {}
}