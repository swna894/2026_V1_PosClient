package com.swna.javafx.config.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.login.auth.ReAuthService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 401 Unauthorized 발생 시 토큰을 재발급받고 요청을 재시도하는 필터
 */
@Slf4j
@Component
public class ReAuthWebClientFilter implements ExchangeFilterFunction {

    private final ReAuthService reAuthService;
    private final TokenStore tokenStore;

    public ReAuthWebClientFilter(ReAuthService reAuthService, TokenStore tokenStore) {
        this.reAuthService = reAuthService;
        this.tokenStore = tokenStore;
    }
   
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request)
                .flatMap(response -> {
                    // 1. 401 에러이고, 아직 재시도하지 않은 요청인지 확인
                    if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                        log.warn("Received 401. Attempting token refresh...");

                        return response.bodyToMono(Void.class) // 기존 바디 비우기
                                .then(reAuthService.refreshToken()) // ReAuthService 내부에서 save() 수행됨
                                .flatMap(newTokens -> {
                                    log.info("Token refreshed. Retrying original request... ");

                                // 2. 새 토큰으로 요청 복제
                                 ClientRequest newRequest = ClientRequest.from(request)
                                                        .headers(headers -> {
                                                            headers.remove("Authorization");
                                                            headers.setBearerAuth(newTokens.accessToken());
                                                        }).build();

                                    // 3. 재시도 수행
                                    return next.exchange(newRequest);
                                })
                                .onErrorResume(error -> {
                                    log.error("Critical: Token refresh failed - {}", error.getMessage());
                                    tokenStore.clear(); // 인증 정보 파기
                                    return Mono.just(response); // 원래의 401 응답 반환
                                });
                    }
                    return Mono.just(response);
                });
    }
}