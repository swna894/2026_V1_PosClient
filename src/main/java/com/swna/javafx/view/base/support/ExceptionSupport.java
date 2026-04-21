package com.swna.javafx.view.base.support;

public interface ExceptionSupport {

    AlertSupport getAlertSupport();

    default void handleException(Throwable e) {
        getAlertSupport().showError(e.getMessage());
    }
}
