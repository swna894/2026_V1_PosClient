package com.swna.javafx.admin.unpacking.api;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.unpacking.dto.UnpackDto;
import com.swna.javafx.admin.unpacking.dto.UnpackItemDto;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpackApiClient {

    private final SimpleApiClient webClientCommon;

    // 서버 Controller 매핑 URL과 동일하게 설정
    private static final String API_UNPACK = "/api/unpack";
    private static final String API_UNPACK_ITEM = "/api/unpackItem";
    private static final String API_UNPACK_ITEMS = "/api/unpackItems";

    // ==========================================
    // 1. Unpack CRUD API
    // ==========================================

    /** C: Unpack 생성 (POST /api/unpack) */
    public Mono<ApiResponse<UnpackDto>> postUnpack(UnpackDto dto) {
        //log.info("[API] POST {} - body: {}", API_UNPACK, dto);
        return webClientCommon.post(API_UNPACK, dto, TypeReferences.single(UnpackDto.class));
    }

    /** R: Unpack 기간별 조회 (GET /api/unpack?start=...&end=...) */
    public Mono<ApiResponse<List<UnpackDto>>> getUnpacks(Map<String, Object> queryParams) {
        StringBuilder urlBuilder = new StringBuilder(API_UNPACK);
        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            queryParams.forEach((key, value) -> 
                urlBuilder.append(key).append("=").append(value != null ? value : "").append("&")
            );
            urlBuilder.setLength(urlBuilder.length() - 1);
        }
        String url = urlBuilder.toString();
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.list(UnpackDto.class));
    }

    /** U: Unpack 다건 수정/갱신 (PUT /api/unpack) */
    public Mono<ApiResponse<List<UnpackDto>>> updateUnpacks(List<UnpackDto> dtos) {
        log.info("[API] PUT {} - count: {}", API_UNPACK, dtos != null ? dtos.size() : 0);
        return webClientCommon.put(API_UNPACK, dtos, TypeReferences.list(UnpackDto.class));
    }

    /** D: Unpack 삭제 (DELETE /api/unpack) */
    public Mono<ApiResponse<Void>> deleteUnpacks(List<UnpackDto> dtos) {
        log.info("[API] DELETE {} - count: {}", API_UNPACK, dtos != null ? dtos.size() : 0);
        return webClientCommon.delete(API_UNPACK, dtos, TypeReferences.VOID_TYPE);
    }

    // ==========================================
    // 2. UnpackItem CRUD API
    // ==========================================

    /** U: UnpackItem 단건 수정 (PUT /api/unpackItem) */
    public Mono<ApiResponse<UnpackItemDto>> updateUnpackItem(UnpackItemDto dto) {
        log.info("[API] PUT {} - body: {}", API_UNPACK_ITEM, dto);
        return webClientCommon.put(API_UNPACK_ITEM, dto, TypeReferences.single(UnpackItemDto.class));
    }

    /** U: UnpackItem 다건 수정 및 재고 추가 (PUT /api/unpackItems) */
    public Mono<ApiResponse<List<UnpackItemDto>>> updateUnpackItems(List<UnpackItemDto> dtos) {
        log.info("[API] PUT {} - count: {}", API_UNPACK_ITEMS, dtos != null ? dtos.size() : 0);
        return webClientCommon.put(API_UNPACK_ITEMS, dtos, TypeReferences.list(UnpackItemDto.class));
    }
}