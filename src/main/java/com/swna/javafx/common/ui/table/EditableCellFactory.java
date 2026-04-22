package com.swna.javafx.common.ui.table;

import javafx.scene.control.*;
import javafx.util.Callback;

public class EditableCellFactory {

    private EditableCellFactory() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>>
    textCell() {

        return column -> new TableCell<>() {

            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (Boolean.FALSE.equals(newV)) {
                        commitEdit(textField.getText());
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textField.setText(getItem());
                setGraphic(textField);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        };
    }
}
