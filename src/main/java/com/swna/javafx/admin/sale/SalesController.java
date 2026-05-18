package com.swna.javafx.admin.sale;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.pos.PosViewController;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 판매 메인 컨트롤러
 * 리팩토링된 MainSalesView.fxml에 맞게 수정됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesController implements Initializable {
    
    private final SalesViewModel viewModel;
    private final NavigationService navigationService;
    
    // ========== ToolBar Controls ==========
    @FXML private Button backButton;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button todayBtn;
    @FXML private Button weekBtn;
    @FXML private Button monthBtn;
    @FXML private Button searchButton;
    @FXML private Button excelButton;
    @FXML private Button reloadButton;
    @FXML private Button deleteButton;

    @FXML private SplitPane splitPane;
    
    // ========== Daily Summary Labels (ToolBar 내부) ==========
    @FXML private Label summaryDateLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Label summaryDiscountLabel;
    @FXML private Label summaryCashLabel;
    @FXML private Label summaryCreditLabel;
    @FXML private Label summaryCashoutLabel;
    
    // ========== Loading & Status ==========
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label totalCountLabel;
    @FXML private Label lblStatus;
    
    // ========== Included Components ==========
    @FXML private SalesTableController salesTableController;
    @FXML private SaleItemsTableController saleItemsTableController;
    
    // 날짜 포맷터
    private static final java.text.DecimalFormat CURRENCY_FORMATTER = new java.text.DecimalFormat("#,###,###");
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBindings();
        setupDatePickers();
        setupButtonActions();
        setupSelectionListener();
        fixSplitPaneDivider();
        // 초기 데이터 로드
        viewModel.loadTodaySales();
    }
    
    private void fixSplitPaneDivider() {
    Platform.runLater(() -> splitPane.setDividerPositions(0.60));
    
    splitPane.sceneProperty().addListener((obs, old, newScene) -> {
        if (newScene != null) {
            Platform.runLater(() -> splitPane.setDividerPositions(0.60));
        }
    });
}
    private void setupBindings() {

        // DatePicker 바인딩
        if (startDatePicker != null) {
            startDatePicker.valueProperty().bindBidirectional(viewModel.startDateProperty());
        }
        if (endDatePicker != null) {
            endDatePicker.valueProperty().bindBidirectional(viewModel.endDateProperty());
        }
        
        // 로딩 상태 바인딩
        if (progressIndicator != null) {
            viewModel.loadingProperty().addListener((obs, old, val) -> 
                progressIndicator.setVisible(val != null && val)
            );
        }
        
        // 상태 메시지 바인딩
        if (lblStatus != null) {
            viewModel.errorMessageProperty().addListener((obs, old, msg) -> 
                lblStatus.setText(msg != null ? msg : "Ready")
            );
        }
        
        // 전체 건수 바인딩
        if (totalCountLabel != null) {
            totalCountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> String.format("Total: %d", viewModel.totalCountProperty().get()),
                    viewModel.totalCountProperty()
                )
            );
        }
        
        // 요약 정보 바인딩 (ToolBar 내부)
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
                    () -> formatCurrency(viewModel.totalSalesAmountProperty().get()),
                    viewModel.totalSalesAmountProperty()
                )
            );
        }
        
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalDiscountAmountProperty().get()),
                    viewModel.totalDiscountAmountProperty()
                )
            );
        }
        
        if (summaryCashLabel != null) {
            summaryCashLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> {
                        BigDecimal cashAmount = viewModel.totalReceivedAmountProperty().get()
                            .subtract(viewModel.totalCashoutAmountProperty().get());
                        return formatCurrency(cashAmount);
                    },
                    viewModel.totalReceivedAmountProperty(),
                    viewModel.totalCashoutAmountProperty()
                )
            );
        }
        
        if (summaryCreditLabel != null) {
            summaryCreditLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalReceivedAmountProperty().get()),
                    viewModel.totalReceivedAmountProperty()
                )
            );
        }
        
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalCashoutAmountProperty().get()),
                    viewModel.totalCashoutAmountProperty()
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
    
    private void setupButtonActions() {
        // Back Button
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
        }
        
        // Date Filter Buttons
        if (todayBtn != null) {
            todayBtn.setOnAction(e -> viewModel.loadTodaySales());
        }
        if (weekBtn != null) {
            weekBtn.setOnAction(e -> viewModel.loadThisWeekSales());
        }
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> viewModel.loadThisMonthSales());
        }
        
        // Search Button
        if (searchButton != null) {
            searchButton.setOnAction(e -> viewModel.loadSalesByDateRange());
        }
        
        // Excel Export
        if (excelButton != null) {
            excelButton.setOnAction(e -> handleExportExcel());
        }
        
        // Reload Button
        if (reloadButton != null) {
            reloadButton.setOnAction(e -> viewModel.refresh());
        }
        
        // Delete Button
        if (deleteButton != null) {
            deleteButton.setOnAction(e -> handleDelete());
        }
    }
    
    private void setupSelectionListener() {
        // SalesTable에서 선택 변경 시 ViewModel에通知
        // if (salesTableController != null && salesTableController.getSalesTableView() != null) {
        //     salesTableController.getSalesTableView().getSelectionModel().selectedItemProperty()
        //         .addListener((obs, oldVal, newVal) -> {
        //             if (newVal != null) {
        //                 viewModel.onSelectedSaleChanged(newVal);
        //             }
        //         });
        // }
    }
    
    private void handleBack() {
        navigationService.navigateStage(PosViewController.class);
    }
    
    private void handleExportExcel() {
        log.info("Excel export button clicked");
    }
    
    private void handleDelete() {
        // if (salesTableController != null && salesTableController.getSalesTableView() != null) {
        //     var selectedItem = salesTableController.getSalesTableView().getSelectionModel().getSelectedItem();
        //     if (selectedItem != null) {
        //         log.info("Deleting sale: {}", selectedItem.getReceiptNo());
        //         // TODO: 삭제 API 호출 후 목록 새로고침
        //     }
        // }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0";
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
        return currencyFormat.format(amount);
    }
    
    // ========== Public Methods for External Access ==========
    
    public void refresh() {
        viewModel.refresh();
    }
    
    public void setDateRange(LocalDate start, LocalDate end) {
        if (startDatePicker != null) startDatePicker.setValue(start);
        if (endDatePicker != null) endDatePicker.setValue(end);
        viewModel.loadSalesByDateRange();
    }
}