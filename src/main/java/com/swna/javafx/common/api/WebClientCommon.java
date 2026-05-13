package com.swna.javafx.common.api;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 필터가 적용된 공통 WebClient를 사용하는 추상화 서비스
 */
@Service
@RequiredArgsConstructor
public class WebClientCommon {

    private final WebClient webClient;

    /* =========================================================
     * GET
     * ========================================================= */

    public <T> Mono<T> get(String url, @NonNull Class<T> responseType) {
        return exchangeMono(HttpMethod.GET, url, null, responseType);
    }

    public <T> Mono<T> get(String url, @NonNull ParameterizedTypeReference<T> typeRef) {
        return exchangeMono(HttpMethod.GET, url, null, typeRef);
    }

    public <T> Flux<T> getFlux(String url, @NonNull Class<T> elementType) {
        return exchangeFlux(HttpMethod.GET, url, null, elementType);
    }

    public <T> Flux<T> getFlux(String url, Map<String, Object> params, @NonNull Class<T> elementType) {
        return exchangeFlux(HttpMethod.GET, buildUrl(url, params), null, elementType);
    }

    /* =========================================================
     * POST / PUT / DELETE
     * ========================================================= */

    public <T> Mono<T> post(String url, @Nullable Object requestBody, @NonNull Class<T> responseType) {
        return exchangeMono(HttpMethod.POST, url, requestBody, responseType);
    }

    public <T> Mono<T> put(String url, @Nullable Object requestBody, @NonNull Class<T> responseType) {
        return exchangeMono(HttpMethod.PUT, url, requestBody, responseType);
    }

    public <T> Mono<T> delete(String url, @NonNull Class<T> responseType) {
        return exchangeMono(HttpMethod.DELETE, url, null, responseType);
    }

    /* =========================================================
     * COMMON EXCHANGE (Internal)
     * ========================================================= */

    /**
     * Mono 기반의 공통 실행 메서드 (Class용)
     */
    private <T> Mono<T> exchangeMono(HttpMethod method, String url, @Nullable Object requestBody, Class<T> responseType) {
        return prepareRequest(method, url, requestBody)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * Mono 기반의 공통 실행 메서드 (ParameterizedTypeReference용)
     */
    private <T> Mono<T> exchangeMono(HttpMethod method, String url, @Nullable Object requestBody, ParameterizedTypeReference<T> typeRef) {
        return prepareRequest(method, url, requestBody)
                .retrieve()
                .bodyToMono(typeRef);
    }

    /**
     * Flux 기반의 공통 실행 메서드
     */
    private <T> Flux<T> exchangeFlux(HttpMethod method, String url, @Nullable Object requestBody, Class<T> elementType) {
        return prepareRequest(method, url, requestBody)
                .retrieve()
                .bodyToFlux(elementType);
    }

    /**
     * Request Spec 설정을 위한 공통 로직
     */
    private WebClient.RequestBodySpec prepareRequest(HttpMethod method, String url, @Nullable Object requestBody) {
        WebClient.RequestBodySpec spec = webClient.method(method).uri(url);
        if (requestBody != null) {
            spec.bodyValue(requestBody);
        }
        return spec;
    }

    /* =========================================================
     * URL BUILDER
     * ========================================================= */

    private String buildUrl(@NonNull String path, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return path;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach(builder::queryParam);
        return builder.toUriString();
    }
}