// com.swna.javafx.pos.functional/TriConsumer.java
package com.swna.javafx.pos.functional;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
