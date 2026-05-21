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

/**
 * 테이블 뷰 및 요약 바 데이터 동기화 컨트롤러 (리팩토링 완성본)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTableController implements Initializable {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final SalesViewModel viewModel;
    
    // 💡 FXML 상단 합계 라벨 컴포넌트 매핑
    @FXML private Label totalAmountLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label totalDiscountLabel;
    @FXML private Label totalCashLabel;
    @FXML private Label totalEftposLabel;
    @FXML private Label totalCashoutLabel;
    
    // 메인 테이블 컴포넌트
    @FXML private TableView<SaleModel> salesTableView;
    @FXML private TableColumn<SaleModel, String> noColumn;
    @FXML private TableColumn<SaleModel, String> receiptColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> originalAmountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> saleAmountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> cashColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> creditColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> cashoutColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> discountColumn;
    @FXML private TableColumn<SaleModel, BigDecimal> costColumn;
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
     * 💡 [완성] ViewModel의 모든 집계 프로퍼티들과 실시간 라벨 1:1 결합 자동화
     */
    private void setupSummaryBindings() {
        totalAmountLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalSalesAmountProperty())
        );
        totalCostLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalCostAmountProperty())
        );
        totalDiscountLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalDiscountAmountProperty())
        );
        totalCashLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalCashAmountProperty())
        );
        totalEftposLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalCreditAmountProperty())
        );
        totalCashoutLabel.textProperty().bind(
            Bindings.format("$%,.2f", viewModel.totalCashoutAmountProperty())
        );
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