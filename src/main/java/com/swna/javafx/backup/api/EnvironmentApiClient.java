package com.swna.javafx.backup.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.backup.domain.Environment;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 환경 설정 API 클라이언트
 * SupplierApiClient 참조 구조로 단순화 및 통일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentApiClient {

    private final SimpleApiClient webClientCommon;
    
    private static final String API_ENVIRONMENTS = "/environments";

    /**
     * [Create] 새로운 환경 설정 생성 (POST)
     */
    public Mono<ApiResponse<Environment>> createEnvironment(Environment environment) {
        log.info("[API] POST {} - body: {}", API_ENVIRONMENTS, environment);
        return webClientCommon.post(API_ENVIRONMENTS, environment, TypeReferences.single(Environment.class));
    }

    /**
     * [Read - 단건] 특정 ID의 환경 설정 조회 (GET)
     */
    public Mono<ApiResponse<Environment>> getEnvironmentById(Long id) {
        String url = API_ENVIRONMENTS + "/" + id;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.single(Environment.class));
    }

    /**
     * [Read - 목록] 전체 환경 설정 목록 조회 (GET)
     */
    public Mono<ApiResponse<List<Environment>>> getAllEnvironments() {
        log.info("[API] GET {}", API_ENVIRONMENTS);
        return webClientCommon.get(API_ENVIRONMENTS, TypeReferences.list(Environment.class));
    }

    /**
     * [Update] 특정 ID의 환경 설정 수정 (PUT)
     */
    public Mono<ApiResponse<Environment>> updateEnvironment(Long id, Environment environment) {
        String url = API_ENVIRONMENTS + "/" + id;
        log.info("[API] PUT {} - body: {}", url, environment);
        return webClientCommon.put(url, environment, TypeReferences.single(Environment.class));
    }

    /**
     * [Delete] 특정 ID의 환경 설정 삭제 (DELETE)
     */
    public Mono<ApiResponse<Void>> deleteEnvironment(Long id) {
        String url = API_ENVIRONMENTS + "/" + id;
        log.info("[API] DELETE {}", url);
        return webClientCommon.delete(url, TypeReferences.VOID_TYPE);
    }
}