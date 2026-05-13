package com.swna.javafx.pos.service.config;

import org.springframework.stereotype.Service;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PosToggleService {
    // 기본값은 true (결제 활성)
    private final BooleanProperty posEnabled = new SimpleBooleanProperty(false);

    public boolean isPosEnabled() {
        return posEnabled.get();
    }

    public void setPosEnabled(boolean enabled) {
        posEnabled.set(enabled);
    }

    public BooleanProperty posEnabledProperty() {
        return posEnabled;
    }

    public void toggle() {
        setPosEnabled(!isPosEnabled());
    }
}