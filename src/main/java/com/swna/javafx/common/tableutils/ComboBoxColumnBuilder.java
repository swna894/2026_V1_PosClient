package com.swna.javafx.common.tableutils;

import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ComboBoxColumnBuilder<S> {

    // 필수 매개변수
    private final TableView<S> tableView;
    private final String title;
    private final Function<S, StringProperty> propertyGetter;
    private final Function<S, ObservableList<String>> itemsGetter;

    // 선택적 매개변수 (기본값 설정)
    private BiConsumer<S, String> setter = null;
    private boolean editable = true;
    private boolean isVisible = true;
    private String alignment = "CENTER";
    private int width = -1;

    public ComboBoxColumnBuilder(TableView<S> tableView, String title, 
                                 Function<S, StringProperty> propertyGetter,
                                 Function<S, ObservableList<String>> itemsGetter) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
        this.itemsGetter = itemsGetter;
    }

    public ComboBoxColumnBuilder<S> setter(BiConsumer<S, String> setter) {
        this.setter = setter;
        return this;
    }

    public ComboBoxColumnBuilder<S> editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public ComboBoxColumnBuilder<S> visible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    public ComboBoxColumnBuilder<S> alignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    public ComboBoxColumnBuilder<S> width(int width) {
        this.width = width;
        return this;
    }

    public TableColumn<S, String> build() {
        return com.swna.javafx.common.tableutils.TableUtil.makeComboBoxColumn(
                tableView,
                title,
                propertyGetter,
                itemsGetter,
                setter,
                editable,
                isVisible,
                alignment,
                width
        );
    }
}