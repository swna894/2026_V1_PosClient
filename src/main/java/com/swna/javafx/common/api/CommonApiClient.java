package com.swna.javafx.common.api;

import java.net.URI;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonApiClient {
    private final WebClient webClient;

    /**
     * 1. 단일 객체 요청 (Mono)
     */
    @SuppressWarnings("unchecked") // 런타임 타입 캐스팅 경고 억제
    public <T> Mono<T> requestMono(
            ApiEndpointMapper.DomainMetadata metadata, 
            Map<String, Object> pathVars, 
            Map<String, Object> queryParams) {

        return webClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .retrieve()
                .bodyToMono((ParameterizedTypeReference<T>) metadata.typeRef())
                .doOnError(e -> log.error("[API Mono Error] Path: {}, Msg: {}", metadata.path(), e.getMessage()));
    }

    /**
     * 2. 스트림/배열 요청 (Flux)
     * Class<T> 캐스팅 경고를 피하기 위해 ParameterizedTypeReference를 그대로 활용합니다.
     */
    @SuppressWarnings("unchecked")
    public <T> Flux<T> requestFlux(
            ApiEndpointMapper.DomainMetadata metadata, 
            Map<String, Object> pathVars, 
            Map<String, Object> queryParams) {

        return webClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .retrieve()
                // bodyToFlux에도 ParameterizedTypeReference를 직접 사용할 수 있습니다.
                .bodyToFlux((ParameterizedTypeReference<T>) metadata.typeRef())
                .doOnError(e -> log.error("[API Flux Error] Path: {}, Msg: {}", metadata.path(), e.getMessage()));
    }

    private URI buildUri(UriBuilder uriBuilder, String path, Map<String, Object> pathVars, Map<String, Object> queryParams) {
        uriBuilder.path(path);
        if (queryParams != null) {
            queryParams.forEach(uriBuilder::queryParam);
        }
        return uriBuilder.build(pathVars != null ? pathVars : Map.of());
    }
}