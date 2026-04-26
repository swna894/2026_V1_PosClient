package com.swna.javafx.view_ui.pos;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.infrastructure.scanner.BarcodeInputEngine;
import com.swna.javafx.viewmodel.pos.PosViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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

    @FXML private TableColumn<PosItem, String> colBarcode;
    @FXML private TableColumn<PosItem, String> colDesc;
    @FXML private TableColumn<PosItem, Integer> colQty;
    @FXML private TableColumn<PosItem, Integer> colStock;
    @FXML private TableColumn<PosItem, Double> colPrice;
    @FXML private TableColumn<PosItem, Double> colTotal;

    @FXML private TableColumn<PosItem, Void> colAction;
    @FXML private TableColumn<PosItem, Void> colDelete;
    @FXML private TableColumn<PosItem, Void> colMinus;
    @FXML private TableColumn<PosItem, Void> colPlus;
    @FXML private TableColumn<PosItem, Void> colDiscount;

    @FXML private Label lblTotal;
    @FXML private Label lblQty;
    @FXML private Label lblScanStatus;

    @FXML private TextField txtScan;

    // =========================
    // Initialize (View lifecycle)
    // =========================
    @FXML
    public void initialize() {

        log.info("[INIT] PosViewController initialized");

        bindTable();
        bindTop();
        setupBarcodeScanner();

        txtScan.requestFocus();
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

        TableColumnUtil.makeButtonColumn(colDelete, null, IconPaths.DELETE, 50, this::actionEvent);

        TableColumnUtil.makeStringColumn(colBarcode, PosItem::barcodeProperty, PosItem::setBarcode, false, null);
        TableColumnUtil.makeStringColumn(colDesc, PosItem::descriptionProperty, PosItem::setDescription, false, null);

        TableColumnUtil.makeIntegerColumn(colQty, PosItem::qtyProperty, PosItem::setQty, false, null);

        TableColumnUtil.makeDoubleColumn(colTotal, PosItem::sellingPriceProperty, PosItem::setSellingPrice, false, null);

        TableColumnUtil.makeIntegerColumn(colStock, PosItem::stockProperty, PosItem::setStock, false, null);

        setupStockStyle();
        setupActionColumn();

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
    // Action Buttons (+ / -)
    // =========================
    private void setupActionColumn() {

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button plus = new Button("+");
            private final Button minus = new Button("-");

            {
                plus.setOnAction(e -> {
                    PosItem item = getTableRow().getItem();
                    if (item != null) {
                        log.debug("[ACTION] increase qty: {}", item.getCode());
                        vm.increaseQty(item);
                    }
                });

                minus.setOnAction(e -> {
                    PosItem item = getTableRow().getItem();
                    if (item != null) {
                        log.debug("[ACTION] decrease qty: {}", item.getCode());
                        vm.decreaseQty(item);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                setGraphic(empty ? null : new HBox(5, minus, plus));
            }
        });
    }

    // =========================
    // Top Binding
    // =========================
    private void bindTop() {

        lblTotal.textProperty().bind(
                vm.totalAmountProperty().asString("Total: %.2f")
        );

        lblQty.textProperty().bind(
                vm.totalQtyProperty().asString("Total Qty: %d")
        );

        lblScanStatus.textProperty().bind(vm.scanStatusProperty());

        log.info("[BIND] top labels bound");
    }

    // =========================
    // Manual Scan (fallback input)
    // =========================
    @FXML
    private void onScan() {

        String code = txtScan.getText();

        log.info("[MANUAL SCAN] input: {}", code);

        if (code == null || code.isBlank()) {
            log.warn("[MANUAL SCAN] empty input ignored");
            return;
        }

        try {
            vm.scan(code);
            log.info("[MANUAL SCAN] processed: {}", code);

        } catch (Exception e) {
            log.error("[MANUAL SCAN] error: {}", code, e);
        }

        txtScan.clear();
        txtScan.requestFocus();
    }

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
    private void onAdd() {
        PosItem selected = table.getSelectionModel().getSelectedItem();

        if (selected != null) {
            log.debug("[ADD] {}", selected.getCode());
            vm.increaseQty(selected);
        }
    }

    @FXML
    private void onRemove() {
        PosItem selected = table.getSelectionModel().getSelectedItem();

        if (selected != null) {
            log.debug("[REMOVE] {}", selected.getCode());
            vm.decreaseQty(selected);
        }
    }

    // =========================
    // Clear
    // =========================
    @FXML
    private void onClear() {

        log.info("[CLEAR] reset POS state");

        vm.clear();
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
}