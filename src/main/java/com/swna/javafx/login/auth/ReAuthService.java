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

    private final WebClient authWebClient;  // 🔥 필터 없는 WebClient 사용
    private final TokenStore tokenStore;

    private Mono<LoginResponse> refreshMono;

    public ReAuthService(@Qualifier("authWebClient") WebClient authWebClient, 
                         TokenStore tokenStore) {
        this.authWebClient = authWebClient;
        this.tokenStore = tokenStore;
    }

    /**
     * 토큰 재발급 (중복 요청 방지 적용)
     */
    public Mono<LoginResponse> refreshToken() {
        
        String currentRefreshToken = tokenStore.getRefreshToken();
        if (currentRefreshToken == null || currentRefreshToken.isEmpty()) {
            return Mono.error(new ApiException("401", "Refresh token not found"));
        }

        if (refreshMono != null) {
            return refreshMono;  // 🔥 이미 진행 중이면 공유
        }

        refreshMono = authWebClient.post()
                .uri("/auth/reissue")  // 🔥 AuthService와 동일한 엔드포인트
                .bodyValue(Map.of("refreshToken", currentRefreshToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {})
                .flatMap(res -> {
                    if (!res.success()) {
                        return Mono.error(new ApiException(res.code(), res.message()));
                    }
                    if (res.data() == null) {
                        return Mono.error(new ApiException("500", "Token response data is null"));
                    }
                    return Mono.just(res.data());
                })
                .doOnNext(token -> {
                    // 🔥 토큰 갱신
                    if (token.accessToken() != null && token.refreshToken() != null) {
                        tokenStore.save(token.accessToken(), token.refreshToken());
                    } else {
                        throw new ApiException("500", "Invalid token response");
                    }
                })
                .doOnError(error -> {
                    // 🔥 재인증 실패 시 토큰 초기화
                    tokenStore.clear();
                    refreshMono = null;
                })
                .doFinally(signal -> {
                    // 🔥 완료되면 초기화 (성공/실패 모두)
                    refreshMono = null;
                })
                .cache();

        return refreshMono;
    }

    /**
     * 현재 진행 중인 재인증이 있는지 확인
     */
    public boolean isRefreshing() {
        return refreshMono != null;
    }
}