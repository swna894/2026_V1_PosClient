package com.swna.javafx.pos.dialog.print_receipt_dialog;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.viewmodel.ShopViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.pos.dialog.BasePosDialog;
import com.swna.javafx.pos.event.ReceiptPrintEvent;

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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * 영수증 출력 다이얼로그 컨트롤러
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>날짜 범위별 영수증 조회 (오늘/이번주/이번달/사용자 지정)</li>
 *   <li>선택한 영수증의 상세 내역 조회</li>
 *   <li>영수증 HTML 프리뷰 (WebView)</li>
 *   <li>영수증 출력 (Print)</li>
 * </ul>
 */
@Slf4j
@Component
@Scope("prototype")
@FxmlView("/view/pos/dialog/PrintReceiptDialog.fxml")
public class PrintReceiptDialogController extends BasePosDialog implements Initializable {

    private static final String DEFAULT_CURRENCY_ZERO = "$0.00";
    // =========================================================================
    // Dependencies (의존성 주입)
    // =========================================================================
    private final ApplicationEventPublisher eventPublisher;
    private final SalesViewModel salesViewModel;
    private final ShopViewModel shopViewModel;

    
    /** 영수증 HTML 생성 헬퍼 */
    private final ReceiptHtmlGenerator htmlGenerator;


    public PrintReceiptDialogController(ApplicationEventPublisher eventPublisher, 
                        ReceiptHtmlGenerator htmlGenerator, 
                        SalesViewModel salesViewModel, 
                        ShopViewModel shopViewModel) {
        this.eventPublisher = eventPublisher;
        this.salesViewModel = salesViewModel;
        this.shopViewModel = shopViewModel;
        this.htmlGenerator = htmlGenerator;   
    }

    // =========================================================================
    // FXML UI Components (ToolBar)
    // =========================================================================
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

    // =========================================================================
    // FXML UI Components (Summary Labels)
    // =========================================================================
    @FXML private Label summaryDateLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Label summaryDiscountLabel;
    @FXML private Label summaryCashLabel;
    @FXML private Label summaryCreditLabel;
    @FXML private Label summaryCashoutLabel;

    // =========================================================================
    // FXML UI Components (Receipt Info)
    // =========================================================================
    @FXML private Label receiptNoLabel;
    @FXML private Label previewReceiptNo;
    @FXML private Label previewDate;
    @FXML private Label previewTotalLabel;

    // =========================================================================
    // FXML UI Components (Status & Count)
    // =========================================================================
    @FXML private Label totalCountLabel;
    @FXML private Label lblStatus;

    // =========================================================================
    // FXML UI Components (TableViews)
    // =========================================================================
    @FXML private TableView<SaleModel> receiptItemsTableView;
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private VBox previewArea;

    // =========================================================================
    // FXML UI Components (WebView)
    // =========================================================================
    @FXML private WebView receiptWebView;

    // =========================================================================
    // FXML UI Components (Receipt Table Columns)
    // =========================================================================
    @FXML private TableColumn<SaleModel, String> noColumn;
    @FXML private TableColumn<SaleModel, String> printIconColumn;
    @FXML private TableColumn<SaleModel, String> receiptColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> amountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> discountColumn;

    // =========================================================================
    // FXML UI Components (Sale Items Table Columns)
    // =========================================================================
    @FXML private TableColumn<SaleItemModel, String> itemNoColumn;
    @FXML private TableColumn<SaleItemModel, String> itemBarcodeColumn;
    @FXML private TableColumn<SaleItemModel, String> itemDescriptionColumn;
    @FXML private TableColumn<SaleItemModel, Integer> itemQtyColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemAmountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> itemDiscountColumn;

    // =========================================================================
    // Fields (인스턴스 변수)
    // =========================================================================
    private List<SaleItemModel> saleItems;
    private PrintReceiptCallback callback;
    private Button[] navButtons;

    // =========================================================================
    // Initializable Implementation (초기화)
    // =========================================================================
    
    /**
     * 컨트롤러 초기화 메서드
     * JavaFX 애플리케이션 시작 시 자동 호출됨
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        log.info("[PrintReceiptDialog] initialize() started");
        
        initializeNavButtons();
        setupBindings();
        setupDatePickers();
        setupTableColumns();
        setupTableBindings();
        setupButtonActions();
        setupSelectionListeners();
        setupKeyboardShortcuts();
        
        loadInitialData();
        shopViewModel.loadInitialData();
        
        log.info("[PrintReceiptDialog] initialize() completed");
    }

    /**
     * 네비게이션 버튼 배열 초기화
     */
    private void initializeNavButtons() {
        this.navButtons = new Button[] { todayBtn, weekBtn, monthBtn, searchButton };
    }

    // =========================================================================
    // Data Loading (데이터 로드)
    // =========================================================================
    
    /**
     * 초기 데이터 로드
     * 오늘 날짜 기준으로 영수증 목록을 조회
     */
    private void loadInitialData() {
        log.info("[PrintReceiptDialog] Loading initial data...");
        
        LocalDate today = LocalDate.now();
        setDatePickerValues(today, today);
        salesViewModel.loadTodaySales();
        
        Platform.runLater(() -> {
            int size = salesViewModel.getSalesList().size();
            String status = size == 0 
                ? "No sales data found for today: " + today 
                : "Ready - " + size + " receipts found";
            updateStatusMessage(status);
            refreshTableView(receiptItemsTableView);
        });
    }

    // =========================================================================
    // Bindings Setup (바인딩 설정)
    // =========================================================================
    
    /**
     * 모든 바인딩 설정
     * - DatePicker ↔ ViewModel 양방향 바인딩
     * - 요약 정보 라벨 바인딩
     */
    private void setupBindings() {
        log.info("[PrintReceiptDialog] Setting up bindings...");
        bindDatePickers();
        bindStatusMessage();
        bindReceiptNoLabel();
        bindSummaryLabels();
    }

    /** DatePicker ↔ ViewModel 양방향 바인딩 */
    private void bindDatePickers() {
        if (startDatePicker != null) {
            startDatePicker.valueProperty().bindBidirectional(salesViewModel.startDateProperty());
        }
        if (endDatePicker != null) {
            endDatePicker.valueProperty().bindBidirectional(salesViewModel.endDateProperty());
        }
    }

    /** 상태 메시지 바인딩 */
    private void bindStatusMessage() {
        if (lblStatus != null) {
            salesViewModel.errorMessageProperty().addListener((obs, old, msg) -> 
                Platform.runLater(() -> updateStatusMessage(msg != null ? msg : "Ready"))
            );
        }
    }

    /** 영수증 번호 라벨 바인딩 */
    private void bindReceiptNoLabel() {
        if (receiptNoLabel != null) {
            receiptNoLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        return (selected != null && selected.getReceiptNo() != null) 
                            ? selected.getReceiptNo() 
                            : "No receipt selected";
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
    }

    /**
     * 요약 정보 라벨 바인딩
     * 선택된 영수증의 정보를 실시간으로 표시
     */
    private void bindSummaryLabels() {
        bindSummaryDateLabel();
        bindSummaryTotalLabel();
        bindSummaryDiscountLabel();
        bindSummaryCashLabel();
        bindSummaryCreditLabel();
        bindSummaryCashoutLabel();
        bindTotalCountLabel();
    }

    private void bindSummaryDateLabel() {
        if (summaryDateLabel != null) {
            summaryDateLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        if (selected != null && selected.getPaymentDateTime() != null) {
                            return selected.getPaymentDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }
                        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
    }

    private void bindSummaryTotalLabel() {
        if (summaryTotalLabel != null) {
            summaryTotalLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.itemTotalAmountProperty().get()),
                    salesViewModel.itemTotalAmountProperty()
                )
            );
        }
    }

    private void bindSummaryDiscountLabel() {
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(salesViewModel.itemTotalDiscountProperty().get()),
                    salesViewModel.itemTotalDiscountProperty()
                )
            );
        }
    }

    private void bindSummaryCashLabel() {
        if (summaryCashLabel != null) {
            summaryCashLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        return selected != null ? formatCurrency(selected.getCashAmount()) : DEFAULT_CURRENCY_ZERO;
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
    }

    private void bindSummaryCreditLabel() {
        if (summaryCreditLabel != null) {
            summaryCreditLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        return selected != null ? formatCurrency(selected.getCreditAmount()) : DEFAULT_CURRENCY_ZERO;
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
    }

    private void bindSummaryCashoutLabel() {
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        SaleModel selected = salesViewModel.selectedSaleProperty().get();
                        return selected != null ? formatCurrency(selected.getCashoutAmount()) : DEFAULT_CURRENCY_ZERO;
                    },
                    salesViewModel.selectedSaleProperty()
                )
            );
        }
    }

    private void bindTotalCountLabel() {
        if (totalCountLabel != null) {
            totalCountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> String.format("Items: %d", salesViewModel.itemTotalQtyProperty().get()),
                    salesViewModel.itemTotalQtyProperty()
                )
            );
        }
    }

    // =========================================================================
    // DatePicker Setup (날짜 선택기 설정)
    // =========================================================================
    
    /** DatePicker 초기값 설정 */
    private void setupDatePickers() {
        setDatePickerValueIfNull(startDatePicker, LocalDate.now());
        setDatePickerValueIfNull(endDatePicker, LocalDate.now());
    }

    /** DatePicker 값 설정 (null인 경우에만) */
    private void setDatePickerValueIfNull(DatePicker picker, LocalDate value) {
        if (picker != null && picker.getValue() == null) {
            picker.setValue(value);
        }
    }

    /** 두 DatePicker 동시 설정 */
    private void setDatePickerValues(LocalDate start, LocalDate end) {
        if (startDatePicker != null) startDatePicker.setValue(start);
        if (endDatePicker != null) endDatePicker.setValue(end);
    }

    // =========================================================================
    // Table Setup (테이블 설정)
    // =========================================================================
    
    /**
     * 테이블 컬럼 설정
     * 각 컬럼의 데이터 타입과 정렬, 포맷을 지정
     */
    private void setupTableColumns() {
        log.info("[PrintReceiptDialog] setupTableColumns() started");
        setupReceiptTableColumns();
        setupSaleItemsTableColumns();
    }

    /** 영수증 목록 테이블 컬럼 설정 */
    private void setupReceiptTableColumns() {
        if (receiptItemsTableView == null) return;
        
        TableColumnUtil.createNumberColumn(receiptItemsTableView, noColumn, 50);
        TableColumnUtil.makeStringColumn(receiptColumn, SaleModel::receiptNoProperty, 
            null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(amountColumn, SaleModel::saleAmountProperty, 
            false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountColumn, SaleModel::discountAmountProperty, 
            false, true, TableColumnUtil.RIGHT, null);
    }

    /** 영수증 상세 품목 테이블 컬럼 설정 */
    private void setupSaleItemsTableColumns() {
        if (saleItemsTableView == null) return;
        
        TableColumnUtil.createNumberColumn(saleItemsTableView, itemNoColumn, 50);
        TableColumnUtil.makeStringColumn(itemBarcodeColumn, SaleItemModel::barcodeProperty, 
            null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeStringColumn(itemDescriptionColumn, SaleItemModel::descriptionProperty, 
            null, false, true, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeIntegerColumn(itemQtyColumn, SaleItemModel::quantityProperty, 
            null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(itemPriceColumn, SaleItemModel::salePriceProperty, 
            false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(itemAmountColumn, SaleItemModel::saleAmountProperty, 
            false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(itemDiscountColumn, SaleItemModel::discountAmountProperty, 
            false, true, TableColumnUtil.RIGHT, null);
    }

    /** TableView와 ViewModel 데이터 바인딩 */
    private void setupTableBindings() {
        if (receiptItemsTableView != null) {
            receiptItemsTableView.setItems(salesViewModel.getFilteredSalesList());
        }
        if (saleItemsTableView != null) {
            saleItemsTableView.setItems(salesViewModel.getSaleItemsList());
        }
    }

    // =========================================================================
    // Button Actions (버튼 액션 설정)
    // =========================================================================
    
    /** 모든 버튼 액션 설정 */
    private void setupButtonActions() {
        setupTodayButton();
        setupWeekButton();
        setupMonthButton();
        setupSearchButton();
        setupPrintButton();
        setupRefreshButton();
        setupCloseButton();
        setupPreviewPrintButton();
    }

    private void setupTodayButton() {
        if (todayBtn != null) {
            todayBtn.setOnAction(e -> {
                updateButtonSelection(todayBtn);
                salesViewModel.loadTodaySales();
            });
        }
    }

    private void setupWeekButton() {
        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                updateButtonSelection(weekBtn);
                salesViewModel.loadThisWeekSales();
            });
        }
    }

    private void setupMonthButton() {
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                updateButtonSelection(monthBtn);
                salesViewModel.loadThisMonthSales();
            });
        }
    }

    private void setupSearchButton() {
        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                updateButtonSelection(searchButton);
                salesViewModel.loadSalesByDateRange();
                notifySearchCallback();
            });
        }
    }

    private void setupPrintButton() {
        if (printButton != null) {
            printButton.setOnAction(e -> handlePrint());
        }
    }

    private void setupRefreshButton() {
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> salesViewModel.refresh());
        }
    }

    private void setupCloseButton() {
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleCancel());
        }
    }

    private void setupPreviewPrintButton() {
        if (previewPrintButton != null) {
            previewPrintButton.setOnAction(e -> handlePreviewPrint());
        }
    }

    /** 검색 콜백 호출 */
    private void notifySearchCallback() {
        if (callback != null && startDatePicker != null && endDatePicker != null) {
            callback.onSearch(startDatePicker.getValue(), endDatePicker.getValue());
        }
    }

    // =========================================================================
    // Selection Listeners (선택 리스너 설정)
    // =========================================================================
    
    /**
     * 영수증 선택 리스너 설정
     * 테이블에서 영수증 선택 시 상세 정보와 프리뷰 업데이트
     */
    private void setupSelectionListeners() {
        setupReceiptTableSelectionListener();
        setupSaleItemsListListener();
        setupSalesListListener();
        setupSelectedSalePropertyListener();
    }

    /** 영수증 테이블 선택 리스너 */
    private void setupReceiptTableSelectionListener() {
        if (receiptItemsTableView == null) return;
        
        receiptItemsTableView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    log.info("[PrintReceiptDialog] Receipt selected: {}", newVal.getReceiptNo());
                    
                    if (salesViewModel.selectedSaleProperty().get() != newVal) {
                        salesViewModel.selectedSaleProperty().set(newVal);
                        salesViewModel.onSelectedSaleChanged(newVal);
                        
                        // 선택된 영수증의 아이템 저장 및 프리뷰 업데이트
                        updateReceiptPreview(newVal);
                    }
                }
            });
    }

    /** SaleItemsList 변경 리스너 - 상세 품목 데이터 로드 완료 시 프리뷰 갱신 */
    private void setupSaleItemsListListener() {
        salesViewModel.getSaleItemsList().addListener(
            (ListChangeListener<SaleItemModel>) change -> {
                SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
                if (selectedSale == null) return;
                
                Platform.runLater(() -> {
                    List<SaleItemModel> snapshot = List.copyOf(salesViewModel.getSaleItemsList());
                    log.info("[Preview Refresh] receipt={}, items={}", 
                        selectedSale.getReceiptNo(), snapshot.size());
                    updateReceiptPreview(selectedSale, snapshot);
                });
            }
        );
    }

    /** SalesList 변경 리스너 - 데이터 로드 완료 시 첫 번째 항목 자동 선택 */
    private void setupSalesListListener() {
        salesViewModel.getSalesList().addListener(
            (ListChangeListener<SaleModel>) change -> 
                Platform.runLater(this::refreshSalesView)
        );
    }

    /** selectedSaleProperty 변경 리스너 - 외부에서 선택 변경 시 동기화 */
    private void setupSelectedSalePropertyListener() {
        salesViewModel.selectedSaleProperty().addListener((obs, oldVal, newVal) -> 
            Platform.runLater(() -> {
                if (newVal != null && receiptItemsTableView != null) {
                    var selectionModel = receiptItemsTableView.getSelectionModel();
                    if (selectionModel.getSelectedItem() != newVal) {
                        selectionModel.select(newVal);
                        receiptItemsTableView.scrollTo(newVal);
                    }
                    updateReceiptPreview(newVal);
                }
            })
        );
    }

    // =========================================================================
    // Keyboard Shortcuts (키보드 단축키)
    // =========================================================================
    
    /** ESC 키로 다이얼로그 종료 */
    private void setupKeyboardShortcuts() {
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
    }

    // =========================================================================
    // UI Update Methods (UI 업데이트 메서드)
    // =========================================================================
    
    /** 영수증 목록 뷰 새로고침 */
    private void refreshSalesView() {
        int size = salesViewModel.getSalesList().size();
        String status = size == 0 
            ? "No receipts found for selected date range"
            : "Ready - " + size + " receipts found";
        updateStatusMessage(status);
        
        refreshTableView(receiptItemsTableView);
        
        // 첫 번째 항목 자동 선택
        if (!salesViewModel.getSalesList().isEmpty()) {
            SaleModel firstSale = salesViewModel.getSalesList().get(0);
            receiptItemsTableView.getSelectionModel().select(firstSale);
        }
    }

    /** TableView 새로고침 헬퍼 */
    private void refreshTableView(TableView<?> tableView) {
        if (tableView != null) {
            tableView.refresh();
        }
    }

    /** 상태 메시지 업데이트 */
    private void updateStatusMessage(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    // =========================================================================
    // Receipt Preview (영수증 프리뷰)
    // =========================================================================
    
    /**
     * 영수증 프리뷰 업데이트 (선택된 영수증 기준)
     * @param saleModel 선택된 영수증
     */
    private void updateReceiptPreview(SaleModel saleModel) {
        if (saleModel == null) return;
        
        log.info("[PrintReceiptDialog] updateReceiptPreview for receipt: {}", saleModel.getReceiptNo());
        
        saleItems = salesViewModel.getSaleItemsList();
        logItemsInfo(saleItems);
        
        updatePreviewLabels(saleModel);
        loadReceiptHtml(saleModel, saleItems);
    }

    /**
     * 영수증 프리뷰 업데이트 (아이템 리스트 직접 전달)
     * @param saleModel 선택된 영수증
     * @param items 영수증 품목 리스트
     */
    private void updateReceiptPreview(SaleModel saleModel, List<SaleItemModel> items) {
        if (saleModel == null) return;
        
        log.info("[updateReceiptPreview] receipt={}, itemCount={}", 
            saleModel.getReceiptNo(), items != null ? items.size() : 0);
        
        updatePreviewLabels(saleModel);
        loadReceiptHtml(saleModel, items);
    }

    /** 아이템 정보 로깅 */
    private void logItemsInfo(List<SaleItemModel> items) {
        if (items == null || items.isEmpty()) {
            log.warn("[PrintReceiptDialog] No items found for receipt");
            return;
        }
        
        log.info("[PrintReceiptDialog] Current items count: {}", items.size());
        for (SaleItemModel item : items) {
            log.info("[PrintReceiptDialog] Item: {}, Qty: {}, Amount: {}", 
                item.getDescription(), item.getQuantity(), item.getSaleAmount());
        }
    }

    /** 프리뷰 라벨 업데이트 */
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
            double total = saleModel.getSaleAmount() != null 
                ? saleModel.getSaleAmount().doubleValue() 
                : 0;
            previewTotalLabel.setText(String.format("$%.2f", total));
        }
    }

    /** WebView에 HTML 영수증 로드 */
    private void loadReceiptHtml(SaleModel saleModel, List<SaleItemModel> items) {
        if (receiptWebView == null) return;
        
        Shop shop = shopViewModel.getCachedShop();
        String html = htmlGenerator.generateReceiptHTML(saleModel, items, shop);
        receiptWebView.getEngine().loadContent(html);
        log.info("[PrintReceiptDialog] Receipt preview loaded for: {}", saleModel.getReceiptNo());
    }

    // =========================================================================
    // Button Selection (버튼 선택 상태 관리)
    // =========================================================================
    
    /** 선택된 네비게이션 버튼 스타일 업데이트 */
    private void updateButtonSelection(Button selectedBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                String style = (btn == selectedBtn) 
                    ? "-fx-background-color: #3498db; -fx-text-fill: white;" 
                    : "";
                btn.setStyle(style);
            }
        }
    }

    // =========================================================================
    // Print Methods (출력 메서드)
    // =========================================================================
    
    /**
     * 영수증 출력 처리
     * WebView의 print() 메서드를 사용하여 현재 프리뷰를 출력
     */
    @FXML private void handlePrint() { 
        // 1. 선택된 영수증 검증
        SaleModel selectedSale = validateSelectedSale();
        if (selectedSale == null) return;
        
        eventPublisher.publishEvent(new ReceiptPrintEvent(
                    this, 
                    selectedSale, 
                    saleItems
                ));
    
        // updateStatusMessage(success 
        //     ? "Print completed successfully: " + receiptNo
        //     : "Print failed or canceled: " + receiptNo);
        
        //notifyPrintCallback(receiptNo);
    }

    /**
     * 프리뷰 출력 버튼 핸들러
     * 콜백을 통해 외부에서 프리뷰 처리
     */
    @FXML
    private void handlePreviewPrint() {
        SaleModel selectedSale = validateSelectedSale();
        if (selectedSale == null) return;
        
        String receiptNo = selectedSale.getReceiptNo();
        log.info("[PrintReceiptDialog] Previewing receipt: {}", receiptNo);
        
        if (callback != null) {
            callback.onPreview(receiptNo);
        }
    }

    /** 선택된 영수증 검증 */
    private SaleModel validateSelectedSale() {
        SaleModel selectedSale = salesViewModel.selectedSaleProperty().get();
        if (selectedSale == null) {
            updateStatusMessage("Please select a receipt to print");
            return null;
        }
        return selectedSale;
    }

    /** WebView 준비 상태 확인 */
    @SuppressWarnings("unused")
    private boolean isWebViewReady() {
        return receiptWebView != null && receiptWebView.getEngine() != null;
    }

    /** 출력 콜백 호출 */
    // private void notifyPrintCallback(String receiptNo) {
    //     if (callback != null) {
    //         callback.onPrint(receiptNo);
    //     }
    // }

    // =========================================================================
    // BasePosDialog Implementation (추상 메서드 구현)
    // =========================================================================
    
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
        closeDialog();
    }

    // =========================================================================
    // Dialog Close (다이얼로그 종료)
    // =========================================================================
    

    // =========================================================================
    // Public Methods (외부 호출 메서드)
    // =========================================================================
    
    /**
     * 외부에서 콜백 설정
     * @param callback 영수증 출력 관련 콜백
     */
    public void initData(PrintReceiptCallback callback) {
        this.callback = callback;
    }

    /**
     * 영수증 번호 설정 (외부에서 호출)
     * @param receiptNo 영수증 번호
     */
    public void setReceiptNumber(String receiptNo) {
        if (receiptNoLabel != null) {
            receiptNoLabel.setText(receiptNo);
        }
    }

    // =========================================================================
    // Utility Methods (유틸리티 메서드)
    // =========================================================================
    
    /**
     * BigDecimal 금액을 통화 형식으로 변환
     * @param amount 금액
     * @return 통화 형식 문자열 (예: $1,234.56)
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return DEFAULT_CURRENCY_ZERO;
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        return currencyFormat.format(amount);
    }

    // =========================================================================
    // Callback Interface (콜백 인터페이스)
    // =========================================================================
    
    /**
     * 영수증 출력 다이얼로그 콜백 인터페이스
     * 외부에서 다이얼로그의 이벤트를 처리하기 위해 사용
     */
    @FunctionalInterface
    public interface PrintReceiptCallback {
        /**
         * 검색 버튼 클릭 시 호출
         * @param startDate 검색 시작일
         * @param endDate 검색 종료일
         */
        void onSearch(LocalDate startDate, LocalDate endDate);
        
        /**
         * 출력 버튼 클릭 시 호출 (선택적)
         * @param receiptNo 출력할 영수증 번호
         */
        default void onPrint(String receiptNo) {
            log.info("Default print: {}", receiptNo);
        }
        
        /**
         * 프리뷰 버튼 클릭 시 호출 (선택적)
         * @param receiptNo 프리뷰할 영수증 번호
         */
        default void onPreview(String receiptNo) {
            log.info("Default preview: {}", receiptNo);
        }
    }
}