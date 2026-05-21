package com.swna.javafx.admin.sale;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaleItemsTableController implements Initializable {
    
    // 중복 리터럴 방지를 위한 통화 포맷 선언
    private static final String CURRENCY_FORMAT = "$%,.2f";
    
    private final SalesViewModel viewModel;
    
    // 💡 추가된 선택 전표 영수증 라벨 매핑
    @FXML private Label itemSelectedReceiptLabel;
    
    @FXML private Label itemTotalAmountLabel;
    @FXML private Label itemTotalOriginalLabel;
    @FXML private Label itemTotalDiscountLabel;
    @FXML private Label itemTotalCostLabel;
    @FXML private Label itemTotalMarginLabel;
    @FXML private Label itemTotalQtyLabel;
    
    @FXML private TableView<SaleItemModel> saleItemsTableView;
    @FXML private TableColumn<SaleItemModel, String> barcodeColumn;
    @FXML private TableColumn<SaleItemModel, Integer> qtyColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> originalPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> discountPriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> salePriceColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> discountAmountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> saleAmountColumn;
    @FXML private TableColumn<SaleItemModel, BigDecimal> costColumn;
    @FXML private TableColumn<SaleItemModel, String> codeColumn;
    @FXML private TableColumn<SaleItemModel, String> commentColumn;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupTableBindings();
        setupSummaryBindings(); 
    }
    
    private void setupTableColumns() {
        TableColumnUtil.makeStringColumn(barcodeColumn, SaleItemModel::barcodeProperty, null, false, true, TableColumnUtil.CENTER, null);
        TableColumnUtil.makeIntegerColumn(qtyColumn, SaleItemModel::quantityProperty, null, false, true, TableColumnUtil.CENTER, null);
        
        TableColumnUtil.makeBigDecimalCurrencyColumn(salePriceColumn, SaleItemModel::salePriceProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(saleAmountColumn, SaleItemModel::saleAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountPriceColumn, SaleItemModel::discountPriceProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(originalPriceColumn, SaleItemModel::originalPriceProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(discountAmountColumn, SaleItemModel::discountAmountProperty, false, true, TableColumnUtil.RIGHT, null);
        TableColumnUtil.makeBigDecimalCurrencyColumn(costColumn, SaleItemModel::costProperty, false, true, TableColumnUtil.RIGHT, null);
        
        TableColumnUtil.makeStringColumn(codeColumn, SaleItemModel::idProperty, null, false, true, TableColumnUtil.LEFT, null);
        TableColumnUtil.makeStringColumn(commentColumn, SaleItemModel::commentProperty, null, false, true, TableColumnUtil.LEFT, null);
    }
    
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

    private void setupSummaryBindings() {
        // 💡 핵심 추가: 메인 테이블에서 선택된 SaleModel의 receiptNo 속성을 안전하게 추적 및 바인딩
        itemSelectedReceiptLabel.textProperty().bind(
            Bindings.selectString(viewModel.selectedSaleProperty(), "receiptNo")
        );

        // 기존 요약 통계 속성 바인딩
        itemTotalAmountLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.itemTotalAmountProperty()));
        itemTotalOriginalLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.itemTotalOriginalProperty()));
        itemTotalDiscountLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.itemTotalDiscountProperty()));
        itemTotalCostLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.itemTotalCostProperty()));
        itemTotalMarginLabel.textProperty().bind(Bindings.format(CURRENCY_FORMAT, viewModel.itemTotalMarginProperty()));
        itemTotalQtyLabel.textProperty().bind(viewModel.itemTotalQtyProperty().asString());
    }
    
    public TableView<SaleItemModel> getSaleItemsTableView() {
        return saleItemsTableView;
    }
}