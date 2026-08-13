package com.swna.javafx.common.ui.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import com.swna.javafx.common.ui.table.TableColumnUtil.DirtyConsumer;

public class DatePickerTableCell<T> extends TableCell<T, LocalDateTime> {

    private final DatePicker datePicker = new DatePicker();

    private final BiConsumer<T, LocalDateTime> setter;
    private final DirtyConsumer<T> dirtyConsumer;

    public DatePickerTableCell(
            BiConsumer<T, LocalDateTime> setter,
            DirtyConsumer<T> dirtyConsumer
    ) {
        this.setter = setter;
        this.dirtyConsumer = dirtyConsumer;

        // 날짜 선택 시 commit
        datePicker.setOnAction(e -> commitEdit(toLocalDateTime(datePicker.getValue())));
    }

    @Override
    protected void updateItem(LocalDateTime item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
        } else {
            setText(item.toLocalDate().toString());
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (!isEmpty()) {
            datePicker.setValue(getItem().toLocalDate());
            setGraphic(datePicker);
            setText(null);
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem().toLocalDate().toString());
        setGraphic(null);
    }

    @Override
    public void commitEdit(LocalDateTime newValue) {
        super.commitEdit(newValue);

        T row = getTableView().getItems().get(getIndex());

        setter.accept(row, newValue);

        if (dirtyConsumer != null) {
            dirtyConsumer.accept(row);
        }

        setGraphic(null);
        setText(newValue.toLocalDate().toString());
    }

    private LocalDateTime toLocalDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }
}
