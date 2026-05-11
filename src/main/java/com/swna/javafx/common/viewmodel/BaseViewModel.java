package com.swna.javafx.common.viewmodel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.swna.javafx.common.exception.ApiException;
import com.swna.javafx.common.exception.ErrorPolicy;
import com.swna.javafx.common.exception.ErrorPolicyResolver;
import com.swna.javafx.common.exception.NetworkException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public abstract class BaseViewModel {

    // ================= UI STATE =================
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty errorProperty() {
        return error;
    }

    // ================= STATE CONTROL =================
    protected void setLoading(boolean value) {
        loading.set(value);
    }

    protected void setError(String message) {
        error.set(message);
    }

    protected void clearError() {
        error.set(null);
    }

    // ================= THREAD EXECUTOR =================
    private final ExecutorService executor =
            Executors.newCachedThreadPool();

    // ================= ERROR HANDLING CORE =================
    protected void handleError(Throwable ex) {

        if (ex == null) {
            setError("Unknown error");
            return;
        }

        // 🔥 Network layer
        if (ex instanceof NetworkException) {
            setError("Cannot connect to the server.");
            return;
        }

        // 🔥 API layer (code 기반)
        if (ex instanceof ApiException apiEx) {

            ErrorPolicy policy = ErrorPolicyResolver.resolve(apiEx.getCode());

            setError(policy.message());

            return;
        }

        // 🔥 fallback
        setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error");
    }

    // ================= ASYNC EXECUTION =================

    // 기본형
    protected <T> void runAsync(
            Supplier<T> supplier,
            Consumer<T> onSuccess
    ) {
        runAsync(supplier, onSuccess, null);
    }

    // 확장형 (error handler 포함)
    protected <T> void runAsync(
            Supplier<T> supplier,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {

        setLoading(true);
        clearError();

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                setLoading(false);
                onSuccess.accept(task.getValue());
            });
        });

        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            Platform.runLater(() -> {
                setLoading(false);

                if (onError != null) {
                    onError.accept(ex);
                } else {
                    handleError(ex);
                }
            });
        });

        executor.execute(task);
    }
}