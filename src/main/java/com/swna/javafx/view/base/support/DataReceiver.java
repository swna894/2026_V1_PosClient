package com.swna.javafx.view.base.support;

public interface DataReceiver<T> {
    void onReceive(T data);

    Class<T> getType();
}
