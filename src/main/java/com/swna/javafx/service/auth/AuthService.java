package com.swna.javafx.service.auth;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.common.store.Role;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.dto.auth.LoginResponse;
import com.swna.javafx.dto.auth.TokenResponse;

import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final WebClient webClient;
    private final TokenStore tokenStore;
    private final AuthStore authStore;

    public AuthService(WebClient webClient,
                       TokenStore tokenStore,
                       AuthStore authStore) {
        this.webClient = webClient;
        this.tokenStore = tokenStore;
        this.authStore = authStore;
    }

    // LOGIN
    public Mono<LoginResponse> login(String username, String password) {

        return webClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of(
                        "username", username,
                        "password", password
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .map(res -> {

                    String access = res.get("accessToken");
                    String refresh = res.get("refreshToken");
                    String role = res.get("role");

                    tokenStore.save(access, refresh);
                    authStore.setAuthenticated(Role.valueOf(role));

                    return new LoginResponse(access, refresh, role);
                });
    }

    // REFRESH (🔥 reactive only)
    public Mono<TokenResponse> refreshToken(String refreshToken) {

        return webClient.post()
                .uri("/auth/refresh")
                .bodyValue(Map.of("refreshToken", refreshToken))
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    public void logout() {
        tokenStore.clear();
        authStore.logout();
    }
}
