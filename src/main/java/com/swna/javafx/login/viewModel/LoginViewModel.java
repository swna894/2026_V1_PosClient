package com.swna.javafx.login.viewModel;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.exception.ApiException;
import com.swna.javafx.common.exception.ErrorHandler;
import com.swna.javafx.common.exception.ErrorPolicy;
import com.swna.javafx.common.exception.ErrorPolicyResolver;
import com.swna.javafx.common.exception.NetworkException;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.common.store.AuthState;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.common.store.Role;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.common.viewmodel.BaseViewModel;
import com.swna.javafx.login.auth.AuthService;
import com.swna.javafx.login.dto.LoginResponse;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Component
public class LoginViewModel extends BaseViewModel {

    private final AuthService authService;
    private final AuthStore authStore;
    private final TokenStore tokenStore;

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final BooleanProperty isPosMode = new SimpleBooleanProperty(true);
    public BooleanProperty isPosModeProperty() { return isPosMode;}

    private static final String LOGIN_FAILED = "Login Failed";
    private static final String LOGIN_SUCCESS = "Login Success";
    private static final String SERVER_ERROR = "Server is not responding.";
    private static final String UNKNOWN_ERROR = "Unknown error.";
    private static final String NO_RESPONSE_SERVER = "No response from the server.";

    public LoginViewModel(AuthService authService,
                          AuthStore authStore,
                          TokenStore tokenStore) {

        this.authService = authService;
        this.authStore = authStore;
        this.tokenStore = tokenStore;

        email.addListener((o, a, b) -> clearMessages());
        password.addListener((o, a, b) -> clearMessages());
 
        // TODO:   배포시 변경 필요
        email.set("admin@gmail.com");
        password.set("1234");
    }

    // =========================
    // MAIN FLOW (Complexity LOW)
    // =========================
    public void login() {

        setError(null);

        if (!validateInput()) return;

        status.set("Logging in...");

        runAsync(  this::requestLogin, this::handleSuccess,  this::handleError  );
    }

    // =========================
    // API CALL
    // =========================
    private ApiResponse<LoginResponse> requestLogin() {
        return authService.login(email.get(), password.get()).block();
    }

    // =========================
    // SUCCESS FLOW
    // =========================
    private Void handleSuccess(ApiResponse<LoginResponse> response) {

        if (response == null) {
            fail(NO_RESPONSE_SERVER);
            return null;
        }

        if (!response.success()) {
            handleBusinessError(response);
            fail(LOGIN_FAILED);
            return null;
        }

        applyLoginSuccess(response.data());

        status.set(LOGIN_SUCCESS);
        // 💡 선택된 라디오 버튼에 맞게 AuthStore 모드 설정
        if (isPosMode.get()) {
            authStore.setSelectedMode(AuthStore.AppMode.POS);
        } else {
            authStore.setSelectedMode(AuthStore.AppMode.ADMIN);
        }

        authStore.setAuthState(AuthState.AUTHENTICATED);

        return null;
    }

    private void applyLoginSuccess(LoginResponse res) {

        tokenStore.save(res.accessToken(), res.refreshToken());

        authStore.setAuthenticated(mapRole(res.role()));
    }

    private void handleBusinessError(ApiResponse<LoginResponse> response) {

        ErrorPolicy policy = ErrorHandler.resolve(response);

        setError(policy.message());

        if (policy.logout()) {
            authStore.logout();
        }
    }

    // =========================
    // ERROR FLOW
    // =========================
    @Override
    protected void handleError(Throwable error) {

        if (error instanceof NetworkException) {
            fail(SERVER_ERROR);
            return;
        }

        if (error instanceof ApiException apiEx) {
            ErrorPolicy policy = ErrorPolicyResolver.resolve(apiEx.getCode());
            setError(policy.message());

            if (policy.logout()) { authStore.logout(); }
            fail(LOGIN_FAILED);
            return;
        }

        fail(UNKNOWN_ERROR);
    }

    // =========================
    // VALIDATION
    // =========================
    private boolean validateInput() {

        if (isBlank(email.get())) {
            return failBool("Email is required.");
        }

        if (isBlank(password.get())) {
            return failBool("Password is required.");
        }

        return true;
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    // =========================
    // COMMON FAIL HANDLERS
    // =========================
    private boolean failBool(String message) {
        setError(message);
        status.set(null);
        return false;
    }

    private void fail(String message) {
        setError(message);
        status.set(LOGIN_FAILED);
    }

    // =========================
    // ROLE MAPPING
    // =========================
    private Role mapRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (Exception e) {
            return Role.USER;
        }
    }

    // =========================
    // CLEANUP
    // =========================
    private void clearMessages() {
        setError(null);
        status.set("");
    }

    // =========================
    // GETTERS
    // =========================
    public StringProperty emailProperty() { return email; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty statusProperty() { return status; }

}