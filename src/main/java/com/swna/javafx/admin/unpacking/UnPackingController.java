package com.swna.javafx.admin.unpacking;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.dialog.ReadExcelController;
import com.swna.javafx.admin.unpacking.model.Unpack;
import com.swna.javafx.admin.unpacking.model.UnpackItem;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import com.swna.javafx.pos.manager.BarcodeScannerManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@FxmlView("/view/admin/unpacking-view.fxml")
@RequiredArgsConstructor
public class UnPackingController {

    // PseudoClass 상수 정의
    private static final PseudoClass NEW_ITEM_PSEUDO = PseudoClass.getPseudoClass("new-item");
    
    private final NavigationService navigationService;
    private final UnpackingViewModel viewModel;
    private final SafeBarcodeScanner safeBarcodeScanner;
    private final BarcodeScannerManager scannerManager;

    @FXML private BorderPane borderPane;
    @FXML private ToolBar mainToolBar;
    @FXML private BorderPane leftPane;
    @FXML private BorderPane rightPane;
    @FXML private SplitPane splitPane;

    @FXML private Button buttonReload;
    @FXML private Button buttonExcelRead;
    @FXML private Button buttonAddStock;
    @FXML private Button buttonEPrice;
    @FXML private Button buttonDelete;

    @FXML private TextField textFieldSearch;
    @FXML private TextField textFieldPriceMultiplier;
    @FXML private DatePicker datePickerStart;
    @FXML private DatePicker datePickerEnd;
    @FXML private Label labelUnpacksSummary;
    @FXML private Label labelItemsSummary;
    @FXML private ComboBox<Supplier> comboBoxSupplier;
    @FXML private ComboBox<String> comboBoxConfirmFilter;
    @FXML private TableView<Unpack> tableViewUnpacks;
    @FXML private TableView<UnpackItem> tableViewItems;

    @FXML private TableColumn<Unpack, Double> colAmount;
    @FXML private TableColumn<Unpack, String> colComment;
    @FXML private TableColumn<Unpack, String> colInvoice;
    @FXML private TableColumn<Unpack, String> colNo;
    @FXML private TableColumn<Unpack, Boolean> colSelected;

    @FXML private TableColumn<UnpackItem, String> colItemNo;
    @FXML private TableColumn<UnpackItem, Boolean> colItemConfirm;
    @FXML private TableColumn<UnpackItem, String> colItemBarcode;
    @FXML private TableColumn<UnpackItem, String> colItemDescription;
    @FXML private TableColumn<UnpackItem, String> colItemCategory;
    @FXML private TableColumn<UnpackItem, BigDecimal> colItemOldPriceIn;
    @FXML private TableColumn<UnpackItem, BigDecimal> colItemPriceIn;
    @FXML private TableColumn<UnpackItem, Integer> colItemQty;
    @FXML private TableColumn<UnpackItem, Integer> colItemMinStock;
    @FXML private TableColumn<UnpackItem, BigDecimal> colItemPriceOutEstimated;
    @FXML private TableColumn<UnpackItem, BigDecimal> colItemPriceOut;
    @FXML private TableColumn<UnpackItem, Boolean> colItemIsSaved;
    @FXML private TableColumn<UnpackItem, String> colItemCode;
    @FXML private TableColumn<UnpackItem, String> colItemComment;

    // ---------------- Button Event Handlers ----------------

    @FXML
    private void handleExcelRead(ActionEvent event) {
        navigationService.openModalWindow(ReadExcelController.class, "Read Unpacking Excel");
    }

    @FXML
    private void handleReload(ActionEvent event) {
        log.info("[Action] Reload button clicked");
        viewModel.reload();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        log.info("[Action] Delete button clicked");
        viewModel.deleteSelectedInspections();
    }

    @FXML
    private void handleEPrice(ActionEvent event) {
        log.info("[Action] E-Price button clicked");
        viewModel.applyEstimatedPriceMultiplier();
    }

    @FXML
    private void handleAddStock(ActionEvent event) {
        log.info("[Action] Add Stock button clicked");
        viewModel.addStockForConfirmedItems();
    }

    // ---------------- Public API ----------------

    public ObservableList<Supplier> getSuppliers() {
        return viewModel.getSuppliers();
    }

    public void updateTableViewInspection(Unpack unpack) {
        viewModel.addInspection(unpack);
    }

    public void updateTableViewInspection(List<Unpack> inspections) {
        viewModel.reload();
    }

    // ---------------- FXML Initializer ----------------

    @FXML
    private void initialize() {
        viewModel.initialize();
        wireControls();
        configureUnpackTable();
        configureItemsTable();

        // 5. 바코드 스캐너 설정
        scannerManager.setup(tableViewItems, safeBarcodeScanner, this::handleBarcode);
        Platform.runLater(() -> tableViewItems.requestFocus());
    }

    private void wireControls() {
        datePickerStart.setValue(viewModel.startDateProperty().get());
        datePickerEnd.setValue(viewModel.endDateProperty().get());
        datePickerStart.valueProperty().bindBidirectional(viewModel.startDateProperty());
        datePickerEnd.valueProperty().bindBidirectional(viewModel.endDateProperty());
        datePickerStart.setOnAction(e -> viewModel.reload());
        datePickerEnd.setOnAction(e -> viewModel.reload());

        // 2. textFieldPriceMultiplier 양방향 바인딩
        textFieldPriceMultiplier.textProperty().bindBidirectional(viewModel.priceMultiplierProperty());
        // 3. 숫자 및 소수점만 입력 허용하는 TextFormatter 적용
        textFieldPriceMultiplier.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // 빈 값("") 또는 소수점을 포함한 숫자 패턴만 허용 (예: 2, 2.3, 0.5)
            if (newText.isEmpty() || newText.matches("^\\d*\\.?\\d*$")) {
                return change;
            }
            return null; // 조건에 맞지 않는 입력(문자 등)은 무시
        }));

        labelUnpacksSummary.textProperty().bind(viewModel.inspectionSummaryProperty());
        labelItemsSummary.textProperty().bind(viewModel.productSummaryProperty());

        comboBoxSupplier.setItems(viewModel.getSuppliers());
        comboBoxSupplier.setButtonCell(supplierListCell());
        comboBoxSupplier.setCellFactory(lv -> supplierListCell());
        comboBoxSupplier.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> viewModel.filterBySupplier(newValue));

        comboBoxConfirmFilter.setItems(viewModel.getConfirmFilterOptions());
        comboBoxConfirmFilter.getSelectionModel().selectFirst();
        comboBoxConfirmFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            List<UnpackItem> filtered = viewModel.filterByConfirmStatus(newValue);
            tableViewItems.setItems(FXCollections.observableArrayList(filtered));
        });
    }

    private ListCell<Supplier> supplierListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getCompany());
            }
        };
    }

        // ========== Handler Methods ==========
    
    private void handleBarcode(String code) {
        if (code == null || code.isBlank()) return;
        Platform.runLater(() -> viewModel.scan(code)); 
  
    }

    // ---------------- Table Configurations ----------------

    private void configureUnpackTable() {
        tableViewUnpacks.setTableMenuButtonVisible(true);
        tableViewUnpacks.setItems(viewModel.getUnpacks());
        tableViewUnpacks.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> viewModel.selectInspection(newValue));

        TableColumnUtil.createNumberColumn(tableViewUnpacks, colNo, 50);
        TableColumnUtil.createCheckBoxHeaderColumn(tableViewUnpacks, colSelected, Unpack::selectedProperty, "", 50);

        TableColumnUtil.makeStringColumn(colInvoice, Unpack::invoiceProperty, Unpack::setInvoice, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeCurrencyColumn(colAmount, Unpack::amountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeStringColumn(colComment, Unpack::commentProperty, Unpack::setComment, false, true, TableColumnUtil.CENTER, null);
    }

    private void configureItemsTable() {
        tableViewItems.setTableMenuButtonVisible(true);
        tableViewItems.setItems(viewModel.getUnpackItems());

        // isNew가 true인 경우 글자색을 붉은색으로 표시
        // PseudoClass 기반 RowFactory 적용
        tableViewItems.setRowFactory(tv -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<Boolean> isNewListener = 
                    (obs, oldVal, newVal) -> updatePseudoState(newVal);

            @Override
            protected void updateItem(UnpackItem item, boolean empty) {
                UnpackItem prevItem = getItem();
                if (prevItem != null) {
                    prevItem.isNewProperty().removeListener(isNewListener);
                }

                super.updateItem(item, empty);

                if (empty || item == null) {
                    updatePseudoState(false);
                } else {
                    item.isNewProperty().addListener(isNewListener);
                    updatePseudoState(item.getIsNew());
                }
            }

            private void updatePseudoState(Boolean isNew) {
                // :new-item 의사 클래스 상태 변경
                pseudoClassStateChanged(NEW_ITEM_PSEUDO, Boolean.TRUE.equals(isNew));
            }
        });

        TableColumnUtil.createNumberColumn(tableViewItems, colItemNo, 50);
        TableColumnUtil.createCheckBoxHeaderColumn(tableViewItems, colItemConfirm, UnpackItem::confirmProperty, "", 60);
        TableColumnUtil.createCheckBoxTextColumn(tableViewItems, colItemIsSaved, UnpackItem::isSavedProperty, "Added", 65, false);

        TableColumnUtil.makeStringColumn(colItemBarcode, UnpackItem::barcodeProperty, UnpackItem::setBarcode, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(colItemDescription, UnpackItem::descriptionProperty, UnpackItem::setDescription, true, true, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeStringColumn(colItemCode, UnpackItem::codeProperty, UnpackItem::setCode, false, true, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeStringColumn(colItemComment, UnpackItem::commentProperty, UnpackItem::setComment, true, true, TableColumnUtil.LEFT, null);

        TableColumnUtil.makeIntegerColumn(colItemQty, UnpackItem::qtyProperty, UnpackItem::setQty, true, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeIntegerColumn(colItemMinStock, UnpackItem::minStockProperty, UnpackItem::setMinStock, true, true, TableColumnUtil.CENTER, null);

        TableColumnUtil.makeBigDecimalCurrencyColumn(colItemOldPriceIn, UnpackItem::oldPriceinProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(colItemPriceIn, UnpackItem::priceinProperty, true, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(colItemPriceOutEstimated, UnpackItem::priceoutEstimatedProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(colItemPriceOut, UnpackItem::priceoutProperty, true, true, TableColumnUtil.RIGHT, null);

        if (colItemCategory != null) {
            colItemCategory.setOnEditCommit(event -> event.getRowValue().setCategory(event.getNewValue()));
        }
    }
}