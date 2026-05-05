package com.swna.javafx.pos.manager;

import javafx.animation.PauseTransition;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

@Component
public class UiNotifier {

    private PauseTransition clearTimer;

    public void setupAutoClear(StringProperty messageProperty) {
        clearTimer = new PauseTransition(Duration.minutes(3));
        clearTimer.setOnFinished(e -> messageProperty.set(""));

        messageProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                clearTimer.playFromStart();
            }
        });
    }

    public void showTemporary(StringProperty messageProperty, String message, Duration duration) {
        messageProperty.set(message);
        PauseTransition timer = new PauseTransition(duration);
        timer.setOnFinished(e -> {
            if (message.equals(messageProperty.get())) {
                messageProperty.set("");
            }
        });
        timer.play();
    }

    public void showTemporary(StringProperty messageProperty, String message) {
        showTemporary(messageProperty, message, Duration.seconds(3));
    }

    public void showError(StringProperty messageProperty, String errorMessage) {
        messageProperty.set("ERROR: " + errorMessage);
        PauseTransition timer = new PauseTransition(Duration.seconds(5));
        timer.setOnFinished(e -> {
            if (messageProperty.get() != null && messageProperty.get().startsWith("ERROR:")) {
                messageProperty.set("");
            }
        });
        timer.play();
    }
}
