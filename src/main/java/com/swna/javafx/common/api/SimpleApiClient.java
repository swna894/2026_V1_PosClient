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
public class SimpleApiClient {

    private final WebClient webClient;

    /* =========================================================
     * GET
     * ========================================================= */

    public <T> Mono<T> get(String url, @NonNull Class<T> responseClass) {
        return sendRequest(HttpMethod.GET, url, null, responseClass);
    }

    public <T> Mono<T> get(String url, @NonNull ParameterizedTypeReference<T> responseTypeRef) {
        return sendRequest(HttpMethod.GET, url, null, responseTypeRef);
    }

    public <T> Flux<T> getFlux(String url, @NonNull Class<T> elementClass) {
        return sendStreamRequest(HttpMethod.GET, url, null, elementClass);
    }

    public <T> Flux<T> getFlux(String url, Map<String, Object> params, @NonNull Class<T> elementClass) {
        return sendStreamRequest(HttpMethod.GET, buildUrlWithParams(url, params), null, elementClass);
    }

    /* =========================================================
     * POST
     * ========================================================= */

    public <T> Mono<T> post(String url, @Nullable Object requestBody, @NonNull Class<T> responseClass) {
        return sendRequest(HttpMethod.POST, url, requestBody, responseClass);
    }

    public <T> Mono<T> post(String url, @Nullable Object requestBody, @NonNull ParameterizedTypeReference<T> responseTypeRef) {
        return sendRequest(HttpMethod.POST, url, requestBody, responseTypeRef);
    }

    /* =========================================================
     * PUT
     * ========================================================= */

    public <T> Mono<T> put(String url, @Nullable Object requestBody, @NonNull Class<T> responseClass) {
        return sendRequest(HttpMethod.PUT, url, requestBody, responseClass);
    }

    public <T> Mono<T> put(String url, @Nullable Object requestBody, @NonNull ParameterizedTypeReference<T> responseTypeRef) {
        return sendRequest(HttpMethod.PUT, url, requestBody, responseTypeRef);
    }

    /* =========================================================
     * DELETE
     * ========================================================= */

    public <T> Mono<T> delete(String url, Object body, @NonNull Class<T> responseClass) {
        return sendRequest(HttpMethod.DELETE, url, body, responseClass);
    }

    public <T> Mono<T> delete(String url, @NonNull ParameterizedTypeReference<T> responseTypeRef) {
        return sendRequest(HttpMethod.DELETE, url, null, responseTypeRef);
    }

    // 🔥 [추가] Request Body와 ParameterizedTypeReference를 함께 처리하는 오버로딩 메서드
    public <T> Mono<T> delete(String url, @Nullable Object body, @NonNull ParameterizedTypeReference<T> responseTypeRef) {
        return sendRequest(HttpMethod.DELETE, url, body, responseTypeRef);
    }

    /* =========================================================
     * COMMON EXCHANGE (Internal)
     * ========================================================= */

    /**
     * Mono 기반의 공통 실행 메서드 (Class용)
     */
    private <T> Mono<T> sendRequest(HttpMethod method, String url, @Nullable Object requestBody, Class<T> responseClass) {
        return createRequestSpec(method, url, requestBody)
                .retrieve()
                .bodyToMono(responseClass);
    }

    /**
     * Mono 기반의 공통 실행 메서드 (ParameterizedTypeReference용)
     */
    private <T> Mono<T> sendRequest(HttpMethod method, String url, @Nullable Object requestBody, ParameterizedTypeReference<T> responseTypeRef) {
        return createRequestSpec(method, url, requestBody)
                .retrieve()
                .bodyToMono(responseTypeRef);
    }

    /**
     * Flux 기반의 공통 실행 메서드
     */
    private <T> Flux<T> sendStreamRequest(HttpMethod method, String url, @Nullable Object requestBody, Class<T> elementClass) {
        return createRequestSpec(method, url, requestBody)
                .retrieve()
                .bodyToFlux(elementClass);
    }

    /**
     * Request Spec 설정을 위한 공통 로직
     */
    private WebClient.RequestBodySpec createRequestSpec(HttpMethod method, String url, @Nullable Object requestBody) {
        WebClient.RequestBodySpec spec = webClient.method(method).uri(url);
        if (requestBody != null) {
            spec.bodyValue(requestBody);
        }
        return spec;
    }

    /* =========================================================
     * URL BUILDER
     * ========================================================= */

    private String buildUrlWithParams(@NonNull String baseUri, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return baseUri;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(baseUri);
        params.forEach(builder::queryParam);
        return builder.toUriString();
    }
}