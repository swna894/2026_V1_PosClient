package com.swna.javafx.admin.unpacking;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.dialog.ReadExcelController;
import com.swna.javafx.admin.unpacking.model.Unpack;
import com.swna.javafx.admin.unpacking.model.UnpackItem;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.common.util.AlertDialog;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;
import com.swna.javafx.pos.manager.BarcodeScannerManager;
import com.swna.javafx.pos.manager.BarcodeScannerManager.ScanHandle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@FxmlView("/view/admin/unpacking-view.fxml")
@RequiredArgsConstructor
public class UnPackingController {

    private static final PseudoClass NEW_ITEM_PSEUDO = PseudoClass.getPseudoClass("new-item");
    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\d*(\\.\\d*)?$");

    private final NavigationService navigationService;
    private final UnpackingViewModel viewModel;
    private final SafeBarcodeScanner safeBarcodeScanner;
    private final BarcodeScannerManager scannerManager;
    private final AlertDialog alertDialog;

    /** 스캔 일시 정지/재개 제어용 (initialize에서 설정) */
    private ScanHandle scanHandle;

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

        // 바코드 스캐너 설정 — 반환된 핸들로 알림 중 일시 정지/재개를 제어
        scanHandle = scannerManager.setup(tableViewItems, safeBarcodeScanner, this::handleBarcode);
        Platform.runLater(() -> tableViewItems.requestFocus());
    }

    private void wireControls() {
        datePickerStart.setValue(viewModel.startDateProperty().get());
        datePickerEnd.setValue(viewModel.endDateProperty().get());
        datePickerStart.valueProperty().bindBidirectional(viewModel.startDateProperty());
        datePickerEnd.valueProperty().bindBidirectional(viewModel.endDateProperty());
        datePickerStart.setOnAction(e -> viewModel.reload());
        datePickerEnd.setOnAction(e -> viewModel.reload());

        // 숫자 및 소수점만 입력 허용 (예: 2, 2.3, 0.5)
        textFieldPriceMultiplier.textProperty().bindBidirectional(viewModel.priceMultiplierProperty());
        textFieldPriceMultiplier.setTextFormatter(new TextFormatter<>(change ->
                PRICE_PATTERN.matcher(change.getControlNewText()).matches() ? change : null));

        labelUnpacksSummary.textProperty().bind(viewModel.inspectionSummaryProperty());
        labelItemsSummary.textProperty().bind(viewModel.productSummaryProperty());

        comboBoxSupplier.setItems(viewModel.getSuppliers());
        comboBoxSupplier.setButtonCell(supplierListCell());
        comboBoxSupplier.setCellFactory(lv -> supplierListCell());
        comboBoxSupplier.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> viewModel.filterBySupplier(newValue));

        comboBoxConfirmFilter.setItems(viewModel.getConfirmFilterOptions());
        comboBoxConfirmFilter.getSelectionModel().selectFirst();
        comboBoxConfirmFilter.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> applyConfirmFilter());
    }

    /**
     * 현재 선택된 confirm 필터를 테이블에 적용.
     * 필터 결과는 복사본 리스트이므로, 원본이 바뀌면(스캔 등) 다시 호출해야 화면과 맞습니다.
     */
    private void applyConfirmFilter() {
        String option = comboBoxConfirmFilter.getSelectionModel().getSelectedItem();
        if (option == null) return;

        List<UnpackItem> filtered = viewModel.filterByConfirmStatus(option);
        tableViewItems.setItems(FXCollections.observableArrayList(filtered));
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

    // ---------------- Barcode Handling ----------------

    private void handleBarcode(String code) {
        if (code == null || code.isBlank()) return;

        Platform.runLater(() -> {
            // 알림 표시 중 큐에 이미 쌓여 있던 스캔도 여기서 차단
            if (scanHandle != null && scanHandle.isPaused()) {
                log.debug("Scan ignored while paused: {}", code);
                return;
            }

            log.info("Barcode scanned: {}", code);
            UnpackItem targetItem = viewModel.processScannedCode(code);

            if (targetItem == null) {
                log.warn("Scanned code not found in item list: {}", code);
                showNoBarcodeAlert(code);
                return;
            }
            revealScannedItem(targetItem);
        });
    }

    /**
     * 스캔된 항목을 테이블에서 선택하고 스크롤.
     * confirm 필터로 복사본 리스트를 보고 있다면 먼저 다시 적용해 원본 변경을 반영합니다.
     */
    private void revealScannedItem(UnpackItem target) {
        if (tableViewItems.getItems() != viewModel.getUnpackItems()) {
            applyConfirmFilter();
        }
        tableViewItems.refresh();

        int index = tableViewItems.getItems().indexOf(target);
        if (index >= 0) {
            tableViewItems.getSelectionModel().clearAndSelect(index);
            tableViewItems.scrollTo(index);
        } else {
            log.info("Scanned item is hidden by the current confirm filter: {}", target.getBarcode());
        }
        tableViewItems.requestFocus();
    }

    /**
     * 미등록 바코드 알림. 표시 동안 스캔을 정지하고, 닫히면 재개 + 포커스 복원.
     */
    private void showNoBarcodeAlert(String code) {
        if (scanHandle != null) {
            scanHandle.pause();
        }
        alertDialog.dialogString(
                "No barcode matched\nCode : " + code,
                ownerStage(),
                () -> {
                    if (scanHandle != null) {
                        scanHandle.resume();   // 스캔 재개 + tableViewItems 포커스 복원
                    }
                });
    }

    private Stage ownerStage() {
        if (tableViewItems.getScene() == null) return null;
        Window window = tableViewItems.getScene().getWindow();
        return (window instanceof Stage stage) ? stage : null;
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
        tableViewItems.setRowFactory(tv -> createItemRow());

        TableColumnUtil.createNumberColumn(tableViewItems, colItemNo, 50);
        TableColumnUtil.createCheckBoxHeaderColumn(tableViewItems, colItemConfirm, UnpackItem::confirmProperty, "", 60);
        TableColumnUtil.createCheckBoxTextColumn(tableViewItems, colItemIsSaved, UnpackItem::isSavedProperty, "Added", 65, true);

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

    /**
     * isNew 상태를 PseudoClass(new-item)로 표시하고, 행 클릭 시 confirm=true 처리하는 Row.
     */
    private TableRow<UnpackItem> createItemRow() {
        TableRow<UnpackItem> row = new TableRow<>() {
            private final ChangeListener<Boolean> isNewListener =
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
                pseudoClassStateChanged(NEW_ITEM_PSEUDO, Boolean.TRUE.equals(isNew));
            }
        };

        row.setOnMouseClicked(event -> {
            if (!row.isEmpty() && row.getItem() != null) {
                row.getItem().setConfirm(true);
            }
        });
        return row;
    }
}
