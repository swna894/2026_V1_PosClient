package com.swna.javafx.admin.shop.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.dto.Shop;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Shop API 클라이언트.
 * UserApiClient와 동일하게 모든 메서드가 Mono<ApiResponse<T>>를 그대로 반환한다.
 * 응답 검증(isSuccess/hasData)은 호출하는 쪽(ViewModel 등)에서 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopApiClient {

    private final SimpleApiClient webClientCommon;

    private static final String API_SHOPS = "/shops";
    private static final String API_SHOP_FIRST = API_SHOPS + "/first";

    // ==========================================
    // Create
    // ==========================================

    /**
     * 매장 신규 등록
     */
    public Mono<ApiResponse<Long>> createShop(Shop request) {
        log.info("[API] POST {} - body: {}", API_SHOPS, request);
        return webClientCommon.post(API_SHOPS, request, TypeReferences.single(Long.class));
    }

    // ==========================================
    // Read
    // ==========================================

    /**
     * 첫 번째 매장 조회
     */
    public Mono<ApiResponse<Shop>> getFirstShop() {
        log.info("[API] GET {}", API_SHOP_FIRST);
        return webClientCommon.get(API_SHOP_FIRST, TypeReferences.single(Shop.class));
    }

    /**
     * 매장 단건 조회 (ID)
     */
    public Mono<ApiResponse<Shop>> getShopById(Long id) {
        String url = API_SHOPS + "/" + id;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.single(Shop.class));
    }

    /**
     * 전체 매장 목록 조회
     */
    public Mono<ApiResponse<List<Shop>>> getAllShops() {
        log.info("[API] GET {}", API_SHOPS);
        return webClientCommon.get(API_SHOPS, TypeReferences.list(Shop.class));
    }

    // ==========================================
    // Update
    // ==========================================

    /**
     * 매장 정보 수정
     */
    public Mono<ApiResponse<Void>> updateShop(Long id, Shop request) {
        String url = API_SHOPS + "/" + id;
        log.info("[API] PUT {} - body: {}", url, request);
        return webClientCommon.put(url, request, TypeReferences.VOID_TYPE);
    }

    /**
     * 매장 활성/비활성 상태 변경
     */
    public Mono<ApiResponse<Void>> toggleShopStatus(Long id, boolean active) {
        String url = String.format("%s/%d/status?active=%b", API_SHOPS, id, active);
        log.info("[API] POST {}", url);
        return webClientCommon.post(url, null, TypeReferences.VOID_TYPE);
    }

    // ==========================================
    // Delete
    // ==========================================

    /**
     * 매장 삭제
     */
    public Mono<ApiResponse<Void>> deleteShop(Long id) {
        String url = API_SHOPS + "/" + id;
        log.info("[API] DELETE {}", url);
        return webClientCommon.delete(url, TypeReferences.VOID_TYPE);
    }
}
