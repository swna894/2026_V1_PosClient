package com.swna.javafx.viewmodel;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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


    protected <T> void runAsync(Supplier<T> supplier, Consumer<T> onSuccess) {

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return supplier.get();
            }
        };

        loading.set(true);

        task.setOnSucceeded(e -> {
            loading.set(false);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            loading.set(false);
            Throwable ex = task.getException();
            error.set(ex != null ? ex.getMessage() : "Unknown error");
        });

        new Thread(task).start();
    }
}