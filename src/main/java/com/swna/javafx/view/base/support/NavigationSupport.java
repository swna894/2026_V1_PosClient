package com.swna.javafx.view.base.support;

import com.swna.javafx.navigation.SceneManager;

public interface NavigationSupport {

    SceneManager getSceneManager();

    default void move(String fxml) {
        getSceneManager().switchScene(fxml);
    }

    default void move(String fxml, Object data) {
        getSceneManager().switchScene(fxml, data);
    }

    default void back() {
        getSceneManager().goBack();
    }
}
