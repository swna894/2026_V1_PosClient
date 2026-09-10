package com.swna.javafx.common.tableutils;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class NumberColumnBuilder<S, N extends Number> {

    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT; -fx-padding: 0 4 0 4;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT; -fx-padding: 0 4 0 4;";
    private static final String STYLE_CENTER = "-fx-alignment: CENTER; -fx-padding: 0 4 0 4;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";

    private final TableView<S> tableView;
    private final String title;
    private final Function<S, ObservableValue<N>> propertyGetter;

    private boolean isVisible = true;
    private String alignment = "RIGHT"; // 숫자는 기본 우측 정렬
    private boolean useGrouping = true;  // 천 단위 쉼표(,) 기본 적용

    private int width = -1;
    private int minWidth = -1;
    private int maxWidth = -1;
    private boolean resizable = true;

    public NumberColumnBuilder(TableView<S> tableView, String title, Function<S, ObservableValue<N>> propertyGetter) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
    }

    public static <S, N extends Number> NumberColumnBuilder<S, N> numberColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObservableValue<N>> propertyGetter) {
        return new NumberColumnBuilder<>(tableView, title, propertyGetter);
    }

    public NumberColumnBuilder<S, N> alignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    public NumberColumnBuilder<S, N> useGrouping(boolean useGrouping) {
        this.useGrouping = useGrouping;
        return this;
    }

    public NumberColumnBuilder<S, N> fixedWidth(int width) {
        this.width = width;
        this.minWidth = width;
        this.maxWidth = width;
        this.resizable = false;
        return this;
    }

    public TableColumn<S, N> build() {
        TableColumn<S, N> column = new TableColumn<>(title);

        if (width > 0) column.setPrefWidth(width);
        if (minWidth > 0) column.setMinWidth(minWidth);
        if (maxWidth > 0) column.setMaxWidth(maxWidth);
        column.setResizable(resizable);

        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setEditable(false);

        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new NumberTableCell<>(useGrouping));

        if (tableView != null) {
            tableView.getColumns().add(column);
        }

        return column;
    }

    private static String getAlignmentStyle(String alignment) {
        if (alignment == null) return STYLE_RIGHT;
        return switch (alignment.toUpperCase()) {
            case "LEFT" -> STYLE_LEFT;
            case "CENTER" -> STYLE_CENTER;
            default -> STYLE_RIGHT;
        };
    }

    // 숫자 포맷팅 전용 Cell
    private static class NumberTableCell<S, N extends Number> extends TableCell<S, N> {
        private final NumberFormat numberFormat;

        public NumberTableCell(boolean useGrouping) {
            this.numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            this.numberFormat.setGroupingUsed(useGrouping);
        }

        @Override
        protected void updateItem(N item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(numberFormat.format(item));
            }
        }
    }
}
