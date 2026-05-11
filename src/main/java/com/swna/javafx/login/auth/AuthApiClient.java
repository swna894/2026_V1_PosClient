package com.swna.javafx.login.auth;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.login.dto.TokenResponse;

import reactor.core.publisher.Mono;

@Service
public class AuthApiClient {

    private final WebClient webClient;

    public AuthApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<TokenResponse> refresh(String refreshToken) {

        return webClient.post()
                .uri("/auth/reissue")
                .bodyValue(Map.of("refreshToken", refreshToken))
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }
}