package com.swna.javafx.service.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.dto.auth.TokenResponse;

import reactor.core.publisher.Mono;

@Service
public class ReAuthService {

   private final WebClient authWebClient;
    private final TokenStore tokenStore;

    public ReAuthService( @Qualifier("authWebClient") WebClient authWebClient, TokenStore tokenStore) {
        this.authWebClient = authWebClient;
        this.tokenStore = tokenStore;
    }

    public Mono<TokenResponse> refreshToken() {
        return authWebClient.post()
                .uri("/auth/refresh")
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnNext(token ->  tokenStore.save( token.accessToken(), token.refreshToken() ) );
    }
}
