package com.swna.javafx.common.ui.component;

import javafx.scene.control.Button;

public class CustomButton extends Button {

    public CustomButton(String text) {
        super(text);

        setStyle("""
            -fx-background-color: #2d89ef;
            -fx-text-fill: white;
            -fx-padding: 6 12;
        """);

        setOnMouseEntered(e ->
            setStyle("-fx-background-color: #1b5fbf; -fx-text-fill: white;")
        );

        setOnMouseExited(e ->
            setStyle("-fx-background-color: #2d89ef; -fx-text-fill: white;")
        );
    }

    public static CustomButton primary(String text) {
        return new CustomButton(text);
    }
}
