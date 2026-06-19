package com.swna.javafx.pos.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.viewmodel.ShopViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.pos.dto.request.DiscountType;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleItemRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.SaleResponse;
import com.swna.javafx.pos.print.SaleRequestConverter;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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
public class PrintReceiptDialogController extends BasePosDialog implements Initializable {

    private final SalesViewModel salesViewModel;
    private final ShopViewModel shopViewModel;

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
    @SuppressWarnings("unused")
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
 setupSalesListListener();
        enableFullWindowDrag();
        // Scene에 키 이벤트 추가 (가장 간단함)
        Platform.runLater(() -> {
            if (receiptNoLabel != null && receiptNoLabel.getScene() != null) {
                receiptNoLabel.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        log.info("[PrintReceiptDialog] ESC key pressed - closing dialog");
                        handleCancel();
                        event.consume();
                    }
                });
            }
        });

        loadInitialData();
        shopViewModel.loadInitialData();
        
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
    @SuppressWarnings("unused")
    private void updateSummaryDate() {
        if (summaryDateLabel != null) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            summaryDateLabel.setText(todayStr);
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
            receiptItemsTableView.setItems(salesViewModel.getFilteredSalesList());
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
              //  updateAfterDataLoad();
            });
        }
        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                updateButtonSelection(weekBtn);
                salesViewModel.loadThisWeekSales();
               // updateAfterDataLoad();
            });
        }
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                updateButtonSelection(monthBtn);
                salesViewModel.loadThisMonthSales();
               // updateAfterDataLoad();
            });
        }
        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                updateButtonSelection(searchButton);
                salesViewModel.loadSalesByDateRange();
               // updateAfterDataLoad();
                if (callback != null && startDatePicker != null && endDatePicker != null) {
                    callback.onSearch(startDatePicker.getValue(), endDatePicker.getValue());
                }
            });
        }
        if (printButton != null) printButton.setOnAction(e -> handlePrint());
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> {
                salesViewModel.refresh();
                //updateAfterDataLoad();
            });
        }
        if (closeButton != null) closeButton.setOnAction(e -> handleCancel());
        if (previewPrintButton != null) previewPrintButton.setOnAction(e -> handlePreviewPrint());
    }
    
    /**
     * 데이터 로드 후 UI 업데이트를 처리하는 통합 메서드
     */

     private void setupSalesListListener() {

        salesViewModel.getSalesList()
                .addListener((ListChangeListener<SaleModel>) change ->
                        Platform.runLater(this::refreshSalesView));
    }

    private void refreshSalesView() {

        int size = salesViewModel.getSalesList().size();

        updateStatusMessage(
                size == 0
                        ? "No receipts found for selected date range"
                        : "Ready - " + size + " receipts found");

        receiptItemsTableView.refresh();

        if (!salesViewModel.getSalesList().isEmpty()) {

            SaleModel firstSale = salesViewModel.getSalesList().getFirst();

            receiptItemsTableView.getSelectionModel().select(firstSale);
        }
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
    private void updateReceiptPreview(SaleModel saleModel) {
        if (saleModel == null) return;
        
        log.info("[PrintReceiptDialog] updateReceiptPreview for receipt: {}", saleModel.getReceiptNo());
        
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
            log.warn("[PrintReceiptDialog] No items found for receipt: {}", saleModel.getReceiptNo());
        }
        
        updatePreviewLabels(saleModel);
        
        // WebView에 HTML 영수증 표시 (현재 선택된 아이템 전달)
        if (receiptWebView != null) {
            String htmlReceipt = generateReceiptHTML(saleModel, items);
            receiptWebView.getEngine().loadContent(htmlReceipt);
            log.info("[PrintReceiptDialog] Receipt preview loaded for: {}", saleModel.getReceiptNo());
        }
    }
    
    /**
     * 프리뷰 라벨 업데이트 (코드 중복 제거)
     */
    private void updatePreviewLabels(SaleModel saleModel) {
        if (previewReceiptNo != null) {
            previewReceiptNo.setText("Receipt #: " + saleModel.getReceiptNo());
        }

        if (previewDate != null) {
            String date = saleModel.getPaymentDateTime() != null
                    ? saleModel.getPaymentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "";
            previewDate.setText("Date : " + date);
        }

        if (previewTotalLabel != null) {
            double total = saleModel.getSaleAmount() != null ? saleModel.getSaleAmount().doubleValue() : 0;
            previewTotalLabel.setText(String.format("$%.2f", total));
        }
    }
    
    /**
     * SaleModel 데이터로 HTML 영수증 생성 (아이템 리스트를 파라미터로 받음)
     * @param sale 영수증 정보
     * @param items 해당 영수증의 아이템 리스트
     */
/**
     * ReceiptFormatter와 동일한 로직(헤더, 가격, 결제 정보)을 따르는 HTML 생성기
     */
    private String generateReceiptHTML(SaleModel saleModel, List<SaleItemModel> items) {
        StringBuilder sb = new StringBuilder();
        Shop shop = shopViewModel.getCachedShop();

        // 1. Shop 데이터 처리 (Null 체크 및 기본값 설정)
        String shopName = (shop != null && shop.getCompany() != null) ? shop.getCompany().toUpperCase() : "MY STORE";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "123 Main Street, Suite 100";
        String phone = (shop != null && shop.getPhone() != null) ? shop.getPhone() : "(123) 456-7890";
        String businessNo = (shop != null && shop.getBusinessNo() != null) ? shop.getBusinessNo() : "1234567890";

        // 1. 스타일 설정 (ReceiptFormatter의 레이아웃 유지)
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n<style>\n");
        sb.append("body { font-family: 'Courier New', monospace; font-size: 11px; width: 280px; margin: 0 auto; padding: 5px; }\n");
        sb.append(".header { text-align: left; margin-bottom: 5px; }\n");
        sb.append(".shop-name { font-size: 16px; font-weight: bold; }\n");
        sb.append(".divider { border-top: 1px solid #000; margin: 4px 0; }\n");
        sb.append(".row { display: flex; justify-content: space-between; margin: 1px 0; }\n");
        sb.append(".item-row { display: flex; justify-content: space-between; margin: 2px 0; font-size: 10px; }\n");
        sb.append(".items-header { display: flex; justify-content: space-between; font-weight: bold; font-size: 10px; border-bottom: 1px dashed #000; padding: 2px 0; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        // 2. Header (ReceiptFormatter.buildHeader)
        sb.append("<div class='header'>\n");
        sb.append("<div class='shop-name'>").append(escapeHtml(shopName)).append("</div>\n");
        sb.append("<div>").append(escapeHtml(shopAddress)).append("</div>\n");
        sb.append("<div>Tel: ").append(escapeHtml(phone)).append("</div>\n");
        sb.append("<div>GST: ").append(escapeHtml(businessNo)).append("</div>\n");
        sb.append("</div>\n<div class='divider'></div>\n");

        String date = saleModel.getPaymentDateTime() != null ? saleModel.getPaymentDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        sb.append("<div class='row'><span>Date:</span><span>").append(escapeHtml(date)).append("</span></div>\n");
        sb.append("<div class='row'><span>Receipt No:</span><span>").append(escapeHtml(saleModel.getReceiptNo())).append("</span></div>\n");
        sb.append("<div class='divider'></div>\n");

        // 3. Body (ReceiptFormatter.buildBody)
        sb.append("<div class='items-header'><span>Item</span><span>Price</span><span>Qty</span><span>D/C</span><span>Amount</span></div>\n");
        
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                SaleItemModel item = items.get(i);
                double price = item.getSalePrice() != null ? item.getSalePrice().doubleValue() : 0;
                double discount = item.getDiscountAmount() != null ? item.getDiscountAmount().doubleValue() : 0;
                double amount = item.getSaleAmount() != null ? item.getSaleAmount().doubleValue() : 0;

                sb.append("<div style='margin-top:4px'>").append((i + 1)).append(". ").append(escapeHtml(item.getDescription())).append("</div>\n");
                sb.append("<div class='item-row'>\n");
                sb.append("<span></span><span>").append(formatCurrency(price)).append("</span>");
                sb.append("<span>").append(item.getQuantity()).append("</span>");
                sb.append("<span>").append(discount > 0 ? formatCurrency(discount) : "-").append("</span>");
                sb.append("<span>").append(formatCurrency(amount)).append("</span>\n");
                sb.append("</div>\n");
            }
        }
        sb.append("<div class='divider'></div>\n");

        //===========================================================================
        // 4. Footer & Payment (ReceiptFormatter.buildFooter & buildPaymentInfo)
        //===========================================================================
        int width = 40;
        String rowFormat = "%20s %-" + (width - 21) + "s";
        String NL = "\n";
        log.info("salseRequest {} ", saleModel);
        BigDecimal cashAmount = saleModel.getCashAmount();
        BigDecimal cashoutAmount = saleModel.getCashoutAmount();
        BigDecimal creditAmount = saleModel.getCreditAmount();
        BigDecimal discountAmount = saleModel.getDiscountAmount();
        BigDecimal saleAmount = saleModel.getSaleAmount();
        BigDecimal receivedAmount = saleModel.getReceivedAmount();
        BigDecimal changeAmount = saleModel.getChangeAmount();

        String paymentType = saleModel.getPaymentType();
      
      
        BigDecimal balance = changeAmount;
        BigDecimal gst = saleAmount.subtract(saleAmount.divide(BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP));

        // 3. HTML 출력
        sb.append("<div style='font-family: monospace; white-space: pre; font-size: 12px;'>");

        // Final Amount 출력
        String finalAmountStr = String.format("%s (GST: %s)", 
                formatCurrency(saleAmount.doubleValue()), formatCurrency(gst.doubleValue()));
        sb.append(String.format(rowFormat, "Final Amount : ", finalAmountStr)).append(NL);
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format(rowFormat, "D/C : ", formatCurrency(discountAmount))).append(NL);
        }

 
        if ("CASH".equalsIgnoreCase(paymentType)) {
            sb.append(String.format(rowFormat, "Cash Paid : ", formatCurrency(receivedAmount))).append(NL);
            if (balance.doubleValue() > 0) {
                sb.append(String.format(rowFormat, "Balance : ", formatCurrency(balance.doubleValue()))).append(NL);
            }
        }
        if (paymentType.contains("CASHOUT")) {
            sb.append(String.format(rowFormat, "EFT : ", formatCurrency(creditAmount))).append(NL);
            if (cashoutAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format(rowFormat, "Cash Out : ", formatCurrency(cashoutAmount))).append(NL);
            }
        }
        if (paymentType.contains("CARD")) {
            sb.append(String.format(rowFormat, "EFT : ", formatCurrency(receivedAmount))).append(NL);
            if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format(rowFormat, "Cash Paid : ", formatCurrency(cashAmount))).append(NL);
            }
        }
       
   
        sb.append("</div>\n");
        sb.append("<div class='divider'></div>\n");
        
        sb.append("<div style='text-align:center; font-size:9px;'>Goods sold are not refundable</div>\n");
        sb.append("<div style='text-align:center; font-size:9px;'>For exchange, please bring receipt</div>\n");
        sb.append("<div class='divider'></div>\n");
        sb.append("<div style='text-align:center; font-size:9px;'>Thank you for your visit!</div>\n");
        return sb.toString();
    }
    
    private String formatCurrency(double amount) {
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
        // 1. 현재 선택된 영수증 건이 있는지 검증
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            updateStatusMessage("Please select a receipt to print");
            return;
        }
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Direct WebView Printing Started for receipt: {}", receiptNo);

        // 2. 프리뷰용 WebView 컴포넌트와 그 내부의 WebEngine이 유효한지 확인
        if (receiptWebView != null && receiptWebView.getEngine() != null) {
            
            // 3. 시스템 기본 프린터로 인쇄 작업을 생성
            PrinterJob job = PrinterJob.createPrinterJob();
            
            if (job != null) {
                updateStatusMessage("Sending to printer... : " + receiptNo);
                
                /* 
                * [선택사항] 만약 OS 인쇄 대화상자(설정창)를 띄우고 싶다면 아래 2줄 주석을 해제하세요.
                * boolean proceed = job.showPrintDialog(receiptWebView.getScene().getWindow());
                * if (!proceed) return; 
                */

                // 4. WebView에 렌더링된 HTML 내용을 프린터 출력 스트림으로 직접 전송
                receiptWebView.getEngine().print(job);
                
                // 5. 인쇄 전송 완료 및 성공 여부 확인
                boolean success = job.endJob();
                if (success) {
                    updateStatusMessage("Print completed successfully: " + receiptNo);
                } else {
                    updateStatusMessage("Print failed or canceled: " + receiptNo);
                }
            } else {
                updateStatusMessage("Error: No default printer setup found on this PC.");
            }
        } else {
            updateStatusMessage("Error: Receipt preview component is not ready.");
        }

        // (선택) 기존에 설정되어 있던 콜백도 유지하고 싶다면 그대로 두셔도 무방합니다.
        if (callback != null) callback.onPrint(receiptNo);
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