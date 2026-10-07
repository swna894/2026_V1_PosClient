package com.swna.javafx.admin.shop.api;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.shop.dto.Shop;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopApiClient {

    private final SimpleApiClient webClientCommon;
    
    // API Endpoint 상수
    private static final String API_SHOPS = "/shops";
    private static final String API_SHOP_FIRST = "/shops/first";

    /**
     * C: Create - 새로운 샵 정보를 서버에 등록하고 ApiResponse<Long> 반환
     */
    public Mono<ApiResponse<Long>> createShopInfo(Shop shop) {
        log.debug("[ShopApiClient] Creating shop: {}", shop);
        
        return webClientCommon.post(API_SHOPS, shop, TypeReferences.single(Long.class))
            .doOnError(e -> log.error("Create shop API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<Long> createShop(Shop shop) {
        return createShopInfo(shop)
            .flatMap(this::unwrapSingleResponse);
    }

    /**
     * R: Read (전체 목록) - 등록된 모든 샵 목록을 ApiResponse<List<Shop>>으로 반환
     */
    public Mono<ApiResponse<List<Shop>>> fetchAllShopsInfo() {
        log.debug("[ShopApiClient] Fetching all shops");
        
        return webClientCommon.get(API_SHOPS, TypeReferences.list(Shop.class))
            .doOnError(e -> log.error("Fetch all shops API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<List<Shop>> fetchAllShops() {
        return fetchAllShopsInfo()
            .flatMap(this::unwrapSingleResponse);
    }

    /**
     * 서버에서 첫 번째 매장 정보를 가져와 Mono<ApiResponse<Shop>>으로 반환
     */
    public Mono<ApiResponse<Shop>> fetchShopInfo() {
        log.debug("[ShopApiClient] Fetching shop info from: {}", API_SHOP_FIRST);
        
        return webClientCommon.get(API_SHOP_FIRST, TypeReferences.single(Shop.class))
            .doOnError(e -> log.error("Fetch first shop API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }
    
    /**
     * 서버에서 매장 정보를 가져와 Mono<Shop>으로 반환 (데이터만 필요할 때)
     */
    public Mono<Shop> fetchShop() {
        return fetchShopInfo()
            .flatMap(this::unwrapSingleResponse);
    }

    /**
     * R: Read (단건 ID 조회) - 특정 ID의 샵 정보를 조회
     */
    public Mono<ApiResponse<Shop>> fetchShopByIdInfo(Long id) {
        log.debug("[ShopApiClient] Fetching shop by id: {}", id);
        
        return webClientCommon.get(API_SHOPS + "/" + id, TypeReferences.single(Shop.class))
            .doOnError(e -> log.error("Fetch shop by id API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<Shop> fetchShopById(Long id) {
        return fetchShopByIdInfo(id)
            .flatMap(this::unwrapSingleResponse);
    }

    /**
     * U: Update - 특정 ID의 샵 정보를 수정
     */
    public Mono<ApiResponse<Void>> updateShopInfo(Long id, Shop shop) {
        log.debug("[ShopApiClient] Updating shop id: {}", id);
        
        return webClientCommon.put(API_SHOPS + "/" + id, shop, TypeReferences.single(Void.class))
            .doOnError(e -> log.error("Update shop API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<Void> updateShop(Long id, Shop shop) {
        return updateShopInfo(id, shop)
            .flatMap(this::unwrapVoidResponse);
    }

    /**
     * U: Status Toggle - 샵의 활성화/비활성화 상태 변경
     * (SimpleApiClient에 patch 메서드가 없을 경우를 대비해 본문 없는 요청 형태 혹은 put으로 대체 가능)
     */
    public Mono<ApiResponse<Void>> toggleShopStatusInfo(Long id, boolean active) {
        log.debug("[ShopApiClient] Toggling status for shop id: {}, active: {}", id, active);
        String url = String.format("%s/%d/status?active=%b", API_SHOPS, id, active);
        
        // 만약 SimpleApiClient에 patch가 없다면 put이나 다른 전송 방식을 확인해야 합니다.
        // 아래는 바디를 비우거나 빈 객체를 넘기는 일반적인 형태입니다.
        return webClientCommon.post(url, null, TypeReferences.single(Void.class))
            .doOnError(e -> log.error("Toggle shop status API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<Void> toggleShopStatus(Long id, boolean active) {
        return toggleShopStatusInfo(id, active)
            .flatMap(this::unwrapVoidResponse);
    }

    /**
     * D: Delete - 특정 ID의 샵 정보를 삭제
     */
    public Mono<ApiResponse<Void>> deleteShopInfo(Long id) {
        log.debug("[ShopApiClient] Deleting shop id: {}", id);
        
        return webClientCommon.delete(API_SHOPS + "/" + id, TypeReferences.single(Void.class))
            .doOnError(e -> log.error("Delete shop API call failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.just(ApiResponse.error(
                    ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                    "Unable to connect to the server."
            )));
    }

    public Mono<Void> deleteShop(Long id) {
        return deleteShopInfo(id)
            .flatMap(this::unwrapVoidResponse);
    }

    // =========================================================================
    // Helper Methods (Response Unwrapping)
    // =========================================================================

    private <T> Mono<T> unwrapSingleResponse(ApiResponse<T> response) {
        if (response != null && response.isSuccess() && response.hasData()) {
            log.debug("[ShopApiClient] Data fetched successfully: {}", response.data());
            return Mono.just(response.data());
        } else {
            String message = (response != null) ? response.message() : "Null response received";
            log.warn("[ShopApiClient] Failed to fetch data: {}", message);
            return Mono.empty();
        }
    }

    private Mono<Void> unwrapVoidResponse(ApiResponse<Void> response) {
        if (response != null && response.isSuccess()) {
            log.debug("[ShopApiClient] Void operation completed successfully");
            return Mono.empty();
        } else {
            String message = (response != null) ? response.message() : "Null response received";
            log.warn("[ShopApiClient] Void operation failed: {}", message);
            return Mono.empty();
        }
    }
}