package com.swna.javafx.pos;

import java.time.LocalDate;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.MenuController;
import com.swna.javafx.admin.SettingViewController;
import com.swna.javafx.admin.supplier.SupplierController;
import com.swna.javafx.barcode.LabelController;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.common.util.StatusLabel;
import com.swna.javafx.common.util.StatusLabelManager;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import com.swna.javafx.pos.dialog.PrintReceiptDialogController;
import com.swna.javafx.pos.factory.PosTableFactory;
import com.swna.javafx.pos.manager.BarcodeScannerManager;
import com.swna.javafx.pos.manager.CartButtonManager;
import com.swna.javafx.pos.manager.ClockManager;
import com.swna.javafx.pos.manager.PosDialogManager;
import com.swna.javafx.pos.manager.UiNotifier;
import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.service.config.PosToggleService;
import com.swna.javafx.pos.service.config.PrintToggleService;
import com.swna.javafx.pos.viewmodel.PosViewModel;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;


@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/pos-view.fxml")
public class PosViewController {

    // 스타일 클래스 상수 정의
    private static final String STYLE_ON = "print-on";
    private static final String STYLE_OFF = "print-off";
    private static final String TEXT_ON = "ON";
    private static final String TEXT_OFF = "OFF";

    // ========== Constants ==========
    private final PosViewModel viewModel;
    private final SafeBarcodeScanner safeBarcodeScanner;
    private final BarcodeScannerManager scannerManager;
    private final PosTableFactory tableSetup;
    private final PosDialogManager posDialogManager;
    private final UiNotifier uiNotifier;
    private final ClockManager clockManager;
    private final CartButtonManager cartButtonManager;
    private final NavigationService navigationService;
    private final StatusLabelManager statusLabelManager;
    private final PrintToggleService printToggleService; 
    private final PosToggleService posToggleService; 

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

    @FXML private BorderPane rootPane;
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
    @FXML private Button buttonBarcode;
    @FXML private Button buttonDiscountVolumn;
    @FXML private Button buttonReceipt;
    @FXML private Button buttonCancel;
    @FXML private Button buttonPrint;
    @FXML private Button buttonQty;
    @FXML private Button buttonCash;
    @FXML private Button buttonCredit;
    @FXML private Button buttonCashout;
    @FXML private Button buttonDrawer;
    @FXML private Button buttonMenu;
    @FXML private Button buttonOnGenerate;
    @FXML private Button buttonOnPos;
    @FXML private Button buttonOnPrint;
    @FXML private Button buttonSettings;
    @FXML private Button buttonSupplier;
    
    // Image Views
    @FXML private ImageView posImageView;
    @FXML private ImageView printImageView;

    @FXML
    public void initialize() {
        // 0. Hotkeys 설정
        setupHotkeys();

        // 1. 테이블 설정
        setupTable();
        
        // 2. 상단 레이블 바인딩
        setupLabelBindings();
        
        // 3. UI 알림 타이머 설정
        uiNotifier.setupAutoClear(viewModel.scanStatusProperty());
        
        // 4. 시계 시작
        clockManager.start(labelClockTime, labelClockDate);
        
        // 5. 바코드 스캐너 설정
        scannerManager.setup(table, safeBarcodeScanner, this::handleBarcode);
        
        // 6. 장바구니 버튼 숨김 처리
        cartButtonManager.hideUnused(buttonCart3);
        if(!printToggleService.isBarcodeEnabled()) {
            cartButtonManager.hideUnused(buttonReceipt);
        }
        
        // 7. 테이블 포커스
        table.requestFocus();
        colChangePrice.setVisible(false);
        
        // 8. Print 버튼 초기 상태 설정 (서비스 상태 반영)
        updatePrintButtonState();
        updatePosButtonState();
        
        // 9. Print 상태 변경 리스너 등록
        printToggleService.printEnabledProperty().addListener((obs, oldVal, newVal) ->  Platform.runLater(this::updatePrintButtonState));
        posToggleService.posEnabledProperty().addListener((obs, oldVal, newVal) ->  Platform.runLater(this::updatePosButtonState));
    }
    
    // ========== Setup Methods ==========

    private void setupTable() {
        PosTableFactory.TableColumns columns = PosTableFactory.TableColumns.builder()
            .colNo(colNo)
            .colBarcode(colBarcode)
            .colDesc(colDesc)
            .colComment(colComment)
            .colQty(colQty)
            .colStock(colStock)
            .colPrice(colPrice)
            .colDiscount(colDiscount)
            .colTotal(colTotal)
            .colDelete(colDelete)
            .colMinus(colMinus)
            .colPlus(colPlus)
            .colDiscountPrice(colDiscountPrice)
            .colChangePrice(colChangePrice)
            .build();
        
        PosTableFactory.Callbacks callbacks = PosTableFactory.Callbacks.builder()
            .onDiscount(this::showDiscountDialog)
            .onChangePrice(this::showPriceChangeDialog)
            .build();
        
        tableSetup.setup(table, viewModel, columns, callbacks);
        
    }
    
    private void setupLabelBindings() {
        labelTotalAmount.textProperty().bind(viewModel.totalAmountProperty().asString("Total: %.2f"));
        labelTotalQty.textProperty().bind(viewModel.totalQtyProperty().asString("Total Qty: %d"));
        labelDiscount.textProperty().bind(viewModel.discountProperty().asString("Discount: %.2f"));
        labelStatus.textProperty().bind(viewModel.scanStatusProperty());
        labelStatus.bindTo(viewModel.scanStatusProperty());
    }

    // ========== Handler Methods ==========
    
    private void handleBarcode(String code) {
        if (code == null || code.isBlank()) return;
        Platform.runLater(() -> viewModel.scan(code)); 
    }

    private void showDiscountDialog(PosItem item) {
        posDialogManager.showDiscountDialog(item, 
            revisedPrice -> viewModel.discountItemPrice(item, revisedPrice),
            () -> table.refresh()
        );
    }

    private void showPriceChangeDialog(PosItem item) {
        posDialogManager.showPriceChangeDialog(item,
            newPrice -> viewModel.changeItemPrice(item, newPrice),
            () -> table.refresh()
        );
    }

    // ========== Action Methods ==========
    
    @FXML
    private void onQuickAmount(ActionEvent event) {
        String amountText = ((Button) event.getSource()).getText().replaceAll("[^0-9.]", "");
        if (!amountText.isEmpty()) {
            viewModel.addQuickAmountItem(Double.parseDouble(amountText));
            table.requestFocus();
        }
    }

    // 버튼 1 클릭 시
    @FXML private void onActionCart1(ActionEvent event) {
        cartButtonManager.handleCartAction(viewModel, 1, buttonCart1, () -> refreshTable());
        refreshCartButtonStyles();
    }

    // 버튼 2 클릭 시
    @FXML private void onActionCart2(ActionEvent event) {
        cartButtonManager.handleCartAction(viewModel, 2, buttonCart2, () -> refreshTable());
        refreshCartButtonStyles();
    }

    @FXML private void onActionCart3(ActionEvent event) {
        log.info("cart2");
    }

    // 버튼 스타일 전체 갱신 (물건 유무에 따른 색상 구분)
    private void refreshCartButtonStyles() {
        updateButtonStyle(buttonCart1, 1);
        updateButtonStyle(buttonCart2, 2);
    }

    private void updateButtonStyle(Button btn, int cartId) {
        boolean hasItems = viewModel.getHoldManager().hasItems(cartId);
        String styleClass = "cart-held";

        if (hasItems) {
            // 없으면 추가 (중복 방지)
            if (!btn.getStyleClass().contains(styleClass)) {
                btn.getStyleClass().add(styleClass);
            }
        } else {
            // 있으면 제거
            btn.getStyleClass().remove(styleClass);
        }
    }

    /**
     * 테이블 뷰의 데이터를 새로고침하고 UI 상태를 최적화합니다.
     */
    private void refreshTable() {
        // 1. 테이블의 데이터를 새로고침 (데이터 소스가 변경되었을 때 필수)
        table.refresh();
        
        // 2. 테이블에 아이템이 있다면 첫 번째 행을 선택 (사용자 편의성)
        if (!table.getItems().isEmpty()) {
            table.getSelectionModel().select(0);
        }
        
        // 3. 테이블에 포커스를 주어 즉시 바코드 스캔이 가능하도록 함
        table.requestFocus();
    }

    // [사용자] → [PosViewController] → [PosDialogManager] → [CashDialogController]
    // → [PosViewModel] → [PosProcessor] → [PosApiService] → [WebClientCommon]
    // → [CommonApiClient] → [WebClient] → [서버]
    @FXML private void onActionCash(ActionEvent event) {
        posDialogManager.showCashDialog(viewModel, 
            result -> afterPayment(result, "Cash")
        );
    }

    @FXML private void onActionCredit(ActionEvent event) {
        posDialogManager.showCreditDialog(viewModel,
            result -> afterPayment(result, "Mixed")
        );
    }

    // [사용자] → [PosViewController] → [PosDialogManager] → [CashoutDialogController]
    // → [PosViewModel] → [PosProcessor] → [PosApiService] → [WebClientCommon]
    // → [CommonApiClient] → [WebClient] → [서버]
    @FXML private void onActionCashout(ActionEvent event) {
        posDialogManager.showCashoutDialog(viewModel,
            result -> afterPayment(result, "Cashout")
        );
    }

    private void afterPayment(PosDialogManager.DialogResult result, String paymentType) {
        if (result.isSuccess()) {
            log.info("[UI] {} payment successful: {}", paymentType, result.getMessage());
            showSuccessMessage(result.getMessage());
        } else {
            log.warn("[UI] {} payment failed: {}", paymentType, result.getMessage());
            showErrorMessage("Payment failed: " + result.getMessage());
        }
    }

    private void showSuccessMessage(String message) {
        statusLabelManager.setFadeOutEnabled(true);
        statusLabelManager.showSuccess(message);
    }

    private void showErrorMessage(String message) {
        statusLabelManager.setFadeOutEnabled(true);
        statusLabelManager.showError(message);
    }

    @FXML private void onClose(MouseEvent event) { viewModel.clear(); System.exit(0); }

    @FXML private void onActionDiscountVolumn(ActionEvent e) { 
        log.info("onActionDiscountVolumn");
        
        posDialogManager.showVolumeDiscountDialog( viewModel, this::afterVolumeDiscount );
    }

    private void afterVolumeDiscount(PosDialogManager.DialogResult result) {
        if (result.isSuccess()) {
            log.info("[UI] Volume discount successful: {}", result.getMessage());
            showSuccessMessage(result.getMessage());
            table.refresh();  // 테이블 새로고침
        } else {
            log.warn("[UI] Volume discount failed: {}", result.getMessage());
            showErrorMessage("Volume discount failed: " + result.getMessage());
        }
    }

    @FXML private void onActionReceipt(ActionEvent e) { 
        log.info("onActionReceipt");
        
        posDialogManager.showReceiptDialog(receiptNumber -> {
            log.info("Searching for receipt: {}", receiptNumber);
            // 영수증 검색 로직 구현
            searchReceipt(receiptNumber);
        });
    }

    /**
     * 영수증 번호로 검색하는 메서드
     */
    private void searchReceipt(String receiptNumber) {
        showSuccessMessage("Searching for receipt: " + receiptNumber);
    }

    @FXML  private void onActionBarcode(ActionEvent e) { 
        log.info("onActionBarcode");
        
        posDialogManager.showBarcodeDialog(barcodeNumber -> {
            log.info("Searching for barcode: {}", barcodeNumber);
            // 바코드 검색 로직 구현
            searchBarcode(barcodeNumber);
        });
    }

    /**
     * 바코드 번호로 검색하는 메서드
     */
    private void searchBarcode(String barcodeNumber) {
        // 바코드 스캔 처리
        viewModel.scan(barcodeNumber);
        showSuccessMessage("Barcode scanned: " + barcodeNumber);
    }

    @FXML private void onActionCancel(ActionEvent e) { 
        log.info("onActionCancel");
        
        posDialogManager.showCancelDialog(viewModel,  result -> afterPayment(result, "Cash") );
    }


    @FXML private void onActionQty(ActionEvent e) { log.info("onActionQty"); }
    @FXML private void onActionPrint(ActionEvent e) { 
        log.info("onActionPrint");
        
        posDialogManager.showPrintReceiptDialog(new PrintReceiptDialogController.PrintReceiptCallback() {
            @Override
            public void onSearch(LocalDate startDate, LocalDate endDate) {
                log.info("Searching receipts from {} to {}", startDate, endDate);
                searchReceipts(startDate, endDate);
            }
            
            @Override
            public void onPrint(String receiptNo) {
                log.info("Printing receipt: {}", receiptNo);
                printReceipt(receiptNo);
            }
            
            @Override
            public void onPreview(String receiptNo) {
                log.info("Previewing receipt: {}", receiptNo);
                previewReceipt(receiptNo);
            }
        });
    }

    /**
     * 영수증 검색 메서드
     */
    private void searchReceipts(LocalDate startDate, LocalDate endDate) {
        showSuccessMessage("Searching receipts from " + startDate + " to " + endDate);
    }

    /**
     * 영수증 출력 메서드
     */
    private void printReceipt(String receiptNo) {
        showSuccessMessage("Printing receipt: " + receiptNo);
    }

    /**
     * 영수증 미리보기 메서드
     */
    private void previewReceipt(String receiptNo) {
        showSuccessMessage("Previewing receipt: " + receiptNo);
    }

    @FXML private void onActionDrawer(ActionEvent e) { log.info("onActionDrawer"); }
    @FXML private void onGenerate(ActionEvent e) { navigationService.navigateStage(LabelController.class); }

    @FXML private void onMenu(ActionEvent e) {
        // 기존 대체
        // navigationService.navigateStage(MenuController.class);
        // 현재 창은 그대로 두고 새 창으로 MenuController 실행
        navigationService.openInNewWindow(MenuController.class, "메뉴 관리");
    }
    @FXML
    private void onPos(ActionEvent e) {
        posToggleService.toggle();
        updatePosButtonState();
        
        String msg = posToggleService.isPosEnabled() ? "POS 결제 활성화" : "POS 결제 비활성화";
        if (posToggleService.isPosEnabled()) {
            showSuccessMessage(msg);
        } else {
            showErrorMessage(msg);
        }
    }

    /**
     * POS 버튼 UI 업데이트 (텍스트 및 스타일 클래스 변경)
     */
    private void updatePosButtonState() {
        // 기존 스타일 클래스 제거 (Print와 동일한 클래스 재사용 가능 또는 별도 정의)
        buttonOnPos.getStyleClass().removeAll(STYLE_ON, STYLE_OFF);
        
        if (posToggleService.isPosEnabled()) {
            buttonOnPos.setText(TEXT_ON);
            buttonOnPos.getStyleClass().add(STYLE_ON); // 녹색 스타일
        } else {
            buttonOnPos.setText(TEXT_OFF);
            buttonOnPos.getStyleClass().add(STYLE_OFF); // 빨간색 스타일
        }
    }
    
    /**
     * Print 버튼 클릭 시 토글 (ON/OFF)
     * - ON: 프린트 활성화 (실제 프린터로 출력)
     * - OFF: 프린트 비활성화 (출력하지 않음)
     */
    @FXML 
    private void onPrint(ActionEvent e) {
        printToggleService.toggle();
        String statusMsg = printToggleService.isPrintEnabled() 
            ? "Print enabled (ON)" 
            : "Print disabled (OFF)";

        if (printToggleService.isPrintEnabled()) {
            showSuccessMessage(statusMsg);  // 초록색
        } else {
            showErrorMessage(statusMsg);     // 빨간색
        }
    }
    
    @FXML void  onSettings(ActionEvent e) {
       // 새로운 창(Stage)으로 띄우고 싶을 때
        navigationService.navigateStage(SettingViewController.class);
        
        // 또는 현재 창의 씬(Scene)만 교체하고 싶을 때
        // navigationService.navigate(SupplierController.class)
    }

    @FXML 
    private void onSupplier(ActionEvent e) {
       // 새로운 창(Stage)으로 띄우고 싶을 때
        navigationService.navigateStage(SupplierController.class);
        
        // 또는 현재 창의 씬(Scene)만 교체하고 싶을 때
        // navigationService.navigate(SupplierController.class)
    }
    /**
     * Print 버튼 UI 업데이트 (텍스트 및 색상 변경)
     * - ON: 녹색 텍스트 (print-on 클래스 추가)
     * - OFF: 빨간색 텍스트 (print-off 클래스 추가)
     * - 버튼 기본 스타일(btn, btn-md)은 유지
     */
    private void updatePrintButtonState() {
        // 기존 ON/OFF 관련 스타일 클래스 제거
        buttonOnPrint.getStyleClass().removeAll(STYLE_ON, STYLE_OFF);
        
        if (printToggleService.isPrintEnabled()) {
            buttonOnPrint.setText(TEXT_ON);
            buttonOnPrint.getStyleClass().add(STYLE_ON);
        } else {
            buttonOnPrint.setText(TEXT_OFF);
            buttonOnPrint.getStyleClass().add(STYLE_OFF);
        }
    }

    /**
     * POS 시스템에서 사용하는 키보드 단축키(Hotkeys)를 등록합니다.
     * 루트 컨테이너(rootPane)에 EventFilter를 추가하여 포커스 위치에 상관없이 작동합니다.
     */
    private void setupHotkeys() {
        // 1. rootPane에 키 이벤트 필터 등록
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            
            // 화살표 키(UP, DOWN)는 TableView의 기본 행 이동 기능을 사용해야 하므로
            // 별도의 처리를 하지 않고 이벤트를 통과시킵니다.
            if (code == KeyCode.UP || code == KeyCode.DOWN) {
                return; 
            }

            // ====== 좌/우 방향키 수량 변경 로직 (별도 메서드로 추출) ======
            if (adjustQuantityWithArrowKeys(code)) {
                event.consume();
                return; // 처리 완료 시 리턴
            }
            
            switch (code) {
                case F1 -> {
                    // 현금함 열기
                    buttonDrawer.fire(); 
                    event.consume(); // 이벤트가 다른 컨트롤로 전파되는 것을 방지
                }
                case F2 -> {
                    // 장바구니 1번 액션
                    buttonCart1.fire();
                    event.consume();
                }
                case F3 -> {
                    // 영수증 검색[cite: 1]
                    buttonReceipt.fire(); 
                    event.consume();
                }
                case F5 -> {
                    // 장바구니 3번 액션[cite: 1]
                    buttonDiscountVolumn.fire();
                    event.consume();
                }
                case F6 -> {
                    // 장바구니 3번 액션[cite: 1]
                    buttonBarcode.fire();
                    event.consume();
                }
                case F7 -> {
                    // 장바구니 3번 액션[cite: 1]
                    buttonCancel.fire();
                    event.consume();
                }
                case F8 -> {
                    // 프린트 설정 토글[cite: 1]
                    buttonOnPrint.fire();
                    event.consume();
                }
                case F9 -> {
                    // 프린트 설정 토글[cite: 1]
                    buttonQty.fire();
                    event.consume();
                }
                case F10 -> {
                    // 현금 결제 실행[cite: 1]
                    buttonCash.fire();
                    event.consume();
                }
                case F11 -> {
                    // 신용카드/혼합 결제 실행[cite: 1]
                    buttonCredit.fire();
                    event.consume();
                }
                case F12 -> {
                    // 캐시아웃 결제 실행[cite: 1]
                    buttonCashout.fire();
                    event.consume();
                }
                default -> {
                    // 지정되지 않은 키는 무시
                }
            }
        });
    }

    /**
     * 좌/우 방향키 입력을 감지하여 선택된 행의 수량을 조절합니다.
     * @return 이벤트를 처리했으면 true, 아니면 false
     */
    private boolean adjustQuantityWithArrowKeys(KeyCode code) {
        if (code != KeyCode.LEFT && code != KeyCode.RIGHT) {
            return false;
        }

        PosItem selectedItem = table.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return false;
        }

        if (code == KeyCode.LEFT) {
            viewModel.decreaseQty(selectedItem); // 왼쪽 키: 수량 감소[cite: 1, 2]
        } else {
            viewModel.increaseQty(selectedItem);  // 오른쪽 키: 수량 증가[cite: 1, 2]
        }

        table.refresh(); // 테이블 UI 갱신[cite: 1]
        return true;
    }
}