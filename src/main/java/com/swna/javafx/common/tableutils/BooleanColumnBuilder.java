package com.swna.javafx.common.tableutils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

public class BooleanColumnBuilder<S> {

    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";

    private final TableView<S> tableView;
    private final String title;
    private final Function<S, BooleanProperty> propertyGetter;

    private BiConsumer<S, Boolean> setter = null;
    private boolean editable = true;
    private boolean isVisible = true;
    
    // 너비 관련 필드
    private int width = -1;
    private int minWidth = -1;
    private int maxWidth = -1;
    private boolean resizable = true;

    // 변경 감지용 (dirty 처리 등) Consumer
    private Consumer<S> dirtyConsumer = null;

    public BooleanColumnBuilder(TableView<S> tableView, String title, Function<S, BooleanProperty> propertyGetter) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
    }

    public static <S> BooleanColumnBuilder<S> booleanColumn(
            TableView<S> tableView,
            String title,
            Function<S, BooleanProperty> propertyGetter) {
        return new BooleanColumnBuilder<>(tableView, title, propertyGetter);
    }

    public BooleanColumnBuilder<S> setter(BiConsumer<S, Boolean> setter) {
        this.setter = setter;
        return this;
    }

    public BooleanColumnBuilder<S> editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public BooleanColumnBuilder<S> visible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    public BooleanColumnBuilder<S> width(int width) {
        this.width = width;
        return this;
    }

    public BooleanColumnBuilder<S> minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public BooleanColumnBuilder<S> maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public BooleanColumnBuilder<S> resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public BooleanColumnBuilder<S> fixedWidth(int width) {
        this.width = width;
        this.minWidth = width;
        this.maxWidth = width;
        this.resizable = false;
        return this;
    }

    /** 값 변경 시 호출할 Consumer (예: viewModel::markAsDirty) */
    public BooleanColumnBuilder<S> dirtyConsumer(Consumer<S> dirtyConsumer) {
        this.dirtyConsumer = dirtyConsumer;
        return this;
    }

    public TableColumn<S, Boolean> build() {
        TableColumn<S, Boolean> column = new TableColumn<>(title);

        if (width > 0) column.setPrefWidth(width);
        if (minWidth > 0) column.setMinWidth(minWidth);
        if (maxWidth > 0) column.setMaxWidth(maxWidth);
        column.setResizable(resizable);

        column.setSortable(true);
        column.setVisible(isVisible);
        column.setEditable(editable);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);

        // CheckBoxCell 적용 (체크박스 컬럼이 올바르게 렌더링되도록 지정)
        column.setCellFactory(CheckBoxTableCell.forTableColumn(column));

        column.setCellValueFactory(cellData -> {
            S rowItem = cellData.getValue();
            BooleanProperty prop = propertyGetter.apply(rowItem);
            
            if (editable && prop != null) {
                // 기존 리스너 중복 방지를 위해 기존 리스너가 있다면 관리하거나 
                // TableView 패턴에 맞춰 값 변경 이벤트 연결
                prop.removeListener(this::handlePropertyChange); // 안전한 관리를 위해 분리 가능
                prop.addListener(this::handlePropertyChange);
            }
            return prop;
        });

        if (tableView != null) {
            tableView.getColumns().add(column);
            // TableView 전체 편집 활성화 필요 시 대비
            tableView.setEditable(true);
        }

        return column;
    }

    private void handlePropertyChange(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
        // 필요 시 추가 처리 공통화 지점
    }
}