package com.swna.javafx.view_ui.pos;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.viewmodel.pos.PosViewModel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@RequiredArgsConstructor
@FxmlView("/view/pos/PosView.fxml")
public class PosViewController {

    private final PosViewModel vm;

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

    @FXML
    public void initialize() {
        bindTable();
        bindTop();
        txtScan.requestFocus();
    }

    // =========================
    // 🔥 핵심: Property 직접 바인딩
    // =========================
    private void bindTable() {

        table.setItems(vm.getItems());

        TableColumnUtil.makeButtonColumn(colDelete, null, IconPaths.DELETE, 50, this::actionEvent );
        TableColumnUtil.makeStringColumn(colBarcode,PosItem::barcodeProperty, PosItem::setBarcode, false, null);
        TableColumnUtil.makeStringColumn(colDesc,PosItem::descriptionProperty, PosItem::setDescription, false, null);
        TableColumnUtil.makeLableColumn(colMinus, null, IconPaths.MINUS, 50, this::actionEvent );
        TableColumnUtil.makeIntegerColumn(colQty,PosItem::qtyProperty, PosItem::setQty, false, null);
        TableColumnUtil.makeLableColumn(colPlus, null, IconPaths.PLUS, 50, this::actionEvent );
        TableColumnUtil.makeDoubleColumn(colTotal,PosItem::sellingPriceProperty, PosItem::setSellingPrice, false, null);
        TableColumnUtil.makeIntegerColumn(colStock,PosItem::stockProperty, PosItem::setStock, false, null);
        TableColumnUtil.makeLableColumn(colDiscount, null, IconPaths.DISCOUNT, 50, this::actionEvent );

        setupStockStyle();
        setupActionColumn();
    }

    // =========================
    // 재고 스타일
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
    // 액션 버튼
    // =========================
    private void setupActionColumn() {

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button plus = new Button("+");
            private final Button minus = new Button("-");

            {
                plus.setOnAction(e -> {
                    PosItem item = getTableRow().getItem();
                    if (item != null) vm.increaseQty(item);
                });

                minus.setOnAction(e -> {
                    PosItem item = getTableRow().getItem();
                    if (item != null) vm.decreaseQty(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, minus, plus));
                }
            }
        });
    }

    private void actionEvent(MouseEvent event) {
        System.out.println("Button clicked!");
    }
    // =========================
    // 상단 바인딩
    // =========================
    private void bindTop() {

        lblTotal.textProperty().bind(
                vm.totalAmountProperty().asString("Total: %.2f"));

        lblQty.textProperty().bind(
                vm.totalQtyProperty().asString("Total Qty: %d"));

        // 👉 ViewModel 책임으로 넘긴다
        lblScanStatus.textProperty().bind(vm.scanStatusProperty());
    }

    // =========================
    // 이벤트
    // =========================
    @FXML
    private void onScan() {

        String code = txtScan.getText();
        if (code == null || code.isBlank()) return;

        vm.scan(code);

        txtScan.clear();
        txtScan.requestFocus();
    }

    @FXML
    private void onQuickAmount(ActionEvent e) {

        PosItem selected = table.getSelectionModel().getSelectedItem();

        if (selected != null) {
            vm.increaseQty(selected);
        }
    }

    @FXML
    private void onAdd() {
        PosItem selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) vm.increaseQty(selected);
    }

    @FXML
    private void onRemove() {
        PosItem selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) vm.decreaseQty(selected);
    }

    @FXML
    private void onClear() {
        vm.clear();
    }

    @FXML
    private void onPayment() {

        double total = vm.totalAmountProperty().get();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("결제");
        alert.setContentText("총 결제 금액: " + total);
        alert.showAndWait();
    }
}