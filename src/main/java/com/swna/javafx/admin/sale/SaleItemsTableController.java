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
 * TableColumnUtil을 적용한 리팩토링 버전
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleItemsTableController implements Initializable {
    
    private final SalesViewModel viewModel;
    
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private TableColumn<SaleItemModel, String> barcodeColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> amountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> priceColumn;
    @FXML private TableColumn<SaleItemModel, Integer> qtyColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> discountColumn;
    @FXML private TableColumn<SaleItemModel, String> codeColumn;
    @FXML private TableColumn<SaleItemModel, String> supplierColumn;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupTableBindings();
    }
    
    /**
     * TableColumnUtil을 사용한 컬럼 설정
     */
    private void setupTableColumns() {
        // 1. 바코드 컬럼
        TableColumnUtil.makeStringColumn(
            barcodeColumn,
            SaleItemModel::barcodeProperty,
            null,
            false,  // 읽기 전용
            true,
            TableColumnUtil.CENTER,
            null
        );
        
        // 2. 금액(소계) 컬럼 - BigDecimal 통화 형식
        TableColumnUtil.makeBigDecimalCurrencyColumn(
            amountColumn,
            SaleItemModel::subtotalProperty,
            false,  // 읽기 전용
            true,
            TableColumnUtil.RIGHT,
            null
        );
        
        // 3. 단가 컬럼 - BigDecimal 통화 형식
        TableColumnUtil.makeBigDecimalCurrencyColumn(
            priceColumn,
            SaleItemModel::priceProperty,
            false,  // 읽기 전용
            true,
            TableColumnUtil.RIGHT,
            null
        );
        
        // 4. 수량 컬럼 - Integer 형식
        TableColumnUtil.makeIntegerColumn(
            qtyColumn,
            SaleItemModel::quantityProperty,
            null,  // 읽기 전용
            true,
            false,
            TableColumnUtil.CENTER,
            null
        );
        
        // 5. 할인액 컬럼 - BigDecimal 통화 형식
        TableColumnUtil.makeBigDecimalCurrencyColumn(
            discountColumn,
            SaleItemModel::discountProperty,
            false,  // 읽기 전용
            true,
            TableColumnUtil.RIGHT,
            null
        );
        
        // 6. 상품명/코드 컬럼
        TableColumnUtil.makeStringColumn(
            codeColumn,
            SaleItemModel::productNameProperty,
            null,
            false,
            true,
            TableColumnUtil.LEFT,
            null
        );
        
        // 7. 공급사 컬럼
        TableColumnUtil.makeStringColumn(
            supplierColumn,
            SaleItemModel::supplierProperty,
            null,
            false,
            true,
            TableColumnUtil.LEFT,
            null
        );
    }
    
    /**
     * 테이블 데이터 바인딩
     */
    private void setupTableBindings() {
        // TableView에 ViewModel의 아이템 리스트 직접 설정
        saleItemsTableView.setItems(viewModel.getSaleItemsList());
        
        // ViewModel 리스트 변경 시 테이블 새로고침
        viewModel.getSaleItemsList().addListener((javafx.collections.ListChangeListener<SaleItemModel>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    saleItemsTableView.refresh();
                }
            }
        });
    }
}