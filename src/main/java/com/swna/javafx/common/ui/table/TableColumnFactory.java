package com.swna.javafx.common.ui.table;

import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableColumnFactory {

    // 기본 컬럼 생성
    public static <T, R> TableColumn<T, R> createColumn(String title, String property) {

        TableColumn<T, R> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));

        return column;
    }

    // 정렬 가능 컬럼
    public static <T, R> TableColumn<T, R> createSortableColumn(String title, String property) {

        TableColumn<T, R> column = createColumn(title, property);
        column.setSortable(true);

        return column;
    }

    // 가운데 정렬 (공통 스타일)
    public static <T> TableColumn<T, ?> createCenteredColumn(String title, String property) {

        TableColumn<T, ?> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));

        column.setStyle("-fx-alignment: CENTER;");

        return column;
    }
}
