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
    
    /**
     * 테이블 선택 상태 양방향 동기화 및 연쇄 조회 리스너
     */
    private void setupSelectionListener() {
        // [A] 사용자가 테이블 행을 마우스나 키보드로 선택했을 때 -> ViewModel 상태 변경 및 상세 아이템 조회 요청
        salesTableView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null && viewModel.selectedSaleProperty().get() != newVal) {
                    viewModel.selectedSaleProperty().set(newVal);
                    viewModel.onSelectedSaleChanged(newVal); 
                }
            });
            
        // [B] 오늘/이번주/이번달 버튼 클릭 등으로 ViewModel 측에서 첫 행을 강제 선택했을 때 -> UI 테이블 뷰 파란색 하이라이트 동기화
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