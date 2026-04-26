package com.swna.javafx.view_ui.base.support;

import javafx.concurrent.Task;

public interface AsyncSupport {

    default <T> void runAsync(Task<T> task) {
        new Thread(task).start();
    }

    default <T> void runWithCallback(Task<T> task,
                                     Runnable onSuccess,
                                     Runnable onFail) {

        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onFail.run());

        new Thread(task).start();
    }
}