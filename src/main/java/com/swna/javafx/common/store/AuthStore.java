package com.swna.javafx.common.store;

import org.springframework.stereotype.Component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Component
public class AuthStore {

    // 💡 login 모드 구분용 Enum
    public enum AppMode {  POS, ADMIN }

    private final ObjectProperty<AuthState> authState = new SimpleObjectProperty<>(AuthState.UNAUTHENTICATED);
    // 💡 login 현재 선택된 모드 관리 프로퍼티
    private final ObjectProperty<AppMode> selectedMode = new SimpleObjectProperty<>(AppMode.POS);
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>();

    public ObjectProperty<AuthState> authStateProperty() { return authState; }
    public AuthState getAuthState() { return authState.get(); }
    public void setAuthState(AuthState state) { authState.set(state);}

    public ObjectProperty<Role> roleProperty() { return role;}
    public Role getRole() { return role.get(); }
    public void setRole(Role role) { this.role.set(role); }

    public ObjectProperty<AppMode> selectedModeProperty() { return selectedMode; }
    public AppMode getSelectedMode() { return selectedMode.get(); }
    public void setSelectedMode(AppMode mode) { this.selectedMode.set(mode); }

    public void setAuthenticated(Role role) {
        this.authState.set(AuthState.AUTHENTICATED);
        this.role.set(role);
    }

    public void logout() {
        this.authState.set(AuthState.UNAUTHENTICATED);
        this.role.set(null);
    }

    public boolean isLoggedIn() {
        return authState.get() == AuthState.AUTHENTICATED;
    }

    public boolean isAdmin() {
        return role.get() == Role.ADMIN;
    }
    public boolean isManager() {
        return role.get() == Role.MANAGER;
    }
}
