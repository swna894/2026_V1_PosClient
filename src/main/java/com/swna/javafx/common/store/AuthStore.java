package com.swna.javafx.common.store;

import org.springframework.stereotype.Component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

@Component
public class AuthStore {

    private final ObjectProperty<AuthState> authState = new SimpleObjectProperty<>(AuthState.UNAUTHENTICATED);
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>();

    public ObjectProperty<AuthState> authStateProperty() { return authState; }
    public ObjectProperty<Role> roleProperty() { return role;}

    public void setAuthenticated(Role role) {
        this.authState.set(AuthState.AUTHENTICATED);
        this.role.set(role);
    }

    public AuthState getAuthState() { return authState.get(); }
    public void setAuthState(AuthState state) { authState.set(state);}

    public Role getRole() { return role.get(); }
    public void setRole(Role role) { this.role.set(role); }

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
