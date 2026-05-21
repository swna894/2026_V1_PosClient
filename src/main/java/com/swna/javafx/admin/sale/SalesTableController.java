package com.swna.javafx.admin.sale;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTableController implements Initializable {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CURRENCY_FORMAT = "$%,.2f";
    
    private final SalesViewModel viewModel;
    
    // ========== 원본에 존재했던 모든 상단 합계 통계 요약 바 필드 ==========
    // (FXML에 없는 totalAmountLabel은 제거 - 에러 원인)
    @FXML private Label totalCostLabel;        // COST
    @FXML private Label totalDiscountLabel;    // D/C
    @FXML private Label totalCashLabel;        // CASH
    @FXML private Label totalEftposLabel;      // CREDIT
    @FXML private Label totalCashoutLabel;     // CASH OUT
    
    // ========== FXML의 ToolBar에 실제 정의된 Label들 (추가 필요) ==========
    @FXML private Label originalTotalLabel;    // Orig. TOTAL
    @FXML private Label finalTotalLabel;       // Final TOTAL
    @FXML private Label discountTotalLabel;    // Dis. TOTAL
    
    // 메인 테이블 컴포넌트
    @FXML private TableView<SaleModel> salesTableView;
    @FXML private TableColumn<SaleModel, String> noColumn;
    @FXML private TableColumn<SaleModel, String> receiptColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> saleAmountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> originalAmountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> discountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> costColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> cashColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> creditColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> cashoutColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> balanceColumn;
    @FXML private TableColumn<SaleModel, String> acceptColumn;
    @FXML private TableColumn<SaleModel, LocalDateTime> dateColumn;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupTableBindings();
        setupSummaryBindings();
        setupSelectionListener();
    }
    
    private void setupTableColumns() {
        TableColumnUtil.createNumberColumn(salesTableView, noColumn, 50);
        TableColumnUtil.makeStringColumn(receiptColumn, SaleModel::receiptNoProperty, null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(saleAmountColumn, SaleModel::saleAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(originalAmountColumn, SaleModel::originalAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountColumn, SaleModel::discountAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(costColumn, SaleModel::costAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(cashColumn, SaleModel::cashAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(creditColumn, SaleModel::creditAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(cashoutColumn, SaleModel::cashoutAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(balanceColumn, SaleModel::changeAmountProperty, false, false, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeStringColumn(acceptColumn, SaleModel::paymentTypeProperty, null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeDateTimeColumn(dateColumn, SaleModel::getPaymentDateTime, null, false, true, TableColumnUtil.CENTER, DATE_TIME_FORMATTER, null);
    }
    
    private void setupTableBindings() {
        salesTableView.setItems(viewModel.getSalesList());
        
        viewModel.getSalesList().addListener((javafx.collections.ListChangeListener<SaleModel>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    salesTableView.refresh();
                }
            }
        });
    }
    
    /**
     * 모든 Label에 대해 Null-safe하게 바인딩 설정
     * - 원본에 있던 필드(totalCostLabel, totalDiscountLabel, totalCashLabel, totalEftposLabel, totalCashoutLabel) 유지
     * - FXML에 정의된 추가 Label(originalTotalLabel, finalTotalLabel, discountTotalLabel)도 바인딩
     */
    private void setupSummaryBindings() {
        // 원본에 존재하던 필드들
        if (totalCostLabel != null) {
            totalCostLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCostAmountProperty()));
        }
        if (totalDiscountLabel != null) {
            totalDiscountLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalDiscountAmountProperty()));
        }
        if (totalCashLabel != null) {
            totalCashLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCashAmountProperty()));
        }
        if (totalEftposLabel != null) {
            totalEftposLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCreditAmountProperty()));
        }
        if (totalCashoutLabel != null) {
            totalCashoutLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCashoutAmountProperty()));
        }
        
        // FXML에 정의되어 있지만 원본 Controller에 없었던 필드들 (추가 바인딩)
        if (originalTotalLabel != null) {
            originalTotalLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalOriginalAmountProperty()));
        }
        if (finalTotalLabel != null) {
            finalTotalLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalSalesAmountProperty()));
        }
        if (discountTotalLabel != null) {
            discountTotalLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalDiscountAmountProperty()));
        }
    }
    
    private void setupSelectionListener() {
        salesTableView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null && viewModel.selectedSaleProperty().get() != newVal) {
                    viewModel.selectedSaleProperty().set(newVal);
                    viewModel.onSelectedSaleChanged(newVal); 
                }
            });
            
        viewModel.selectedSaleProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null) {
                    var selectionModel = salesTableView.getSelectionModel();
                    if (selectionModel.getSelectedItem() != newVal) {
                        selectionModel.select(newVal);
                        salesTableView.scrollTo(newVal);
                    }
                } else {
                    salesTableView.getSelectionModel().clearSelection();
                }
            });
        });
    }
    
    public TableView<SaleModel> getSalesTableView() {
        return salesTableView;
    }
}