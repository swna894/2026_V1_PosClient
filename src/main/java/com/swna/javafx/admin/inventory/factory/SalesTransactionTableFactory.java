package com.swna.javafx.admin.inventory.factory;

import static com.swna.javafx.common.tableutils.TableColumnUtils.numberColumn;
import static com.swna.javafx.common.tableutils.TableColumnUtils.stringColumn;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.model.SalesTransactionDate;
import com.swna.javafx.admin.inventory.viewmodel.SalesTransactionViewModel;
import com.swna.javafx.common.tableutils.TableUtil;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;

@Getter
@Component
public class SalesTransactionTableFactory {

    private TableView<SalesTransactionDate> table;

    private TableColumn<SalesTransactionDate, String> colDate;
    private TableColumn<SalesTransactionDate, Integer> colQty; // 1. 타입 수정 (String -> Integer)

    public TableView<SalesTransactionDate> initializeTable(TableView<SalesTransactionDate> existingTable, SalesTransactionViewModel viewModel) {
        this.table = existingTable;
        createColumns();
        this.table.setItems(viewModel.getSalesDataList());
        return table;
    }

    private void createColumns() {
        TableUtil.createNumberColumn(table, "NO", 55);

        // fixedWidth() 제거 → 폭은 아래에서 직접 지정
        this.colDate = stringColumn(table, "DATE / PERIOD", SalesTransactionDate::dateProperty)
                .alignment(TableUtil.CENTER)
                .build();

        this.colQty = numberColumn(table, "QTY SOLD", item -> item.countProperty().asObject())
                .alignment(TableUtil.RIGHT)
                .build();

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 테이블 폭에 비례해서 컬럼 폭이 변하도록 설정
        bindColumnWidth(colDate, 0.5);
        bindColumnWidth(colQty, 0.5);
    }

    /**
     * 컬럼 폭을 (테이블 폭 - NO 컬럼 폭 - 여백) × ratio 로 바인딩
     */
    private void bindColumnWidth(TableColumn<SalesTransactionDate, ?> column, double ratio) {
        column.setMinWidth(60);
        column.setMaxWidth(Double.MAX_VALUE);
        column.prefWidthProperty().bind(
                table.widthProperty().subtract(55 + 20).multiply(ratio)   // 55: NO 컬럼, 20: 스크롤바/여백
        );
    }
}