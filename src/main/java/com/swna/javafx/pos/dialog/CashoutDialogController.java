package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/CashoutPaymentDialog.fxml")
public class CashoutDialogController extends BasePaymentDialog {

    @FXML private Label lblAmount;      // 총 상품 금액
    @FXML private Label lblDiscount;    // 할인 금액
    @FXML private Label lblCredit;      // 카드 결제 예정 금액
    @FXML private TextField txtCashout; // 현금 인출 요청 금액

    private BigDecimal totalAfterDiscount;  // 할인 적용된 최종 금액 (상품 금액 - 할인)
    private BiConsumer<BigDecimal, BigDecimal> onProcessComplete;

    public void initData(BigDecimal totalAmount, BigDecimal discount, BiConsumer<BigDecimal, BigDecimal> callback) {
        // 1. 할인 적용된 최종 금액 계산
        this.totalAfterDiscount = totalAmount;
        this.onProcessComplete = callback;

        // 2. UI 라벨 설정
        lblAmount.setText(String.format(CURRENCY_FORMAT, totalAmount));
        lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));
        
        // 3. 입력 필드 설정
        applyNumericFilter(txtCashout);
        setupKeyEvents(txtCashout);

        txtCashout.setText("0.00");
        txtCashout.selectAll();

        // 4. 입력값 변경 시 카드 결제 금액 업데이트
        txtCashout.textProperty().addListener((obs, old, val) -> updateTotalCardAmount(val));
        updateTotalCardAmount(txtCashout.getText());
        
        // 5. 포커스 설정
        txtCashout.requestFocus();
        
        log.debug("[CashoutDialog] 초기화 완료 - totalAfterDiscount: {}", totalAfterDiscount);
    }

    /**
     * 현금 인출 금액 변경 시 카드 결제 예정 금액 업데이트
     * 카드 결제 금액 = (상품금액 - 할인) + 현금인출금액
     */
    private void updateTotalCardAmount(String input) {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(input);
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            lblCredit.setText(String.format(CURRENCY_FORMAT, totalCardAmount));
            lblCredit.setStyle("-fx-text-fill: blue;");
            
            log.debug("[CashoutDialog] cashout: {}, totalCard: {}", cashoutAmount, totalAfterDiscount);
        } catch (Exception e) {
            lblCredit.setText("Invalid");
            log.warn("[CashoutDialog] 입력 파싱 오류: {}", input, e);
        }
    }
    
    /**
     * 현금 인출 입력값 파싱
     */
    private BigDecimal parseCashoutInput(String input) {
        if (input == null || input.isEmpty() || input.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(input);
    }
    
    /**
     * 최종 카드 결제 금액 계산
     * totalCardAmount = totalAfterDiscount + cashoutAmount
     */
    private BigDecimal calculateTotalCardAmount(BigDecimal cashoutAmount) {
        return totalAfterDiscount.add(cashoutAmount);
    }

    @Override
    protected void handleConfirm() {
        try {
            BigDecimal cashoutAmount = parseCashoutInput(txtCashout.getText());
            BigDecimal totalCardAmount = calculateTotalCardAmount(cashoutAmount);
            
            log.info("[CashoutDialog] 결제 확인 - cashout: {}, totalCard: {}", cashoutAmount, totalCardAmount);
            
            if (onProcessComplete != null) {
                // ✅ 정확한 값 전달: (cashoutAmount, totalAfterDiscount)
                onProcessComplete.accept(cashoutAmount, totalAfterDiscount);
            }
            closeDialog();
            
        } catch (Exception e) {
            log.error("[CashoutDialog] 결제 확인 중 오류", e);
            txtCashout.requestFocus();
        }
    }

    @Override
    protected TextField getFocusField() {
        return txtCashout;
    }
}