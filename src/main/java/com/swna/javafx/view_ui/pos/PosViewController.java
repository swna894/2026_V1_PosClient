package com.swna.javafx.view_ui.pos;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.infrastructure.scanner.BarcodeInputEngine;
import com.swna.javafx.viewmodel.pos.PosViewModel;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j // 🔥 Logger 활성화 (SLF4J)
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/PosView.fxml")
public class PosViewController {

    // =========================
    // ViewModel (비즈니스 상태 담당)
    // =========================
    private final PosViewModel vm;

    // =========================
    // Barcode Scanner Input Engine
    // =========================
    private final BarcodeInputEngine barcodeInputEngine = new BarcodeInputEngine();

    // =========================
    // UI Components
    // =========================
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

    @FXML private Label lblTotal;
    @FXML private Label lblQty;
    @FXML private Label lblScanStatus;

    //@FXML private TextField txtScan;

    // =========================
    // Initialize (View lifecycle)
    // =========================
    @FXML
    public void initialize() {

        log.info("[INIT] PosViewController initialized");

        bindTable();
        bindTop();
        setupBarcodeScanner();
    }

    // =========================
    // Barcode Scanner Setup
    // =========================
    private void setupBarcodeScanner() {

        log.info("[SCANNER] initializing BarcodeInputEngine");

        // 1. Scanner callback 등록
        barcodeInputEngine.setOnBarcode(this::handleBarcode);

        // 2. Scene attach (UI 로딩 이후)
        table.sceneProperty().addListener((obs, oldScene, scene) -> {

            if (scene != null) {

                log.info("[SCANNER] attaching to scene");

                barcodeInputEngine.attach(scene);
            }
        });
    }

    // =========================
    // 🔥 Barcode 처리 핵심
    // =========================
    private void handleBarcode(String code) {

        log.info("[SCAN] received barcode: {}", code);

        if (code == null || code.isBlank()) {
            log.warn("[SCAN] ignored empty barcode");
            return;
        }

        try {
            Platform.runLater(() -> {

                log.info("[SCAN] sending to ViewModel: {}", code);

                vm.scan(code);

                log.info("[SCAN] ViewModel scan executed: {}", code);
            });

        } catch (Exception e) {
            log.error("[SCAN] unexpected error: {}", code, e);
        }
    }

    // =========================
    // Table Binding
    // =========================
    private void bindTable() {

        table.setItems(vm.getItems());

        TableColumnUtil.createNumberColumn(table, colNo, 50);
        TableColumnUtil.makeLableColumn(colDelete, null, IconPaths.DELETE, 50, this::actionEvent );
        TableColumnUtil.makeStringColumn(colBarcode,PosItem::barcodeProperty, PosItem::setBarcode, false, null);
        TableColumnUtil.makeStringColumn(colDesc,PosItem::descriptionProperty, PosItem::setDescription, false, null);
        TableColumnUtil.makeLableColumn(colMinus, null, IconPaths.MINUS, 50, this::actionEvent );
        TableColumnUtil.makeIntegerColumn(colQty,PosItem::qtyProperty, PosItem::setQty, false, null);
        TableColumnUtil.makeLableColumn(colPlus, null, IconPaths.PLUS, 50, this::actionEvent );
        TableColumnUtil.makeDoubleColumn(colTotal,PosItem::sellingPriceProperty, PosItem::setSellingPrice, false, null);
        TableColumnUtil.makeDoubleColumn(colDiscount,PosItem::discountProperty, PosItem::setDiscount, false, null);
        TableColumnUtil.makeIntegerColumn(colStock,PosItem::stockProperty, PosItem::setStock, false, null);
        TableColumnUtil.makeLableColumn(colDiscountPrice, null, IconPaths.DISCOUNT, 50, this::actionEvent );
        TableColumnUtil.makeLableColumn(colChangePrice, null, IconPaths.PRICE_22, 50, this::actionEvent );
        TableColumnUtil.makeLableColumn(colMinus, null, IconPaths.MINUS, 50, this::actionEvent );
        TableColumnUtil.makeStringColumn(colComment,PosItem::commentProperty, PosItem::setComment, false, null);
        
        setupStockStyle();

        log.info("[TABLE] binding completed");
    }

    // =========================
    // Stock Style
    // =========================
    private void setupStockStyle() {

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

                if (value < 0) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
    }


    // =========================
    // Top Binding
    // =========================
    private void bindTop() {

        lblTotal.textProperty().bind( vm.totalAmountProperty().asString("Total: %.2f") );

        lblQty.textProperty().bind( vm.totalQtyProperty().asString("Total Qty: %d"));

        lblScanStatus.textProperty().bind(vm.scanStatusProperty());

        log.info("[BIND] top labels bound");
    }

    // =========================
    // Manual Scan (fallback input)
    // =========================
    // @FXML
    // private void onScan() {
    //     String code = txtScan.getText();
    //     log.info("[MANUAL SCAN] input: {}", code);
    //     if (code == null || code.isBlank()) {
    //         log.warn("[MANUAL SCAN] empty input ignored");
    //         return;
    //     }
    //     try {
    //         vm.scan(code);
    //         log.info("[MANUAL SCAN] processed: {}", code);
    //     } catch (Exception e) {
    //         log.error("[MANUAL SCAN] error: {}", code, e);
    //     }
    //     txtScan.clear();
    //     txtScan.requestFocus();
    // }

    // =========================
    // Table Action Event
    // =========================
    private void actionEvent(MouseEvent event) {
        log.debug("[TABLE ACTION] clicked: {}", event.getSource());
    }

    // =========================
    // Add / Remove
    // =========================
    @FXML
    private void onQuickAmount(ActionEvent e) {

        PosItem selected = table.getSelectionModel().getSelectedItem();

        if (selected != null) {
            log.debug("[ADD] {}", selected.getCode());
            vm.increaseQty(selected);
        }
    }

    @FXML
    private void onMin(MouseEvent  e) {
        PosItem selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) vm.increaseQty(selected);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void onMiddle(MouseEvent  e) {
         Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                 stage.setWidth(1024);
        stage.setHeight(768);
        stage.centerOnScreen();
        // PosItem selected = table.getSelectionModel().getSelectedItem();

        // if (selected != null) {
        //     log.debug("[REMOVE] {}", selected.getCode());
        //     vm.decreaseQty(selected);
        // }
    }

    @FXML
    private void onMax(MouseEvent  e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    @FXML
    private void onClose(MouseEvent  e) {

        log.info("[CLEAR] reset POS state");

        vm.clear();
        System.exit(0); 
    }

    // =========================
    // Payment
    // =========================
    @FXML
    private void onPayment() {

        double total = vm.totalAmountProperty().get();

        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("결제");
            alert.setContentText("총 결제 금액: " + total);
            alert.showAndWait();

        } catch (Exception e) {
            log.error("[PAYMENT] failed", e);
        }
    }
    
    @FXML
    private void onActionCart(ActionEvent event) {
        System.out.println("onActionCart");
    }

    @FXML
    private void onActionDiscountVolumn(ActionEvent event) {
        System.out.println("onActionDiscountVolumn");
    }

    @FXML
    private void onActionScanner(ActionEvent event) {
        System.out.println("onActionScanner");
    }

    @FXML
    private void onActionCancel(ActionEvent event) {
        System.out.println("onActionCancel");
    }

    @FXML
    private void onActionPrint(ActionEvent event) {
        System.out.println("onActionPrint");
    }

    @FXML
    private void onActionQty(ActionEvent event) {
        System.out.println("onActionQty");
    }

    @FXML
    private void onActionCash(ActionEvent event) {
        System.out.println("onActionCash");
    }

    @FXML
    private void onActionCredit(ActionEvent event) {
        System.out.println("onActionCredit");
    }

    @FXML
    private void onActionCashout(ActionEvent event) {
        System.out.println("onActionCashout");
    }

    @FXML
    private void onActionDrawer(ActionEvent event) {
        System.out.println("onActionDrawer");
    }

    @FXML
    private void onPos(ActionEvent event) {
        System.out.println("onPos");
    }

    @FXML
    private void onPrint(ActionEvent event) {
        System.out.println("onPrint");
    }

    @FXML
    private Button buttonCart1;

    @FXML
    private Button buttonCart2;

    @FXML
    private Button buttonCart3;

    @FXML
    private Button buttonDiscountVolumn;

    @FXML
    private Button buttonScanner;

    @FXML
    private Button buttonCancel;

    @FXML
    private Button buttonPrint;

    @FXML
    private Button buttonQty;

    @FXML
    private Button buttonCash;

    @FXML
    private Button buttonCredit;

    @FXML
    private Button buttonCashout;

    @FXML
    private Button buttonDrawer;

    @FXML
    private ImageView posImageView;

    @FXML
    private ImageView printImageView;
}