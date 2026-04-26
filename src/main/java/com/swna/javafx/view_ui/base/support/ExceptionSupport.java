package com.swna.javafx.view_ui.base.support;

public interface ExceptionSupport {

    AlertSupport getAlertSupport();

    default void handleException(Throwable e) {
        getAlertSupport().showError(e.getMessage());
    }
}
