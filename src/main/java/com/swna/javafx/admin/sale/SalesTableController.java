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
 * 메인 판매 전표 테이블 및 통합 상단 요약 바 제어 컨트롤러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTableController implements Initializable {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // 💡 중복 리터럴 방지를 위한 통화 포맷 상수 선언
    private static final String CURRENCY_FORMAT = "$%,.2f";
    
    private final SalesViewModel viewModel;
    
    // 상단 합계 통계 요약 바 컴포넌트 맵핑
    @FXML private Label totalAmountLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label totalDiscountLabel;
    @FXML private Label totalCashLabel;
    @FXML private Label totalEftposLabel;
    @FXML private Label totalCashoutLabel;
    
    // 메인 테이블 컴포넌트 및 제공해주신 원본 순서 컬럼 선언
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
    
    /**
     * ⭕ 전달해주신 원본 구조와 컬럼 매핑 로직을 완전히 일치시켜 복구한 메서드
     */
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
        TableColumnUtil.makeDateTimeColumn(dateColumn, SaleModel::getPaymentDateTime, null, false, true,TableColumnUtil.CENTER, DATE_TIME_FORMATTER, null);
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
     * ViewModel의 실시간 정산 집계 속성과 상단 요약 바 라벨 간의 1:1 결합 자동화
     */
    private void setupSummaryBindings() {
        // 💡 CURRENCY_FORMAT 상수를 활용하여 중복 리터럴 없이 안전하게 바인딩 처리 완료
        totalAmountLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalSalesAmountProperty()));
        totalDiscountLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalDiscountAmountProperty()));
        totalCashoutLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCashoutAmountProperty()));
        totalCostLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCostAmountProperty()));
        totalCashLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCashAmountProperty()));
        totalEftposLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.totalCreditAmountProperty()));
    }
    
    /**
     * 테이블 행 선택 상태 양방향 동기화 및 상세 아이템 연쇄 조회 리스너
     */
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