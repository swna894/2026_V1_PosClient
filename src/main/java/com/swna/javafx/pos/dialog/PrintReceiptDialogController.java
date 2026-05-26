package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/dialog/PrintReceiptDialog.fxml")
public class PrintReceiptDialogController extends BasePaymentDialog implements Initializable {

    private final SalesViewModel salesViewModel;

    // ========== ToolBar Controls ==========
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button todayBtn;
    @FXML private Button weekBtn;
    @FXML private Button monthBtn;
    @FXML private Button searchButton;
    @FXML private Button printButton;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    @FXML private Button previewPrintButton;
    
    // ========== Summary Labels ==========
    @FXML private Label summaryDateLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Label summaryDiscountLabel;
    @FXML private Label summaryCashLabel;
    @FXML private Label summaryCreditLabel;
    @FXML private Label summaryCashoutLabel;
    
    // ========== Receipt Info Labels ==========
    @FXML private Label receiptNoLabel;
    @FXML private Label previewReceiptNo;
    @FXML private Label previewDate;
    @FXML private Label previewTotalLabel;
    
    // ========== Loading & Status ==========
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label totalCountLabel;
    @FXML private Label lblStatus;
    
    // ========== TableViews ==========
    @FXML private TableView<SaleModel> receiptItemsTableView;
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private VBox previewArea;
    
    // ========== Receipt Table Columns ==========
    @FXML private TableColumn<SaleModel, String> noColumn;
    @FXML private TableColumn<SaleModel, String> printIconColumn;
    @FXML private TableColumn<SaleModel, String> receiptColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> amountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> discountColumn;
    
    // ========== Sale Items Table Columns ==========
    @FXML private TableColumn<SaleItemModel, String> itemNoColumn;
    @FXML private TableColumn<SaleItemModel, String> itemBarcodeColumn;
    @FXML private TableColumn<SaleItemModel, String> itemDescriptionColumn;
    @FXML private TableColumn<SaleItemModel, Integer> itemQtyColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemAmountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemDiscountColumn;

    private PrintReceiptCallback callback;
    private Button[] navButtons;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        log.info("[PrintReceiptDialog] initialize() started");
        
        this.navButtons = new Button[] { todayBtn, weekBtn, monthBtn, searchButton };

        setupBindings();
        setupDatePickers();
        setupTableColumns();      // TableColumnUtil 사용 (원래 방식)
        setupReceiptTableDirect(); // 추가: 직접 Cell Factory 설정 (강제 적용)
        setupTableBindings();
        setupButtonActions();
        setupSelectionListener();
        
        loadInitialData();
        enableFullWindowDrag();
        
        log.info("[PrintReceiptDialog] initialize() completed");
    }
    
    /**
     * Receipt Table 직접 설정 (강제 적용)
     */
    private void setupReceiptTableDirect() {
        log.info("[PrintReceiptDialog] setupReceiptTableDirect() started");
        
        if (receiptItemsTableView == null) {
            log.error("[PrintReceiptDialog] receiptItemsTableView is NULL!");
            return;
        }
        
        // 디버그: TableView가 비어있는지 확인
        Platform.runLater(() -> {
            log.info("[PrintReceiptDialog] Receipt TableView items size: {}", 
                receiptItemsTableView.getItems().size());
        });
        
        // 번호 컬럼 (자동 증가)
        if (noColumn != null) {
            noColumn.setCellValueFactory(cellData -> {
                int index = receiptItemsTableView.getItems().indexOf(cellData.getValue()) + 1;
                log.debug("[PrintReceiptDialog] NO column value: {}", index);
                return new SimpleObjectProperty<>(String.valueOf(index));
            });
            noColumn.setStyle("-fx-alignment: CENTER;");
            noColumn.setPrefWidth(50);
            log.info("[PrintReceiptDialog] receiptNoColumn configured");
        } else {
            log.warn("[PrintReceiptDialog] receiptNoColumn is NULL!");
        }
        
        // 영수증 번호 컬럼
        if (receiptColumn != null) {
            receiptColumn.setCellValueFactory(cellData -> {
                SaleModel sale = cellData.getValue();
                String value = "";
                if (sale != null) {
                    value = sale.getReceiptNo() != null ? sale.getReceiptNo() : "";
                    log.debug("[PrintReceiptDialog] Receipt number: {}", value);
                }
                return new SimpleStringProperty(value);
            });
            receiptColumn.setStyle("-fx-alignment: CENTER;");
            receiptColumn.setPrefWidth(180);
            log.info("[PrintReceiptDialog] receiptNumberColumn configured");
        } else {
            log.warn("[PrintReceiptDialog] receiptNumberColumn is NULL!");
        }
        
        // 총액 컬럼
        if (amountColumn != null) {
            amountColumn.setCellValueFactory(cellData -> {
                SaleModel sale = cellData.getValue();
                BigDecimal value = BigDecimal.ZERO;
                if (sale != null && sale.getSaleAmount() != null) {
                    value = sale.getSaleAmount();
                    log.debug("[PrintReceiptDialog] Total amount: {}", value);
                }
                return new SimpleObjectProperty<>(value);
            });
            amountColumn.setCellFactory(col -> new TableCell<SaleModel, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("$%,.2f", item));
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    }
                }
            });
            amountColumn.setPrefWidth(120);
            log.info("[PrintReceiptDialog] totalAmountColumn configured");
        } else {
            log.warn("[PrintReceiptDialog] totalAmountColumn is NULL!");
        }
        
        // 할인 컬럼
        if (discountColumn != null) {
            discountColumn.setCellValueFactory(cellData -> {
                SaleModel sale = cellData.getValue();
                BigDecimal value = BigDecimal.ZERO;
                if (sale != null && sale.getDiscountAmount() != null) {
                    value = sale.getDiscountAmount();
                }
                return new SimpleObjectProperty<>(value);
            });
            discountColumn.setCellFactory(col -> new TableCell<SaleModel, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("$%,.2f", item));
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    }
                }
            });
            discountColumn.setPrefWidth(100);
            log.info("[PrintReceiptDialog] discountAmountColumn configured");
        } else {
            log.warn("[PrintReceiptDialog] discountAmountColumn is NULL!");
        }
        
        // 강제 refresh
        receiptItemsTableView.refresh();
        log.info("[PrintReceiptDialog} setupReceiptTableDirect() completed");
    }

    private void setupTableColumns() {
        log.info("[PrintReceiptDialog] setupTableColumns() started");
        
        // Receipt Table Columns
        if (receiptItemsTableView != null && noColumn != null) {
            TableColumnUtil.createNumberColumn(receiptItemsTableView, noColumn, 50);
        }
        if (receiptColumn != null) {
            TableColumnUtil.makeStringColumn(receiptColumn, SaleModel::receiptNoProperty, null, false, true, TableColumnUtil.CENTER, null);
        }
        if (amountColumn != null) {
            TableColumnUtil.makeBigDecimalCurrencyColumn(amountColumn, SaleModel::saleAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        }
        if (discountColumn != null) {
            TableColumnUtil.makeBigDecimalCurrencyColumn(discountColumn, SaleModel::discountAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        }
        
        // Sale Items Table Columns
        if (itemBarcodeColumn != null) {
            TableColumnUtil.makeStringColumn(itemBarcodeColumn, SaleItemModel::barcodeProperty, null, false, true, TableColumnUtil.CENTER, null);
        }
        if (itemDescriptionColumn != null) {
            TableColumnUtil.makeStringColumn(itemDescriptionColumn, SaleItemModel::descriptionProperty, null, false, true, TableColumnUtil.LEFT, null);
        }
        if (itemQtyColumn != null) {
            TableColumnUtil.makeIntegerColumn(itemQtyColumn, SaleItemModel::quantityProperty, null, false, true, TableColumnUtil.CENTER, null);
        }
        if (itemPriceColumn != null) {
            TableColumnUtil.makeBigDecimalCurrencyColumn(itemPriceColumn, SaleItemModel::salePriceProperty, false, true, TableColumnUtil.RIGHT, null);
        }
        if (itemAmountColumn != null) {
            TableColumnUtil.makeBigDecimalCurrencyColumn(itemAmountColumn, SaleItemModel::saleAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        }
        if (itemDiscountColumn != null) {
            TableColumnUtil.makeBigDecimalCurrencyColumn(itemDiscountColumn, SaleItemModel::discountAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        }
        
        log.info("[PrintReceiptDialog] setupTableColumns() completed");
    }

    private void loadInitialData() {
        log.info("[PrintReceiptDialog] Loading initial data...");
        
        LocalDate today = LocalDate.now();
        if (startDatePicker != null) {
            startDatePicker.setValue(today);
        }
        if (endDatePicker != null) {
            endDatePicker.setValue(today);
        }
        
        salesViewModel.loadTodaySales();
        
        Platform.runLater(() -> {
            int size = salesViewModel.getSalesList().size();
            log.info("[PrintReceiptDialog] Sales list size: {}", size);
            
            // 디버그: SaleModel 데이터 출력
            for (SaleModel sale : salesViewModel.getSalesList()) {
                log.info("[PrintReceiptDialog] Sale - ReceiptNo: {}, Amount: {}", 
                    sale.getReceiptNo(), sale.getSaleAmount());
            }
            
            if (lblStatus != null) {
                if (size == 0) {
                    lblStatus.setText("No sales data found for today: " + today);
                } else {
                    lblStatus.setText("Ready - " + size + " receipts found");
                }
            }
            
            // TableView 강제 refresh
            if (receiptItemsTableView != null) {
                receiptItemsTableView.refresh();
                log.info("[PrintReceiptDialog] Receipt TableView items count after load: {}", 
                    receiptItemsTableView.getItems().size());
            }
        });
    }

    private void setupBindings() {
        if (startDatePicker != null) {
            startDatePicker.valueProperty().bindBidirectional(salesViewModel.startDateProperty());
        }
        if (endDatePicker != null) {
            endDatePicker.valueProperty().bindBidirectional(salesViewModel.endDateProperty());
        }
        
        if (progressIndicator != null) {
            salesViewModel.loadingProperty().addListener((obs, old, val) -> 
                Platform.runLater(() -> {
                    if (progressIndicator != null) {
                        progressIndicator.setVisible(val != null && val);
                    }
                })
            );
        }
        
        if (lblStatus != null) {
            salesViewModel.errorMessageProperty().addListener((obs, old, msg) -> 
                Platform.runLater(() -> {
                    if (lblStatus != null) {
                        lblStatus.setText(msg != null ? msg : "Ready");
                    }
                })
            );
        }
        
        if (totalCountLabel != null) {
            totalCountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> String.format("Total: %d", salesViewModel.totalCountProperty().get()),
                    salesViewModel.totalCountProperty()
                )
            );
        }
        
        // Summary Labels bindings
        if (summaryDateLabel != null) {
            summaryDateLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            );
        }
        
        if (summaryTotalLabel != null) {
            summaryTotalLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.totalSalesAmountProperty().get()),
                    salesViewModel.totalSalesAmountProperty()
                )
            );
        }
        
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.totalDiscountAmountProperty().get()),
                    salesViewModel.totalDiscountAmountProperty()
                )
            );
        }
        
        if (summaryCashLabel != null) {
            summaryCashLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.totalCashAmountProperty().get()),
                    salesViewModel.totalCashAmountProperty()
                )
            );
        }
        
        if (summaryCreditLabel != null) {
            summaryCreditLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.totalCreditAmountProperty().get()),
                    salesViewModel.totalCreditAmountProperty()
                )
            );
        }
        
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.totalCashoutAmountProperty().get()),
                    salesViewModel.totalCashoutAmountProperty()
                )
            );
        }
    }

    private void setupDatePickers() {
        if (startDatePicker != null && startDatePicker.getValue() == null) {
            startDatePicker.setValue(LocalDate.now());
        }
        if (endDatePicker != null && endDatePicker.getValue() == null) {
            endDatePicker.setValue(LocalDate.now());
        }
    }

    private void setupTableBindings() {
        if (receiptItemsTableView != null) {
            receiptItemsTableView.setItems(salesViewModel.getSalesList());
            log.info("[PrintReceiptDialog] Receipt table bound, list size: {}", 
                salesViewModel.getSalesList().size());
        }
        if (saleItemsTableView != null) {
            saleItemsTableView.setItems(salesViewModel.getSaleItemsList());
        }
    }

    private void setupButtonActions() {
        if (todayBtn != null) {
            todayBtn.setOnAction(e -> {
                log.info("[PrintReceiptDialog] Today button clicked");
                updateButtonSelection(todayBtn);
                salesViewModel.loadTodaySales();
                updateStatusAfterLoad();
            });
        }
        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                log.info("[PrintReceiptDialog] Week button clicked");
                updateButtonSelection(weekBtn);
                salesViewModel.loadThisWeekSales();
                updateStatusAfterLoad();
            });
        }
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                log.info("[PrintReceiptDialog] Month button clicked");
                updateButtonSelection(monthBtn);
                salesViewModel.loadThisMonthSales();
                updateStatusAfterLoad();
            });
        }
        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                log.info("[PrintReceiptDialog] Search button clicked");
                updateButtonSelection(searchButton);
                salesViewModel.loadSalesByDateRange();
                updateStatusAfterLoad();
                
                if (callback != null && startDatePicker != null && endDatePicker != null) {
                    callback.onSearch(startDatePicker.getValue(), endDatePicker.getValue());
                }
            });
        }
        if (printButton != null) {
            printButton.setOnAction(e -> handlePrint());
        }
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> {
                log.info("[PrintReceiptDialog] Refresh button clicked");
                salesViewModel.refresh();
                updateStatusAfterLoad();
            });
        }
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleCancel());
        }
        if (previewPrintButton != null) {
            previewPrintButton.setOnAction(e -> handlePreviewPrint());
        }
    }
    
    private void updateStatusAfterLoad() {
        Platform.runLater(() -> {
            int size = salesViewModel.getSalesList().size();
            log.info("[PrintReceiptDialog] Load completed, found {} receipts", size);
            
            if (receiptItemsTableView != null) {
                receiptItemsTableView.refresh();
                log.info("[PrintReceiptDialog] Receipt TableView items count after refresh: {}", 
                    receiptItemsTableView.getItems().size());
            }
            
            if (lblStatus != null) {
                if (size == 0) {
                    lblStatus.setText("No receipts found for selected date range");
                } else {
                    lblStatus.setText("Ready - " + size + " receipts found");
                }
            }
        });
    }

    private void setupSelectionListener() {
        if (receiptItemsTableView != null) {
            receiptItemsTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        log.info("[PrintReceiptDialog] Receipt selected: {}", newVal.getReceiptNo());
                        if (salesViewModel.selectedSaleProperty().get() != newVal) {
                            salesViewModel.selectedSaleProperty().set(newVal);
                            salesViewModel.onSelectedSaleChanged(newVal);
                            updateReceiptPreview(newVal);
                        }
                    }
                });
        }
        
        salesViewModel.selectedSaleProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null && receiptItemsTableView != null) {
                    log.info("[PrintReceiptDialog] Selected sale changed to: {}", newVal.getReceiptNo());
                    var selectionModel = receiptItemsTableView.getSelectionModel();
                    if (selectionModel.getSelectedItem() != newVal) {
                        selectionModel.select(newVal);
                        receiptItemsTableView.scrollTo(newVal);
                    }
                    updateReceiptPreview(newVal);
                }
            });
        });
    }

    private void updateReceiptPreview(SaleModel sale) {
        if (sale == null) return;
        
        setReceiptNumber(sale.getReceiptNo());
        setPreviewDate(sale.getPaymentDateTime() != null ? 
            sale.getPaymentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        setPreviewTotal(sale.getSaleAmount() != null ? sale.getSaleAmount().doubleValue() : 0);
    }

    private void updateButtonSelection(Button selectedBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                String selectedStyle = "-fx-background-color: #3498db; -fx-text-fill: white;";
                String defaultStyle = "";
                
                if (btn == selectedBtn) {
                    btn.setStyle(selectedStyle);
                } else {
                    btn.setStyle(defaultStyle);
                }
            }
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
        return currencyFormat.format(amount);
    }

    public void initData(PrintReceiptCallback callback) {
        this.callback = callback;
    }

    @FXML
    private void handlePrint() {
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            log.warn("[PrintReceiptDialog] No receipt selected");
            if (lblStatus != null) {
                lblStatus.setText("Please select a receipt to print");
            }
            return;
        }
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Printing receipt: {}", receiptNo);
        
        if (callback != null) {
            callback.onPrint(receiptNo);
        }
        if (lblStatus != null) {
            lblStatus.setText("Printing receipt: " + receiptNo);
        }
    }

    @FXML
    private void handlePreviewPrint() {
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            log.warn("[PrintReceiptDialog] No receipt selected for preview");
            if (lblStatus != null) {
                lblStatus.setText("Please select a receipt to preview");
            }
            return;
        }
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Previewing receipt: {}", receiptNo);
        
        if (callback != null) {
            callback.onPreview(receiptNo);
        }
    }

    // ========== BasePaymentDialog 구현 ==========
    
    @Override
    protected void handleConfirm() {
        if (searchButton != null) {
            searchButton.fire();
        }
    }

    @Override
    protected TextField getFocusField() {
        return startDatePicker != null ? startDatePicker.getEditor() : null;
    }

    @Override
    @FXML
    protected void handleCancel() {
        close();
    }

    private void close() {
        if (receiptNoLabel != null && receiptNoLabel.getScene() != null) {
            Stage stage = (Stage) receiptNoLabel.getScene().getWindow();
            stage.close();
        }
    }

    // ========== Public Methods for UI Update ==========
    
    public void setReceiptNumber(String receiptNo) {
        if (receiptNoLabel != null) {
            receiptNoLabel.setText(receiptNo);
        }
        if (previewReceiptNo != null) {
            previewReceiptNo.setText("Receipt #: " + receiptNo);
        }
    }
    
    public void setPreviewDate(String date) {
        if (previewDate != null) {
            previewDate.setText("Date: " + date);
        }
    }
    
    public void setPreviewTotal(double total) {
        if (previewTotalLabel != null) {
            previewTotalLabel.setText(String.format("$%.2f", total));
        }
    }

    @FunctionalInterface
    public interface PrintReceiptCallback {
        void onSearch(LocalDate startDate, LocalDate endDate);
        
        default void onPrint(String receiptNo) {
            log.info("Default print: {}", receiptNo);
        }
        
        default void onPreview(String receiptNo) {
            log.info("Default preview: {}", receiptNo);
        }
    }
}