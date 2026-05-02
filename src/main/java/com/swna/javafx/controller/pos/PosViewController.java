package com.swna.javafx.controller.pos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.controller.pos.dialog.ItemDiscountDialogController;
import com.swna.javafx.controller.pos.dialog.ItemPriceChangeDialogController;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.infrastructure.scanner.BarcodeInputEngine;
import com.swna.javafx.viewmodel.pos.PosViewModel;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * POS 메인 화면 컨트롤러
 * 상품 스캔, 장바구니 관리, 결제 등 POS 시스템의 전반적인 기능을 담당합니다.
 * 
 * @author POS Team
 * @version 1.0
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/PosView.fxml")
public class PosViewController {

    // =========================
    // Constants
    // =========================
    /** 시간 표시 포맷 (월/일 오전/오후 시:분:초) */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd a HH:mm:ss", Locale.ENGLISH);
    
    /** 금액 버튼에서 숫자만 추출하기 위한 정규식 패턴 */
    private static final String BUTTON_AMOUNT_REGEX = "[^0-9.]";
    
    /** 버튼 컬럼의 고정 너비 (픽셀) */
    private static final int BUTTON_COLUMN_WIDTH = 50;
    
    /** 테이블 셀 기본 스타일 (투명 배경, 중앙 정렬) */
    private static final String STYLE_BASE_CELL = "-fx-background-color: transparent; -fx-alignment: CENTER;";
    
    /** 재고 부족 스타일 (빨간색, 굵게) */
    private static final String STYLE_LOW_STOCK = "-fx-text-fill: red; -fx-font-weight: bold;";
    
    /** 정상 재고 스타일 (검정색) */
    private static final String STYLE_NORMAL_STOCK = "-fx-text-fill: black;";

    // =========================
    // Dependencies
    // =========================
    /** POS 비즈니스 로직 및 상태 관리를 위한 ViewModel */
    private final PosViewModel viewModel;
    
    /** FXML 다이얼로그 로딩을 위한 FxWeaver */
    private final FxWeaver fxWeaver;
    
    /** 바코드 스캐너 입력 처리 엔진 */
    private final BarcodeInputEngine barcodeInputEngine = new BarcodeInputEngine();

    // =========================
    // UI Components (Grouped by purpose)
    // =========================
    
    // Table and Columns
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
    @FXML private Label labelInfo;
    @FXML private Label labelTime;
    @FXML private Label labelTotalAmount;
    @FXML private Label labelTotalQty;

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

    // =========================
    // Initialize (View lifecycle)
    // =========================
    
    /**
     * FXML 로딩 후 자동 호출되는 초기화 메서드
     * UI 컴포넌트 초기화, 데이터 바인딩, 바코드 스캐너 설정, 시계 시작 등을 수행합니다.
     */
    @FXML
    public void initialize() {
        log.info("[INIT] PosViewController initialized");
        initializeUIComponents();
        bindTopLabels();
        setupTableColumns();
        setupBarcodeScanner();
        startClock();
        hideUnusedCartButtons();
    }

    /**
     * UI 컴포넌트의 초기 상태를 설정합니다.
     */
    private void initializeUIComponents() {
        // 추가 UI 초기화 로직
    }

    /**
     * 사용하지 않는 장바구니 버튼(Cart2, Cart3)을 숨깁니다.
     * setVisible(false)와 setManaged(false)로 공간도 제거합니다.
     */
    private void hideUnusedCartButtons() {
        buttonCart2.setVisible(false);
        buttonCart2.setManaged(false);
        buttonCart3.setVisible(false);
        buttonCart3.setManaged(false);
    }

    // =========================
    // Barcode Scanner Setup
    // =========================
    
    /**
     * 바코드 스캐너 입력 엔진을 설정합니다.
     * 콜백을 등록하고 Scene이 준비되면 스캐너를 해당 Scene에 연결합니다.
     */
    private void setupBarcodeScanner() {
        log.info("[SCANNER] initializing BarcodeInputEngine");
        barcodeInputEngine.setOnBarcode(this::handleBarcode);
        
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                log.info("[SCANNER] attaching to scene");
                barcodeInputEngine.attach(newScene);
            }
        });
    }

    /**
     * 스캐너로부터 전달받은 바코드를 처리합니다.
     * 
     * @param code 스캔된 바코드 문자열
     */
    private void handleBarcode(String code) {
        if (code == null || code.isBlank()) {
            log.warn("[SCAN] ignored empty barcode");
            return;
        }

        log.info("[SCAN] received barcode: {}", code);
        
        Platform.runLater(() -> {
            log.info("[SCAN] sending to ViewModel: {}", code);
            viewModel.scan(code);
            log.info("[SCAN] ViewModel scan executed: {}", code);
        });
    }

    // =========================
    // Table Setup
    // =========================
    
    /**
     * 테이블 컬럼 설정의 메인 진입점
     * 테이블 속성, 컬럼 바인딩, 재고 스타일, 선택 동기화를 설정합니다.
     */
    private void setupTableColumns() {
        configureTableProperties();
        setupColumnBindings();
        setupStockCellStyle();
        setupSelectionSync();
        log.info("[TABLE] binding completed");
    }

    /**
     * 테이블 기본 속성을 설정합니다.
     * 편집 가능 여부 및 데이터 소스(items)을 바인딩합니다.
     */
    private void configureTableProperties() {
        table.setEditable(true);
        table.setItems(viewModel.getItems());
    }

    /**
     * 각 테이블 컬럼의 속성과 바인딩을 설정합니다.
     * 번호, 액션 버튼(삭제, 수량 증감, 할인, 가격변경), 데이터 컬럼을 구성합니다.
     */
    private void setupColumnBindings() {
        // Basic columns
        TableColumnUtil.createNumberColumn(table, colNo, 70);
        
        // Action buttons
        TableColumnUtil.makeButtonColumn(colDelete, null, IconPaths.DELETE, BUTTON_COLUMN_WIDTH, 
            event -> getSelectedItem().ifPresent(viewModel::removeItem));
        
        TableColumnUtil.makeButtonColumn(colMinus, null, IconPaths.MINUS, BUTTON_COLUMN_WIDTH, 
            event -> getSelectedItem().ifPresent(viewModel::decreaseQty));
        
        TableColumnUtil.makeButtonColumn(colPlus, null, IconPaths.PLUS, BUTTON_COLUMN_WIDTH, 
            event -> getSelectedItem().ifPresent(viewModel::increaseQty));
        
        TableColumnUtil.makeButtonColumn(colDiscountPrice, null, IconPaths.DISCOUNT, BUTTON_COLUMN_WIDTH, 
            this::onDiscount);
        
        TableColumnUtil.makeButtonColumn(colChangePrice, null, IconPaths.PRICE_22, BUTTON_COLUMN_WIDTH, 
            this::onChangePrice);

        // Data columns
        TableColumnUtil.makeStringColumn(colBarcode, PosItem::barcodeProperty, PosItem::setBarcode, false, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(colDesc, PosItem::descriptionProperty, PosItem::setDescription, false, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeStringColumn(colComment, PosItem::commentProperty, PosItem::setComment, false, TableColumnUtil.LEFT, null);
        
        TableColumnUtil.makeIntegerColumn(colQty, PosItem::qtyProperty, PosItem::setQty, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeIntegerColumn(colStock, PosItem::stockProperty, PosItem::setStock, false, TableColumnUtil.CENTER, null);
        
        TableColumnUtil.makeCurrencyColumn(colPrice, PosItem::sellingPriceProperty, false, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeCurrencyColumn(colTotal, PosItem::finalAmountProperty, false, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeCurrencyColumn(colDiscount, PosItem::discountTotalProperty, false, TableColumnUtil.RIGHT, null);
    }

    /**
     * ViewModel과 TableView 간 선택 항목 동기화를 설정합니다.
     * 양방향 바인딩으로 테이블 선택과 ViewModel의 selectedItem을 일치시킵니다.
     */
    private void setupSelectionSync() {
        // ViewModel -> TableView 동기화
        viewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                table.getSelectionModel().select(newVal);
                table.scrollTo(newVal);
            } else {
                table.getSelectionModel().clearSelection();
            }
        });

        // TableView -> ViewModel 동기화
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.selectedItemProperty().set(newVal);
        });
    }

    /**
     * 재고 컬럼의 셀 스타일을 설정합니다.
     * 재고가 0 미만(부족)일 경우 빨간색 굵게 표시합니다.
     */
    private void setupStockCellStyle() {
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                
                setText(value.toString());
                setStyle(STYLE_BASE_CELL + (value < 0 ? STYLE_LOW_STOCK : STYLE_NORMAL_STOCK));
            }
        });
    }

    /**
     * 현재 테이블에서 선택된 항목을 Optional로 반환합니다.
     * 
     * @return 선택된 PosItem (없을 경우 Optional.empty)
     */
    private Optional<PosItem> getSelectedItem() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    // =========================
    // Top Labels Binding
    // =========================
    
    /**
     * 상단 정보 레이블들을 ViewModel 속성과 바인딩합니다.
     * 총 금액, 총 수량, 할인액, 스캔 상태 정보를 실시간으로 표시합니다.
     */
    private void bindTopLabels() {
        labelTotalAmount.textProperty().bind(viewModel.totalAmountProperty().asString("Total: %.2f"));
        labelTotalQty.textProperty().bind(viewModel.totalQtyProperty().asString("Total Qty: %d"));
        labelDiscount.textProperty().bind(viewModel.discountProperty().asString("Discount: %.2f"));
        labelInfo.textProperty().bind(viewModel.scanStatusProperty());
    }

    // =========================
    // Dialog Handlers
    // =========================
    
    /**
     * 가격 변경 버튼 클릭 이벤트 핸들러
     * 선택된 항목의 가격 변경 다이얼로그를 표시합니다.
     * 
     * @param event 마우스 클릭 이벤트
     */
    private void onChangePrice(MouseEvent event) {
        getSelectedItem().ifPresent(this::showPriceChangeDialog);
    }

    /**
     * 할인 버튼 클릭 이벤트 핸들러
     * 선택된 항목의 할인 설정 다이얼로그를 표시합니다.
     * 
     * @param event 마우스 클릭 이벤트
     */
    private void onDiscount(MouseEvent event) {
        getSelectedItem().ifPresent(this::showDiscountDialog);
    }

    /**
     * 가격 변경 다이얼로그를 표시합니다.
     * 
     * @param item 가격을 변경할 상품
     */
    private void showPriceChangeDialog(PosItem item) {
        showDialog(ItemPriceChangeDialogController.class, 
            controller -> controller.initData(item.getBarcode(), item.getSellingPrice(), 
                newPrice -> {
                    viewModel.changeItemPrice(item, newPrice);
                    table.refresh();
                })
        );
    }

    /**
     * 할인 설정 다이얼로그를 표시합니다.
     * 
     * @param item 할인을 적용할 상품
     */
    private void showDiscountDialog(PosItem item) {
        showDialog(ItemDiscountDialogController.class,
            controller -> controller.initData(item.getBarcode(), item.getSellingPrice(),
                revisedPrice -> {
                    viewModel.discountItemPrice(item, revisedPrice);
                    table.refresh();
                })
        );
    }

    /**
     * 다이얼로그를 생성하고 표시하는 제네릭 메서드
     * 
     * @param <T> 다이얼로그 컨트롤러 타입
     * @param controllerClass 컨트롤러 클래스
     * @param initializer 다이얼로그 초기화 콜백
     */
    private <T> void showDialog(Class<T> controllerClass, DialogInitializer<T> initializer) {
        FxControllerAndView<T, Parent> dialog = fxWeaver.load(controllerClass);
        dialog.getView().ifPresent(view -> {
            initializer.initialize(dialog.getController());
            
            Stage stage = new Stage();
            stage.setScene(new Scene(view));
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        });
    }

    /**
     * 다이얼로그 초기화를 위한 함수형 인터페이스
     * 
     * @param <T> 다이얼로그 컨트롤러 타입
     */
    @FunctionalInterface
    private interface DialogInitializer<T> {
        /**
         * 다이얼로그 컨트롤러를 초기화합니다.
         * 
         * @param controller 초기화할 컨트롤러 인스턴스
         */
        void initialize(T controller);
    }

    // =========================
    // UI Event Handlers
    // =========================
    
    /**
     * 빠른 금액 버튼 클릭 이벤트 핸들러
     * 버튼에 표시된 금액을 기준으로 임의의 상품을 장바구니에 추가합니다.
     * 
     * @param event 버튼 클릭 이벤트
     */
    @FXML
    private void onQuickAmount(ActionEvent event) {
        try {
            Button button = (Button) event.getSource();
            String amountText = button.getText().replaceAll(BUTTON_AMOUNT_REGEX, "");
            
            if (!amountText.isEmpty()) {
                double amount = Double.parseDouble(amountText);
                viewModel.addQuickAmountItem(amount);
            }
        } catch (NumberFormatException ex) {
            log.error("[QUICK AMOUNT] Failed to parse amount", ex);
        }
    }

    /**
     * POS 종료 및 화면 닫기 이벤트 핸들러
     * ViewModel 상태 초기화 후 애플리케이션을 종료합니다.
     * 
     * @param event 마우스 클릭 이벤트
     */
    @FXML
    private void onClose(MouseEvent event) {
        log.info("[CLEAR] reset POS state");
        viewModel.clear();
        System.exit(0);
    }

    /**
     * 결제 버튼 클릭 이벤트 핸들러
     * 현재 총 결제 금액을 표시하는 알림창을 띄웁니다.
     */
    @FXML
    private void onPayment() {
        double total = viewModel.totalAmountProperty().get();
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("결제");
            alert.setContentText("총 결제 금액: " + String.format("%.2f", total));
            alert.showAndWait();
        } catch (Exception e) {
            log.error("[PAYMENT] failed", e);
        }
    }

    // =========================
    // Utility Methods
    // =========================
    
    /**
     * 현재 시간을 1초 간격으로 업데이트하여 화면에 표시합니다.
     * Timeline 애니메이션을 사용하여 주기적으로 시간 문자열을 갱신합니다.
     */
    private void startClock() {
        Timeline clock = new Timeline(
            new KeyFrame(Duration.ZERO, e -> labelTime.setText(LocalDateTime.now().format(TIME_FORMATTER))),
            new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // =========================
    // Placeholder Handlers (To be implemented)
    // =========================
    
    /** 장바구니 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionCart(ActionEvent event) { log.debug("onActionCart - Not yet implemented"); }
    
    /** 볼륨 할인 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionDiscountVolumn(ActionEvent event) { log.debug("onActionDiscountVolumn - Not yet implemented"); }
    
    /** 스캐너 설정 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionScanner(ActionEvent event) { log.debug("onActionScanner - Not yet implemented"); }
    
    /** 취소 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionCancel(ActionEvent event) { log.debug("onActionCancel - Not yet implemented"); }
    
    /** 출력 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionPrint(ActionEvent event) { log.debug("onActionPrint - Not yet implemented"); }
    
    /** 수량 변경 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionQty(ActionEvent event) { log.debug("onActionQty - Not yet implemented"); }
    
    /** 현금 결제 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionCash(ActionEvent event) { log.debug("onActionCash - Not yet implemented"); }
    
    /** 카드 결제 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionCredit(ActionEvent event) { log.debug("onActionCredit - Not yet implemented"); }
    
    /** 현금 인출 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionCashout(ActionEvent event) { log.debug("onActionCashout - Not yet implemented"); }
    
    /** 서랍 열기 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onActionDrawer(ActionEvent event) { log.debug("onActionDrawer - Not yet implemented"); }
    
    /** POS 설정 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onPos(ActionEvent event) { log.debug("onPos - Not yet implemented"); }
    
    /** 출력 설정 버튼 클릭 핸들러 (향후 구현 예정) */
    @FXML private void onPrint(ActionEvent event) { log.debug("onPrint - Not yet implemented"); }
}