package com.swna.javafx.common.ui.table;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableColumnUtil {

        // 🔥 인스턴스 생성 방지
    private TableColumnUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    // ========================
    // String
    // ========================
    public static <T> void makeStringColumn(
            TableColumn<T, String> column,
            Function<T, StringProperty> propertyGetter,
            BiConsumer<T, String> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer   // ⭐ 추가
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue())
        );

        if (editable) {

            column.setCellFactory(TextFieldTableCell.forTableColumn());

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();
                String newValue = event.getNewValue();

                setter.accept(row, newValue);

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);   // ⭐ Dirty 등록
                }
            });

        } else {
            column.setEditable(false);
        }
    }

    // ========================
    // Integer
    // ========================
    public static <T> void makeIntegerColumn(
            TableColumn<T, Integer> column,
            Function<T, IntegerProperty> propertyGetter,
            ObjIntConsumer<T> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue()).asObject()
        );

        if (editable) {

            StringConverter<Integer> converter = new IntegerStringConverter();

            column.setCellFactory(TextFieldTableCell.forTableColumn(converter));

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();

                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });

        } else {
            column.setEditable(false);
        }
    }

    public static <T> void makeReadOnlyIntegerColumn( TableColumn<T, Long> column, Function<T, LongProperty> propertyGetter) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject() );
        column.setEditable(false);
    }

    // ========================
    // Double
    // ========================
    public static <T> void makeDoubleColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            ObjDoubleConsumer<T> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue()).asObject()
        );

        if (editable) {

            StringConverter<Double> converter = new DoubleStringConverter();

            column.setCellFactory(TextFieldTableCell.forTableColumn(converter));

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();

                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });

        } else {
            column.setEditable(false);
        }
    }

    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            Consumer<T> dirtyConsumer
    ) {

        column.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(getter.apply(cell.getValue()))
        );

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString()); // 필요하면 formatter 적용
                }
            }
        });

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });
        }
    }

    // ========================
    // LocalDateTime
    // ========================
    public static <T> void makeDatePickerColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<T, LocalDateTime> setter,
            DirtyConsumer<T> dirtyConsumer
    ) {

        column.setCellValueFactory(cell ->
                getter.apply(cell.getValue())
        );

        column.setCellFactory(col ->
                new DatePickerTableCell<T>(setter, dirtyConsumer) // ✅ 핵심
        );

        column.setEditable(true);
    }

    // ========================
    // Boolean
    // ========================
    public static <T> void makeBooleanColumn(
            TableColumn<T, Boolean> column,
            Function<T, BooleanProperty> propertyGetter,
            BiConsumer<T, Boolean> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> {

            BooleanProperty prop = propertyGetter.apply(cellData.getValue());

            if (editable) {
                prop.addListener((obs, oldVal, newVal) -> {

                    setter.accept(cellData.getValue(), newVal);

                    if (dirtyConsumer != null) {
                        dirtyConsumer.accept(cellData.getValue());
                    }
                });
            }

            return prop;
        });

        column.setEditable(editable);
    }
}