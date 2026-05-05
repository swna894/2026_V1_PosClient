package com.swna.javafx.login.auth;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.login.dto.LoginResponse;
import com.swna.javafx.login.dto.TokenResponse;

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
    public Mono<ApiResponse<LoginResponse>> login(String email, String password) {

        return webClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of(
                        "email", email,
                        "password", password
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {});
                
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
