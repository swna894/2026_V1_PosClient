package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
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
    @FXML private Label totalCountLabel;
    @FXML private Label lblStatus;
    
    // ========== TableViews ==========
    @FXML private TableView<SaleModel> receiptItemsTableView;
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private VBox previewArea;
    
    // ========== WebView for Receipt Preview ==========
    @FXML private WebView receiptWebView;
    
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
    
    // 현재 선택된 영수증의 아이템을 저장할 변수
    private List<SaleItemModel> currentReceiptItems;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        log.info("[PrintReceiptDialog] initialize() started");
        
        this.navButtons = new Button[] { todayBtn, weekBtn, monthBtn, searchButton };

        setupBindings();
        setupDatePickers();
        setupTableColumns();
        setupTableBindings();
        setupButtonActions();
        setupSelectionListener();
        setupSaleItemsListener();

        loadInitialData();
        enableFullWindowDrag();
        
        log.info("[PrintReceiptDialog] initialize() completed");
    }
    

    private void setupTableColumns() {
        log.info("[PrintReceiptDialog] setupTableColumns() started");
        
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
        
        if (saleItemsTableView != null && itemNoColumn != null) {
            TableColumnUtil.createNumberColumn(saleItemsTableView, itemNoColumn, 50);
        }
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
    }

    private void loadInitialData() {
        log.info("[PrintReceiptDialog] Loading initial data...");
        
        LocalDate today = LocalDate.now();
        if (startDatePicker != null) startDatePicker.setValue(today);
        if (endDatePicker != null) endDatePicker.setValue(today);
        
        salesViewModel.loadTodaySales();
        
        Platform.runLater(() -> {
            int size = salesViewModel.getSalesList().size();
            updateStatusMessage(size == 0 ? "No sales data found for today: " + today : "Ready - " + size + " receipts found");
            if (receiptItemsTableView != null) receiptItemsTableView.refresh();
            
            // 첫 번째 영수증 자동 선택
            selectFirstReceiptAfterLoad();
        });
    }

    private void setupBindings() {
        log.info("[PrintReceiptDialog] Setting up bindings...");
        
        // DatePicker 바인딩
        if (startDatePicker != null) {
            startDatePicker.valueProperty().bindBidirectional(salesViewModel.startDateProperty());
        }
        if (endDatePicker != null) {
            endDatePicker.valueProperty().bindBidirectional(salesViewModel.endDateProperty());
        }
        
        // Status Message 바인딩
        if (lblStatus != null) {
            salesViewModel.errorMessageProperty().addListener((obs, old, msg) -> 
                Platform.runLater(() -> updateStatusMessage(msg != null ? msg : "Ready"))
            );
        }

        // =========================================================
        // Receipt No Label 바인딩 (추가)
        // =========================================================
        if (receiptNoLabel != null) {
            receiptNoLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null && selected.getReceiptNo() != null) {
                            return selected.getReceiptNo();
                        }
                        return "No receipt selected";
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
        
        // =========================================================
        // Toolbar Summary - 선택된 영수증의 아이템 기준으로 변경
        // =========================================================
        
        // Summary Date - 선택된 영수증의 날짜로 표시
        if (summaryDateLabel != null) {
            summaryDateLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null && selected.getPaymentDateTime() != null) {
                            return selected.getPaymentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }
                        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
        
        // Total Amount - 선택된 영수증의 아이템 총액
        if (summaryTotalLabel != null) {
            summaryTotalLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.itemTotalAmountProperty().get()),
                    salesViewModel.itemTotalAmountProperty()
                )
            );
        }
        
        // Discount Amount - 선택된 영수증의 아이템 할인总额
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.itemTotalDiscountProperty().get()),
                    salesViewModel.itemTotalDiscountProperty()
                )
            );
        }
        
        // Cash Amount - 선택된 영수증의 현금 결제액
        if (summaryCashLabel != null) {
            summaryCashLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null) {
                            return formatCurrency(selected.getCashAmount());
                        }
                        return "$0.00";
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
        
        // Credit Amount - 선택된 영수증의 카드 결제액
        if (summaryCreditLabel != null) {
            summaryCreditLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null) {
                            return formatCurrency(selected.getCreditAmount());
                        }
                        return "$0.00";
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
        
        // Cashout Amount - 선택된 영수증의 현금 인출액
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null) {
                            return formatCurrency(selected.getCashoutAmount());
                        }
                        return "$0.00";
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
        
        // Total Count - 선택된 영수증의 아이템 개수
        if (totalCountLabel != null) {
            totalCountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> String.format("Items: %d", salesViewModel.itemTotalQtyProperty().get()),
                    salesViewModel.itemTotalQtyProperty()
                )
            );
        }
        
        log.info("[PrintReceiptDialog] Bindings setup completed");
    } 
    /**
     * Summary Date 업데이트
     */
    private void updateSummaryDate() {
        if (summaryDateLabel != null) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            summaryDateLabel.setText(todayStr);
        }
    }
    
    /**
     * Summary 값들을 강제로 새로고침 (ViewModel 데이터 직접 읽기)
     */
    private void refreshSummaryValues() {
        if (summaryTotalLabel != null) {
            summaryTotalLabel.setText(formatCurrency(salesViewModel.totalSalesAmountProperty().get()));
        }
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.setText(formatCurrency(salesViewModel.totalDiscountAmountProperty().get()));
        }
        if (summaryCashLabel != null) {
            summaryCashLabel.setText(formatCurrency(salesViewModel.totalCashAmountProperty().get()));
        }
        if (summaryCreditLabel != null) {
            summaryCreditLabel.setText(formatCurrency(salesViewModel.totalCreditAmountProperty().get()));
        }
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.setText(formatCurrency(salesViewModel.totalCashoutAmountProperty().get()));
        }
        if (totalCountLabel != null) {
            totalCountLabel.setText(String.format("Total: %d", salesViewModel.totalCountProperty().get()));
        }
        
        log.debug("[Refresh] Summary values refreshed manually");
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
        }
        if (saleItemsTableView != null) {
            saleItemsTableView.setItems(salesViewModel.getSaleItemsList());
        }
    }

    private void setupButtonActions() {
        if (todayBtn != null) {
            todayBtn.setOnAction(e -> {
                updateButtonSelection(todayBtn);
                salesViewModel.loadTodaySales();
                updateAfterDataLoad();
            });
        }
        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                updateButtonSelection(weekBtn);
                salesViewModel.loadThisWeekSales();
                updateAfterDataLoad();
            });
        }
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                updateButtonSelection(monthBtn);
                salesViewModel.loadThisMonthSales();
                updateAfterDataLoad();
            });
        }
        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                updateButtonSelection(searchButton);
                salesViewModel.loadSalesByDateRange();
                updateAfterDataLoad();
                if (callback != null && startDatePicker != null && endDatePicker != null) {
                    callback.onSearch(startDatePicker.getValue(), endDatePicker.getValue());
                }
            });
        }
        if (printButton != null) printButton.setOnAction(e -> handlePrint());
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> {
                salesViewModel.refresh();
                updateAfterDataLoad();
            });
        }
        if (closeButton != null) closeButton.setOnAction(e -> handleCancel());
        if (previewPrintButton != null) previewPrintButton.setOnAction(e -> handlePreviewPrint());
    }
    
    /**
     * 데이터 로드 후 UI 업데이트를 처리하는 통합 메서드
     */
    private void updateAfterDataLoad() {
        Platform.runLater(() -> {
            // 지연 후 Summary 값들 강제 새로고침
            new Thread(() -> {
                try {
                    Thread.sleep(300); // 데이터 로드 완료 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    int size = salesViewModel.getSalesList().size();
                    updateStatusMessage(size == 0 ? "No receipts found for selected date range" : "Ready - " + size + " receipts found");
                    if (receiptItemsTableView != null) receiptItemsTableView.refresh();
                    
                    // Summary 값들 강제 새로고침
                    refreshSummaryValues();
                    
                    selectFirstReceiptAfterLoad();
                });
            }).start();
        });
    }
    
    private void selectFirstReceiptAfterLoad() {
        Platform.runLater(() -> {
            // 약간의 지연을 주어 데이터가 완전히 로드되도록 함
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    int size = salesViewModel.getSalesList().size();
                    if (size > 0 && receiptItemsTableView != null) {
                        SaleModel firstSale = salesViewModel.getSalesList().get(0);
                        receiptItemsTableView.getSelectionModel().select(firstSale);
                        salesViewModel.selectedSaleProperty().set(firstSale);
                        salesViewModel.onSelectedSaleChanged(firstSale);
                        updateReceiptPreview(firstSale);
                    }
                });
            }).start();
        });
    }
    
    private void updateStatusMessage(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
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
                            
                            // 현재 선택된 영수증의 아이템을 저장하고 프리뷰 업데이트
                            currentReceiptItems = salesViewModel.getSaleItemsList();
                            updateReceiptPreview(newVal);
                        }
                    }
                });
        }
        
        salesViewModel.selectedSaleProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null && receiptItemsTableView != null) {
                    var selectionModel = receiptItemsTableView.getSelectionModel();
                    if (selectionModel.getSelectedItem() != newVal) {
                        selectionModel.select(newVal);
                        receiptItemsTableView.scrollTo(newVal);
                    }
                    // ViewModel의 selectedSale이 변경될 때도 아이템 업데이트
                    currentReceiptItems = salesViewModel.getSaleItemsList();
                    updateReceiptPreview(newVal);
                }
            });
        });
    }

    /**
     * saleItemsList 변경 감지
     *
     * 핵심:
     * 상세 품목 데이터가 실제로 로딩 완료된 이후에만
     * preview 를 갱신한다.
     */
    private void setupSaleItemsListener() {

        salesViewModel.getSaleItemsList().addListener(
                (ListChangeListener<SaleItemModel>) change -> {

                    SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();

                    if (selectedSale == null) {
                        return;
                    }

                    Platform.runLater(() -> {

                        List<SaleItemModel> snapshot = List.copyOf(salesViewModel.getSaleItemsList());

                        log.info(
                                "[Preview Refresh] receipt={}, items={}",
                                selectedSale.getReceiptNo(),
                                snapshot.size()
                        );

                        updateReceiptPreview(selectedSale, snapshot);
                  
                    });
                });
    }

    /**
     * 실제 preview 갱신 (아이템 리스트 파라미터 버전)
     */
    private void updateReceiptPreview(SaleModel sale, List<SaleItemModel> items) {
        if (sale == null) return;

        log.info(
                "[updateReceiptPreview] receipt={}, itemCount={}",
                sale.getReceiptNo(),
                items != null ? items.size() : 0
        );

        updatePreviewLabels(sale);
        
        if (receiptWebView != null) {
            String html = generateReceiptHTML(sale, items);
            receiptWebView.getEngine().loadContent(html);
        }
    }

    /**
     * 영수증 프리뷰를 HTML 형식으로 WebView에 표시
     * 현재 선택된 영수증의 아이템을 직접 사용
     */
    private void updateReceiptPreview(SaleModel sale) {
        if (sale == null) return;
        
        log.info("[PrintReceiptDialog] updateReceiptPreview for receipt: {}", sale.getReceiptNo());
        
        // 현재 선택된 영수증의 아이템 가져오기
        List<SaleItemModel> items = salesViewModel.getSaleItemsList();
        
        // 디버그 로그
        if (items != null) {
            log.info("[PrintReceiptDialog] Current items count: {}", items.size());
            for (SaleItemModel item : items) {
                log.info("[PrintReceiptDialog] Item: {}, Qty: {}, Amount: {}", 
                    item.getDescription(), item.getQuantity(), item.getSaleAmount());
            }
        } else {
            log.warn("[PrintReceiptDialog] No items found for receipt: {}", sale.getReceiptNo());
        }
        
        updatePreviewLabels(sale);
        
        // WebView에 HTML 영수증 표시 (현재 선택된 아이템 전달)
        if (receiptWebView != null) {
            String htmlReceipt = generateReceiptHTML(sale, items);
            receiptWebView.getEngine().loadContent(htmlReceipt);
            log.info("[PrintReceiptDialog] Receipt preview loaded for: {}", sale.getReceiptNo());
        }
    }
    
    /**
     * 프리뷰 라벨 업데이트 (코드 중복 제거)
     */
    private void updatePreviewLabels(SaleModel sale) {
        if (previewReceiptNo != null) {
            previewReceiptNo.setText("Receipt #: " + sale.getReceiptNo());
        }

        if (previewDate != null) {
            String date = sale.getPaymentDateTime() != null
                    ? sale.getPaymentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "";
            previewDate.setText("Date : " + date);
        }

        if (previewTotalLabel != null) {
            double total = sale.getSaleAmount() != null ? sale.getSaleAmount().doubleValue() : 0;
            previewTotalLabel.setText(String.format("$%.2f", total));
        }
    }
    
    /**
     * SaleModel 데이터로 HTML 영수증 생성 (아이템 리스트를 파라미터로 받음)
     * @param sale 영수증 정보
     * @param items 해당 영수증의 아이템 리스트
     */
    private String generateReceiptHTML(SaleModel sale, List<SaleItemModel> items) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta charset='UTF-8'>\n");
        sb.append("<style>\n");
        sb.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        sb.append("body {\n");
        sb.append("  font-family: 'Courier New', 'Monaco', monospace;\n");
        sb.append("  font-size: 11px;\n");
        sb.append("  width: 280px;\n");
        sb.append("  margin: 0 auto;\n");
        sb.append("  padding: 8px;\n");
        sb.append("  background: white;\n");
        sb.append("}\n");
        sb.append(".header { text-align: center; margin-bottom: 8px; }\n");
        sb.append(".shop-name { font-size: 14px; font-weight: bold; margin-bottom: 3px; }\n");
        sb.append(".shop-info { font-size: 9px; color: #555; }\n");
        sb.append(".divider-dash { border-top: 1px dashed #000; margin: 5px 0; }\n");
        sb.append(".divider-solid { border-top: 1px solid #000; margin: 5px 0; }\n");
        sb.append(".receipt-info { display: flex; justify-content: space-between; margin: 3px 0; font-size: 10px; }\n");
        sb.append(".items-header { display: flex; justify-content: space-between; margin: 5px 0 2px 0; font-weight: bold; font-size: 10px; }\n");
        sb.append(".item-row { display: flex; justify-content: space-between; margin: 2px 0; }\n");
        sb.append(".item-desc { flex: 2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n");
        sb.append(".item-qty { width: 35px; text-align: right; }\n");
        sb.append(".item-price { width: 55px; text-align: right; }\n");
        sb.append(".item-amount { width: 55px; text-align: right; }\n");
        sb.append(".discount-row { font-size: 9px; color: #e74c3c; display: flex; justify-content: flex-end; margin: 1px 0; }\n");
        sb.append(".total-row { display: flex; justify-content: space-between; margin: 5px 0; font-weight: bold; font-size: 12px; }\n");
        sb.append(".summary-row { display: flex; justify-content: space-between; margin: 2px 0; font-size: 10px; }\n");
        sb.append(".footer { text-align: center; margin-top: 8px; }\n");
        sb.append(".barcode { text-align: center; margin: 8px 0 5px 0; font-size: 14px; letter-spacing: 2px; font-family: 'Courier New', monospace; }\n");
        sb.append(".notice { text-align: center; font-size: 9px; margin-top: 8px; }\n");
        sb.append(".thankyou { font-size: 10px; font-weight: bold; margin-top: 5px; }\n");
        sb.append(".empty-message { text-align: center; color: #999; padding: 20px; }\n");
        sb.append("</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        
        // ========== Header ==========
        sb.append("<div class='header'>\n");
        sb.append("<div class='shop-name'>MY STORE</div>\n");
        sb.append("<div class='shop-info'>123 Main Street, Suite 100</div>\n");
        sb.append("<div class='shop-info'>Tel: (123) 456-7890</div>\n");
        sb.append("<div class='shop-info'>GST: 1234567890</div>\n");
        sb.append("<div class='divider-solid'></div>\n");
        
        // 영수증 정보
        String receiptNo = sale.getReceiptNo() != null ? sale.getReceiptNo() : "N/A";
        String date = sale.getPaymentDateTime() != null ? 
            sale.getPaymentDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : 
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        
        sb.append("<div class='receipt-info'><span>Date:</span><span>").append(escapeHtml(date)).append("</span></div>\n");
        sb.append("<div class='receipt-info'><span>Receipt No:</span><span>").append(escapeHtml(receiptNo)).append("</span></div>\n");
        sb.append("<div class='divider-solid'></div>\n");
        sb.append("</div>\n");
        
        // ========== Items Header ==========
        sb.append("<div class='items-header'>\n");
        sb.append("<span>Item</span>\n");
        sb.append("<span style='width:35px;text-align:right'>Qty</span>\n");
        sb.append("<span style='width:55px;text-align:right'>Price</span>\n");
        sb.append("<span style='width:55px;text-align:right'>Amount</span>\n");
        sb.append("</div>\n");
        sb.append("<div class='divider-dash'></div>\n");
        
        // ========== Items (파라미터로 받은 items 사용) ==========
        double subtotal = 0;
        
        if (items != null && !items.isEmpty()) {
            for (SaleItemModel item : items) {
                int qty = item.getQuantity();
                double price = item.getSalePrice() != null ? item.getSalePrice().doubleValue() : 0;
                double amount = item.getSaleAmount() != null ? item.getSaleAmount().doubleValue() : 0;
                double discount = item.getDiscountAmount() != null ? item.getDiscountAmount().doubleValue() : 0;
                double finalAmount = amount;
                
                subtotal += amount;
                
                String desc = item.getDescription() != null ? item.getDescription() : (item.getBarcode() != null ? item.getBarcode() : "Item");
                if (desc.length() > 22) desc = desc.substring(0, 19) + "...";
                
                sb.append("<div class='item-row'>\n");
                sb.append("<span class='item-desc'>").append(escapeHtml(desc)).append("</span>\n");
                sb.append("<span class='item-qty'>").append(qty).append("</span>\n");
                sb.append("<span class='item-price'>").append(formatMoney(price)).append("</span>\n");
                sb.append("<span class='item-amount'>").append(formatMoney(finalAmount)).append("</span>\n");
                sb.append("</div>\n");
                
                // 할인 정보
                if (discount > 0) {
                    sb.append("<div class='discount-row'>\n");
                    sb.append("<span>  Discount</span>\n");
                    sb.append("<span>-").append(formatMoney(discount)).append("</span>\n");
                    sb.append("</div>\n");
                }
            }
        } else {
            sb.append("<div class='empty-message'>No items found for this receipt</div>\n");
        }
        
        sb.append("<div class='divider-dash'></div>\n");
        
        // ========== Totals ==========
        double totalDiscount = sale.getDiscountAmount() != null ? sale.getDiscountAmount().doubleValue() : 0;
        double finalTotal = sale.getSaleAmount() != null ? sale.getSaleAmount().doubleValue() : 0;
        
        if (totalDiscount > 0) {
            sb.append("<div class='summary-row'>\n");
            sb.append("<span>ORIGINAL AMOUNT</span>\n");
            sb.append("<span>").append(formatMoney(subtotal)).append("</span>\n");
            sb.append("</div>\n");
            sb.append("<div class='summary-row'>\n");
            sb.append("<span>DISCOUNT</span>\n");
            sb.append("<span>-").append(formatMoney(totalDiscount)).append("</span>\n");
            sb.append("</div>\n");
            sb.append("<div class='divider-dash'></div>\n");
            sb.append("<div class='total-row'>\n");
            sb.append("<span>TOTAL AMOUNT</span>\n");
            sb.append("<span>").append(formatMoney(finalTotal)).append("</span>\n");
            sb.append("</div>\n");
        } else {
            sb.append("<div class='total-row'>\n");
            sb.append("<span>TOTAL AMOUNT</span>\n");
            sb.append("<span>").append(formatMoney(finalTotal)).append("</span>\n");
            sb.append("</div>\n");
        }
        
        sb.append("<div class='divider-dash'></div>\n");
        
        // ========== Payment Info ==========
        double cashAmount = sale.getCashAmount() != null ? sale.getCashAmount().doubleValue() : 0;
        double creditAmount = sale.getCreditAmount() != null ? sale.getCreditAmount().doubleValue() : 0;
        double cashoutAmount = sale.getCashoutAmount() != null ? sale.getCashoutAmount().doubleValue() : 0;
        
        if (cashAmount > 0) {
            sb.append("<div class='summary-row'>\n");
            sb.append("<span>CASH PAID</span>\n");
            sb.append("<span>").append(formatMoney(cashAmount)).append("</span>\n");
            sb.append("</div>\n");
        }
        
        if (creditAmount > 0) {
            sb.append("<div class='summary-row'>\n");
            sb.append("<span>CARD PAID</span>\n");
            sb.append("<span>").append(formatMoney(creditAmount)).append("</span>\n");
            sb.append("</div>\n");
        }
        
        if (cashoutAmount > 0) {
            sb.append("<div class='summary-row'>\n");
            sb.append("<span>CASHOUT</span>\n");
            sb.append("<span>").append(formatMoney(cashoutAmount)).append("</span>\n");
            sb.append("</div>\n");
        }
        
        sb.append("<div class='divider-solid'></div>\n");
        
        // ========== Barcode ==========
        sb.append("<div class='barcode'>\n");
        sb.append("* ").append(escapeHtml(receiptNo)).append(" *\n");
        sb.append("</div>\n");
        
        // ========== Footer ==========
        sb.append("<div class='footer'>\n");
        sb.append("<div class='notice'>** Tax Invoice **</div>\n");
        sb.append("<div class='divider-dash'></div>\n");
        sb.append("<div class='thankyou'>Thank you for your visit!</div>\n");
        sb.append("<div class='notice'>Goods sold are not refundable</div>\n");
        sb.append("<div class='notice'>For exchange, please bring receipt</div>\n");
        sb.append("</div>\n");
        
        sb.append("</body>\n");
        sb.append("</html>\n");
        
        return sb.toString();
    }
    
    private String formatMoney(double amount) {
        return String.format("$%,.2f", amount);
    }
    
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private void updateButtonSelection(Button selectedBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                String selectedStyle = "-fx-background-color: #3498db; -fx-text-fill: white;";
                String defaultStyle = "";
                btn.setStyle(btn == selectedBtn ? selectedStyle : defaultStyle);
            }
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
    
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        return currencyFormat.format(amount);
    }

    public void initData(PrintReceiptCallback callback) {
        this.callback = callback;
    }

    @FXML
    private void handlePrint() {
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            updateStatusMessage("Please select a receipt to print");
            return;
        }
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Printing receipt: {}", receiptNo);
        
        if (callback != null) callback.onPrint(receiptNo);
        updateStatusMessage("Printing receipt: " + receiptNo);
    }

    @FXML
    private void handlePreviewPrint() {
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            updateStatusMessage("Please select a receipt to preview");
            return;
        }
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Previewing receipt: {}", receiptNo);
        
        if (callback != null) callback.onPreview(receiptNo);
    }

    @Override
    protected void handleConfirm() {
        if (searchButton != null) searchButton.fire();
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

    public void setReceiptNumber(String receiptNo) {
        if (receiptNoLabel != null) receiptNoLabel.setText(receiptNo);
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