package com.swna.javafx.viewmodel;

import javafx.beans.property.*;
import javafx.concurrent.Task;

public abstract class BaseViewModel {

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty errorProperty() {
        return error;
    }

    protected void setLoading(boolean value) {
        loading.set(value);
    }

    protected void setError(String message) {
        error.set(message);
    }


    protected <T> void runAsync(Task<T> task) {

        loading.set(true);

        task.setOnSucceeded(e -> loading.set(false));

        task.setOnFailed(e -> {
            loading.set(false);
            Throwable ex = task.getException();
            error.set(ex != null ? ex.getMessage() : "Unknown error");
        });

        new Thread(task).start();
    }
}