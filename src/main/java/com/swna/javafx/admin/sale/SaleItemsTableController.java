package com.swna.javafx.admin.sale;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 판매 아이템 목록 테이블 컨트롤러
 * SaleItemModel의 단가/총액 자동 연산 구조에 맞춰 컬럼 바인딩 리팩토링 완료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleItemsTableController implements Initializable {
    
    private final SalesViewModel viewModel;
    
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private TableColumn<SaleItemModel, String> barcodeColumn;
    @FXML private TableColumn<SaleItemModel, Integer> qtyColumn;
    
    // 단가 관련 컬럼
    @FXML private TableColumn<SaleItemModel, BigDecimal> originalPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> discountPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> salePriceColumn;
    
    // 총액 관련 컬럼
    @FXML private TableColumn<SaleItemModel, BigDecimal> discountAmountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> saleAmountColumn;
    
    // 기타 컬럼
    @FXML private TableColumn<SaleItemModel, BigDecimal> costColumn;
    @FXML private TableColumn<SaleItemModel, String> codeColumn;
    @FXML private TableColumn<SaleItemModel, String> commentColumn;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupTableBindings();
    }
    
    /**
     * 모델의 속성과 FXML 컬럼 간의 바인딩 설정
     */
    private void setupTableColumns() {
        // 1. 기본 정보
        TableColumnUtil.makeStringColumn(barcodeColumn, SaleItemModel::barcodeProperty, null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeIntegerColumn(qtyColumn, SaleItemModel::quantityProperty, null, true, true, TableColumnUtil.CENTER, null);
        
        // 2. 판매 총액 (가장 중요)
        TableColumnUtil.makeBigDecimalCurrencyColumn(saleAmountColumn, SaleItemModel::saleAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        
        // 3. 단가 정보 (판매단가, 할인단가, 정상단가)
        TableColumnUtil.makeBigDecimalCurrencyColumn(salePriceColumn, SaleItemModel::salePriceProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountPriceColumn, SaleItemModel::discountPriceProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(originalPriceColumn, SaleItemModel::originalPriceProperty, false, true, TableColumnUtil.RIGHT, null);
        
        // 4. 할인 총액
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountAmountColumn, SaleItemModel::discountAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        
        // 5. 관리 정보 (원가, ID, 메모)
        TableColumnUtil.makeBigDecimalCurrencyColumn(costColumn, SaleItemModel::costProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeStringColumn(codeColumn, SaleItemModel::idProperty, null, false, true, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeStringColumn(commentColumn, SaleItemModel::commentProperty, null, false, true, TableColumnUtil.LEFT, null);
    }
    
    /**
     * 데이터 리스트 바인딩 및 새로고침 로직
     */
    private void setupTableBindings() {
        saleItemsTableView.setItems(viewModel.getSaleItemsList());
        
        viewModel.getSaleItemsList().addListener((javafx.collections.ListChangeListener<SaleItemModel>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    saleItemsTableView.refresh();
                }
            }
        });
    }
}