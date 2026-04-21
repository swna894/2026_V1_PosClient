package com.swna.javafx.common.tracking;

import java.util.HashSet;
import java.util.Set;

public class DirtyTracker<T> {

    private final Set<T> dirtySet = new HashSet<>();

    public void markDirty(T item) {
        dirtySet.add(item);
    }

    public boolean contains(T item) {
        return dirtySet.contains(item);
    }

    public Set<T> getDirtyItems() {
        return dirtySet;
    }

    public void clear() {
        dirtySet.clear();
    }
}