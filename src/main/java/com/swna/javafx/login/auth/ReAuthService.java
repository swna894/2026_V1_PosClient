package com.swna.javafx.login.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.common.exception.ApiException;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.login.dto.LoginResponse;

import reactor.core.publisher.Mono;

@Service
public class ReAuthService {

    private final WebClient authWebClient;
    private final TokenStore tokenStore;

    private Mono<LoginResponse> refreshMono;

    public ReAuthService( @Qualifier("authWebClient") WebClient authWebClient, TokenStore tokenStore) {
        this.authWebClient = authWebClient;
        this.tokenStore = tokenStore;
    }

    public Mono<LoginResponse> refreshToken() {

        if (refreshMono != null) {
            return refreshMono; // 🔥 이미 진행 중이면 공유
        }

        refreshMono = authWebClient.post()
                .uri("/auth/refresh")
                .bodyValue(Map.of("refreshToken", tokenStore.getRefreshToken()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {})
                .flatMap(res -> {
                    if (!res.success())  return Mono.error(new ApiException(res.code(), res.message()));
                    return Mono.just(res.data());
                })
                .doOnNext(token -> 
                    // 🔥 토큰 갱신
                    tokenStore.save(token.accessToken(), token.refreshToken())
                )
                .doFinally(signal -> 
                    // 🔥 끝나면 초기화
                    refreshMono = null
                )
                .cache(); // 🔥 핵심

        return refreshMono;
    }

}
