package com.swna.javafx.common.ui.table;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableViewHelper {

    // 컬럼 바인딩
    public static <T> void bindColumn(TableColumn<T, ?> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
    }

    // 데이터 설정
    public static <T> void setItems(TableView<T> table, ObservableList<T> data) {
        table.setItems(data);
    }

    // 숫자 컬럼 정렬 (예시)
    public static <T> void setNumericColumn(TableColumn<T, Number> column) {
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
    }
}
