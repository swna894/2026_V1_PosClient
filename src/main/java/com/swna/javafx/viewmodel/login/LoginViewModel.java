package com.swna.javafx.viewmodel.login;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.store.AuthState;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.common.store.Role;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.dto.auth.LoginResponse;
import com.swna.javafx.service.auth.AuthService;
import com.swna.javafx.viewmodel.BaseViewModel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Component
public class LoginViewModel extends BaseViewModel {

    private final AuthService authService;
    private final AuthStore authStore;   
    private final TokenStore tokenStore; 

    // 입력 상태
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();

    // UI 상태
    private final StringProperty status = new SimpleStringProperty();

    public LoginViewModel(AuthService authService,
                          AuthStore authStore,
                          TokenStore tokenStore) {
        this.authService = authService;
        this.authStore = authStore;
        this.tokenStore = tokenStore;
    }

    public void login() {

        setError(null);
        status.set("Logging in...");

        runAsync(
                () -> {
                    LoginResponse res = authService.login(username.get(), password.get()).block();

                    String role = res.role();

                     switch (role) {
                        case "ADMIN" -> authStore.setAuthenticated(Role.ADMIN);
                        case "MANAGER" -> authStore.setAuthenticated(Role.MANAGER);
                        default -> authStore.setAuthenticated(Role.USER);
                    }
                    return null;
                },
                result -> {
                    status.set("Login Success");

                    // 🔥 핵심: 상태 변경
                    authStore.setAuthState(AuthState.AUTHENTICATED);
                }
        );
    }

    public void logout() {
        tokenStore.clear();
        authStore.logout();
    }

    // getter
    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty statusProperty() { return status; }
}