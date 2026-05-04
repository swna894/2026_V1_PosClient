package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j 
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/CashPaymentDialog.fxml")
public class CashDialogController {

   private static final String CURRENCY_FORMAT = "$%.2f";
   // 숫자와 최대 하나의 소수점만 허용하는 정규식 패턴
   private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");

   @FXML private Label lblAmount;      
   @FXML private TextField txtCash;    
   @FXML private Label lblBalance;     
   @FXML private Label lblDiscount;   

   private BigDecimal totalAmount;
   private Consumer<BigDecimal> onPaymentComplete; 

   public void initData(BigDecimal total, BigDecimal discount, Consumer<BigDecimal> callback) {
      this.totalAmount = total;
      this.lblAmount.setText(String.format(CURRENCY_FORMAT, total));
      this.lblDiscount.setText(String.format(CURRENCY_FORMAT, discount));
      this.onPaymentComplete = callback;

      // 1. 숫자 전용 텍스트 포매터 적용 (숫자와 . 만 허용) [추가]
      applyNumericFilter(txtCash);

      // 2. 기본값 세팅 및 전체 선택
      this.txtCash.setText(total.toPlainString());
      this.txtCash.selectAll();

      // 3. 거스름돈 실시간 계산
      txtCash.textProperty().addListener((observable, oldValue, newValue) -> {
         calculateChange(newValue);
      });

      // 4. 키 이벤트 리스너 (Enter/ESC)
      txtCash.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
         if (event.getCode() == KeyCode.ENTER) {
            handleConfirm();
            event.consume(); 
         } else if (event.getCode() == KeyCode.ESCAPE) {
            handleCancel();
            event.consume();
         }
      });

      // 초기 상태 업데이트
      calculateChange(this.txtCash.getText());
      txtCash.requestFocus();
   }

   /**
    * TextField에 숫자와 소수점 한 개만 입력 가능하도록 필터 적용 [추가]
    */
   private void applyNumericFilter(TextField textField) {
      TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
         String newText = change.getControlNewText();
         if (NUMERIC_PATTERN.matcher(newText).matches()) {
            return change; // 패턴과 일치하면 변경 허용
         }
         return null; // 일치하지 않으면 변경 거부
      });
      textField.setTextFormatter(textFormatter);
   }

   private void calculateChange(String input) {
      try {
         // 입력이 비어있거나 "."만 있는 경우 처리
         String sanitizedInput = (input.isEmpty() || input.equals(".")) ? "0" : input;
         BigDecimal receivedCash = new BigDecimal(sanitizedInput);
         BigDecimal change = receivedCash.subtract(totalAmount);

         if (change.compareTo(BigDecimal.ZERO) >= 0) {
               lblBalance.setText(String.format(CURRENCY_FORMAT, change));
               lblBalance.setStyle("-fx-text-fill: blue;"); 
         } else {
               lblBalance.setText("Insufficient");
               lblBalance.setStyle("-fx-text-fill: red;");  
         }
      } catch (NumberFormatException e) {
         lblBalance.setText("Invalid");
      }
   }

   @FXML
   private void handleConfirm() {
      try {
         String text = txtCash.getText();
         BigDecimal receivedCash = new BigDecimal(text.isEmpty() || text.equals(".") ? "0" : text);

         if (receivedCash.compareTo(totalAmount) >= 0) {
               if (onPaymentComplete != null) {
                  onPaymentComplete.accept(receivedCash);
               }
               closeDialog();
         } else {
               txtCash.requestFocus();
         }
      } catch (NumberFormatException e) {
         txtCash.requestFocus();
      }
   }

   @FXML
   private void handleCancel() {
      closeDialog();
   }

   private void closeDialog() {
      Stage stage = (Stage) txtCash.getScene().getWindow();
      stage.close();
   }
}