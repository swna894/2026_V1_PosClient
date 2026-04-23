package com.swna.javafx.viewmodel;

import org.springframework.stereotype.Component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Component
public class NavigationViewModel {

    private final ObjectProperty<String> currentView = new SimpleObjectProperty<>();

    public ObjectProperty<String> currentViewProperty() {
        return currentView;
    }

    public void navigate(String viewName) {
        currentView.set(viewName);
    }
}
