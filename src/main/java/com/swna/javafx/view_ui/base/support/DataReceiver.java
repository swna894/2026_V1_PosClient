package com.swna.javafx.view_ui.base.support;

public interface DataReceiver<T> {
    void onReceive(T data);

    Class<T> getType();
}
