package com.swna.javafx.common.tableutils;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * 자유 입력이 가능하고, 입력한 값이 옵션 목록에 없으면 자동으로 추가되는
 * 편집형 ComboBox 컬럼을 만들기 위한 빌더입니다.
 * (기존 TableViewColumnlUtil#createComboBoxEditableColumn 로직을 정적 유틸로 이관)
 */
public class EditableComboBoxColumnBuilder<S> {

    private final TableView<S> tableView;
    private final String title;
    private final Function<S, ObservableValue<String>> propertyGetter;
    private final ObservableList<String> options;

    private BiConsumer<S, String> setter;
    private boolean visible = true;
    private String alignment = "";
    private Integer width;

    EditableComboBoxColumnBuilder(TableView<S> tableView,
            String title,
            Function<S, ObservableValue<String>> propertyGetter,
            ObservableList<String> options) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
        this.options = options;
    }

    public EditableComboBoxColumnBuilder<S> setter(BiConsumer<S, String> setter) {
        this.setter = setter;
        return this;
    }

    public EditableComboBoxColumnBuilder<S> visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public EditableComboBoxColumnBuilder<S> alignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    public EditableComboBoxColumnBuilder<S> width(int width) {
        this.width = width;
        return this;
    }

    public TableColumn<S, String> build() {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setVisible(visible);
        tableView.getColumns().add(column);

        // 값 표시
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));

        // commit → 모델 반영
        column.setOnEditCommit(event -> {
            S rowItem = event.getRowValue();
            String newValue = event.getNewValue();
            if (newValue == null || setter == null) return;
            setter.accept(rowItem, newValue);
        });

        column.setCellFactory(col -> new TableCell<S, String>() {

            private final ComboBox<String> comboBox = new ComboBox<>(options);
            private boolean clearOnEdit = false;
            private String originalValue;

            {
                comboBox.setEditable(true);

                // Enter / 선택 → commit
                comboBox.setOnAction(e -> {
                    String value = comboBox.getEditor().getText();
                    if (value == null) return;

                    value = value.trim();
                    if (!options.contains(value)) {
                        options.add(value);
                    }
                    commitEdit(value);
                });

                // 포커스 아웃 → commit
                comboBox.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV && isEditing()) {
                        comboBox.fireEvent(new ActionEvent());
                    }
                });

                // ESC → cancel + 복원
                comboBox.getEditor().setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        comboBox.getEditor().setText(originalValue);
                        cancelEdit();
                        e.consume();
                    }
                });

                setOnMouseClicked(e -> {
                    if (isEmpty()) return;
                    if (e.getClickCount() == 2 && isEditing()) {
                        javafx.application.Platform.runLater(() -> comboBox.getEditor().clear());
                    } else if (e.getClickCount() == 1 && !isEditing()) {
                        clearOnEdit = false;
                        startEdit();
                    }
                });
            }

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    originalValue = getItem();
                    super.startEdit();
                    setText(null);
                    setGraphic(comboBox);
                    comboBox.setValue(getItem());

                    if (clearOnEdit) {
                        comboBox.getEditor().clear();
                    }

                    javafx.application.Platform.runLater(() -> {
                        TextField editor = comboBox.getEditor();
                        editor.requestFocus();
                        editor.positionCaret(editor.getText().length());
                    });
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(originalValue);
                setGraphic(null);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    comboBox.setValue(item);
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        });

        applyAlignmentAndWidth(column);
        tableView.setEditable(true);
        return column;
    }

    private void applyAlignmentAndWidth(TableColumn<S, String> column) {
        if (width == null) {
            column.setMaxWidth(Double.MAX_VALUE);
        } else if (width != 0) {
            column.setPrefWidth(width);
        }
        column.setStyle(TableColumnUtils.getAlignmentStyle(alignment));
    }
}
