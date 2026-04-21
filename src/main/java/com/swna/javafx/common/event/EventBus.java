package com.swna.javafx.common.event;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class EventBus {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    // ================= SUBSCRIBE =================
    public <T> void subscribe(Class<T> type, Consumer<T> handler) {

        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                 .add(handler);
    }

    // ================= PUBLISH =================
    public <T> void publish(T event) {

        List<Consumer<?>> handlers = listeners.get(event.getClass());
        if (handlers == null) return;

        for (Consumer<?> h : handlers) {

            @SuppressWarnings("unchecked")
            Consumer<T> handler = (Consumer<T>) h;

            javafx.application.Platform.runLater(() ->
                handler.accept(event)
            );
        }
    }
}
