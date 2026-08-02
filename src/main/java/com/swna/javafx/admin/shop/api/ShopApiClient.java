package com.swna.javafx.admin.shop.api;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.shop.Shop;
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
    private static final String API_SHOP_FIRST = "/shops/first";

    /**
     * 서버에서 매장 정보를 가져와 Mono<ApiResponse<Shop>>으로 반환
     */
    public Mono<ApiResponse<Shop>> fetchShopInfo() {
        log.debug("[ShopService] Fetching shop info from: {}", API_SHOP_FIRST);
        
        return webClientCommon.get(API_SHOP_FIRST, TypeReferences.single(Shop.class))
            .doOnError(e -> log.error("API call failed: {}", e.getMessage()))
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
     * 공통 ApiResponse 단일 객체 언래핑 헬퍼 메서드
     */
    private <T> Mono<T> unwrapSingleResponse(ApiResponse<T> response) {
        if (response != null && response.isSuccess() && response.hasData()) {
            log.debug("[ShopService] Shop fetched: {}", response.data());
            return Mono.just(response.data());
        } else {
            String message = (response != null) ? response.message() : "Null response received";
            log.warn("[ShopService] Failed to fetch shop: {}", message);
            return Mono.empty();
        }
    }
}