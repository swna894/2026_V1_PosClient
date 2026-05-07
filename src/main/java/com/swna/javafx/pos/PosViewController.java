package com.swna.javafx.pos;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.util.StatusLabel;
import com.swna.javafx.common.util.StatusLabelManager;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.manager.BarcodeScannerManager;
import com.swna.javafx.pos.manager.CartButtonManager;
import com.swna.javafx.pos.manager.ClockManager;
import com.swna.javafx.pos.manager.PaymentDialogManager;
import com.swna.javafx.pos.manager.PosTableSetup;
import com.swna.javafx.pos.manager.UiNotifier;
import com.swna.javafx.pos.viewmodel.PosViewModel;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/PosView.fxml")
public class PosViewController {

    private final PosViewModel viewModel;
    private final SafeBarcodeScanner safeBarcodeScanner;
    private final PosTableSetup tableSetup;
    private final PaymentDialogManager paymentDialogManager;
    private final UiNotifier uiNotifier;
    private final ClockManager clockManager;
    private final BarcodeScannerManager scannerManager;
    private final CartButtonManager cartButtonManager;
    private final StatusLabelManager statusLabelManager;

    // FXML Components
    @FXML private TableView<PosItem> table;
    @FXML private TableColumn<PosItem, String> colNo;
    @FXML private TableColumn<PosItem, String> colBarcode;
    @FXML private TableColumn<PosItem, String> colDesc;
    @FXML private TableColumn<PosItem, String> colComment;
    @FXML private TableColumn<PosItem, Integer> colQty;
    @FXML private TableColumn<PosItem, Integer> colStock;
    @FXML private TableColumn<PosItem, Double> colPrice;
    @FXML private TableColumn<PosItem, Double> colTotal;
    @FXML private TableColumn<PosItem, Double> colDiscount;
    @FXML private TableColumn<PosItem, Void> colDelete;
    @FXML private TableColumn<PosItem, Void> colMinus;
    @FXML private TableColumn<PosItem, Void> colPlus;
    @FXML private TableColumn<PosItem, Void> colDiscountPrice;
    @FXML private TableColumn<PosItem, Void> colChangePrice;

    // Info Labels
    @FXML private Label labelDiscount;
    @FXML private Label labelClockTime;
    @FXML private Label labelClockDate;
    @FXML private Label labelTotalAmount;
    @FXML private Label labelTotalQty;
    @FXML private StatusLabel labelStatus;

    // Action Buttons
    @FXML private Button buttonCart1;
    @FXML private Button buttonCart2;
    @FXML private Button buttonCart3;
    @FXML private Button buttonDiscountVolumn;
    @FXML private Button buttonScanner;
    @FXML private Button buttonCancel;
    @FXML private Button buttonPrint;
    @FXML private Button buttonQty;
    @FXML private Button buttonCash;
    @FXML private Button buttonCredit;
    @FXML private Button buttonCashout;
    @FXML private Button buttonDrawer;
    
    // Image Views
    @FXML private ImageView posImageView;
    @FXML private ImageView printImageView;


    @FXML
    public void initialize() {

        // 1. 테이블 설정 (위임)
        tableSetup.setup(
            table, viewModel,
            colNo, colBarcode, colDesc, colComment,
            colQty, colStock, colPrice, colTotal, colDiscount,
            colDelete, colMinus, colPlus, colDiscountPrice, colChangePrice,
            this::showDiscountDialog, this::showPriceChangeDialog
        );
        
        // 2. 상단 레이블 바인딩
        labelTotalAmount.textProperty().bind(viewModel.totalAmountProperty().asString("Total: %.2f"));
        labelTotalQty.textProperty().bind(viewModel.totalQtyProperty().asString("Total Qty: %d"));
        labelDiscount.textProperty().bind(viewModel.discountProperty().asString("Discount: %.2f"));
        labelStatus.textProperty().bind(viewModel.scanStatusProperty());

        labelStatus.bindTo(viewModel.scanStatusProperty());
        


        // 3. UI 알림 타이머 설정
        uiNotifier.setupAutoClear(viewModel.scanStatusProperty());
        
        // 4. 시계 시작
        clockManager.start(labelClockTime, labelClockDate);
        
        // 5. 바코드 스캐너 설정
        scannerManager.setup(table, safeBarcodeScanner, this::handleBarcode);
        
        // 6. 장바구니 버튼 숨김 처리
        cartButtonManager.hideUnused(buttonCart2, buttonCart3);
        
        table.requestFocus();
    }

    private void handleBarcode(String code) {
        if (code == null || code.isBlank()) return;
        Platform.runLater(() -> viewModel.scan(code));
    }

    private void showDiscountDialog(PosItem item) {
        paymentDialogManager.showDiscountDialog(item, 
            revisedPrice -> viewModel.discountItemPrice(item, revisedPrice),
            () -> table.refresh()
        );
    }

    private void showPriceChangeDialog(PosItem item) {
        paymentDialogManager.showPriceChangeDialog(item,
            newPrice -> viewModel.changeItemPrice(item, newPrice),
            () -> table.refresh()
        );
    }

    @FXML
    private void onQuickAmount(ActionEvent event) {
        String amountText = ((Button) event.getSource()).getText().replaceAll("[^0-9.]", "");
        if (!amountText.isEmpty()) {
            viewModel.addQuickAmountItem(Double.parseDouble(amountText));
            table.requestFocus();
        }
    }

    @FXML
    private void onActionCart(ActionEvent event) {
        cartButtonManager.handleCartAction(viewModel, (Button) event.getSource(), () -> {
            table.refresh();
            if (!table.getItems().isEmpty()) table.getSelectionModel().select(0);
            table.requestFocus();
        });
    }

    @FXML
    private void onActionCash(ActionEvent event) {
        paymentDialogManager.showCashDialog(viewModel, 
            result -> afterPayment(result, "Cash")
        );
    }

    @FXML
    private void onActionCredit(ActionEvent event) {
        paymentDialogManager.showCreditDialog(viewModel,
            result -> afterPayment(result, "Mixed")
        );
    }

    @FXML
    private void onActionCashout(ActionEvent event) {
        paymentDialogManager.showCashoutDialog(viewModel,
            result -> afterPayment(result, "Cashout")
        );
    }

    /**
     * 결제 후 처리
     * 
     * @param result 결제 결과
     * @param paymentType 결제 유형 (Cash, Mixed, Cashout)
     */
    private void afterPayment(PaymentDialogManager.PaymentResult result, String paymentType) {
        if (result.isSuccess()) {
            // 결제 성공
            log.info("[UI] {} payment successful: {}", paymentType, result.getMessage());
            
            // 성공 메시지 표시 (선택사항)
            showSuccessMessage(result.getMessage());
            
            // 추가 작업 (예: 영수증 출력, 장바구니 초기화 등은 ViewModel에서 이미 처리됨)
            // ViewModel의 clear()가 호출되었으므로 UI 업데이트는 자동으로 됨
            
        } else {
            // 결제 실패
            log.warn("[UI] {} payment failed: {}", paymentType, result.getMessage());
            
            // 실패 메시지 표시
            showErrorMessage("Payment failed: " + result.getMessage());
        }
    }


    /**
     * 성공 메시지 표시
     */
    private void showSuccessMessage(String message) {
        // 방법 1: StatusBar에 표시
        statusLabelManager.setFadeOutEnabled(true);
        statusLabelManager.showSuccess(message);
        
        // 방법 2: 토스트 메시지
        // Toast.show(message);
        
        // 방법 3: 알림 다이얼로그
        // Alert alert = new Alert(Alert.AlertType.INFORMATION);
        // alert.setTitle("Success");
        // alert.setHeaderText(null);
        // alert.setContentText(message);
        // alert.showAndWait();
    }

    /**
     * 에러 메시지 표시
     */
    private void showErrorMessage(String message) {
        // 방법 1: StatusBar에 표시
        statusLabelManager.setFadeOutEnabled(true);
        statusLabelManager.showError(message);
        
        // 방법 2: 에러 다이얼로그
        // Alert alert = new Alert(Alert.AlertType.ERROR);
        // alert.setTitle("Payment Error");
        // alert.setHeaderText(null);
        // alert.setContentText(message);
        // alert.showAndWait();
    }

    @FXML private void onClose(MouseEvent event) { viewModel.clear(); System.exit(0); }
    @FXML private void onActionDiscountVolumn(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onActionScanner(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onActionCancel(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onActionPrint(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onActionQty(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onActionDrawer(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onPos(ActionEvent e) { log.debug("Not yet implemented"); }
    @FXML private void onPrint(ActionEvent e) { log.debug("Not yet implemented"); }
}