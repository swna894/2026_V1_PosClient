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
        this.colDate = stringColumn(table, "DATE / PERIOD", SalesTransactionDate::dateProperty).alignment(TableUtil.CENTER).fixedWidth(120).build();

        // 2. 람다식 및 .asObject() 적용
        this.colQty = numberColumn(table, "QTY SOLD", item -> item.countProperty().asObject()).alignment(TableUtil.RIGHT).fixedWidth(120).build();

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}