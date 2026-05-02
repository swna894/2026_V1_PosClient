package com.swna.javafx.controller.base.support;

public interface DataReceiver<T> {
    void onReceive(T data);

    Class<T> getType();
}
