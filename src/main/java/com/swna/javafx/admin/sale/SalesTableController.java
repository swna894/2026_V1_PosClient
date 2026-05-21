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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTableController implements Initializable {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER =  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final SalesViewModel viewModel;
    
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
        setupSelectionListener();
    }
    
    private void setupTableColumns() {
        // 1. 번호 컬럼
        TableColumnUtil.createNumberColumn(salesTableView, noColumn, 50);
        // 2. 영수증 번호
        TableColumnUtil.makeStringColumn(receiptColumn, SaleModel::receiptNoProperty,
            null, false, true, TableColumnUtil.CENTER, null);
        
        // 3. 총액
        TableColumnUtil.makeBigDecimalCurrencyColumn(saleAmountColumn, SaleModel::saleAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);

        TableColumnUtil.makeBigDecimalCurrencyColumn(originalAmountColumn, SaleModel::originalAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 4. 할인액
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountColumn, SaleModel::discountAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 4-1. 원가 총액
        TableColumnUtil.makeBigDecimalCurrencyColumn(costColumn, SaleModel::costAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 5. 현금 (receivedAmount - cashoutAmount)
        TableColumnUtil.makeBigDecimalCurrencyColumn(cashColumn, SaleModel::cashAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 6. EFTPOS (CARD 결제 금액)
        TableColumnUtil.makeBigDecimalCurrencyColumn(creditColumn, SaleModel::creditAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 7. 현금인출
        TableColumnUtil.makeBigDecimalCurrencyColumn(cashoutColumn, SaleModel::cashoutAmountProperty,
            false, true,TableColumnUtil.RIGHT, null);
        
        // 8. 잔액
        TableColumnUtil.makeBigDecimalCurrencyColumn(balanceColumn, SaleModel::changeAmountProperty,
            false, false,TableColumnUtil.RIGHT, null);
        
        // 9. 결제 방식
        TableColumnUtil.makeStringColumn(acceptColumn, SaleModel::paymentTypeProperty,
            null, false, true,TableColumnUtil.CENTER, null);
        
        // 10. 날짜
        TableColumnUtil.makeDateTimeColumn(dateColumn, SaleModel::getPaymentDateTime,
            null, false, true,TableColumnUtil.CENTER, DATE_TIME_FORMATTER, null);
    }
    
    private void setupTableBindings() {
        // ✅ 간단하게 직접 설정 (권장)
        salesTableView.setItems(viewModel.getSalesList());
        
        // 또는 ViewModel의 리스트 변경을 감지하여 업데이트
        viewModel.getSalesList().addListener((javafx.collections.ListChangeListener<SaleModel>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    salesTableView.refresh();
                }
            }
        });
    }
    
    private void setupSelectionListener() {
        salesTableView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    viewModel.onSelectedSaleChanged(newVal);
                }
            });
    }
}