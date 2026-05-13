package com.swna.javafx.admin.shop.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.common.api.WebClientCommon;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final WebClientCommon webClientCommon;
    
    // API Endpoint 상수 직접 정의
    private static final String API_SHOP_FIRST = "/shops/first";
    
    // ParameterizedTypeReference 상수로 정의 (재사용)
    private static final ParameterizedTypeReference<ApiResponse<Shop>> SHOP_RESPONSE_TYPE = 
        new ParameterizedTypeReference<ApiResponse<Shop>>() {};

    /**
     * 서버에서 매장 정보를 가져와 Mono<ApiResponse<Shop>>으로 반환
     */
    public Mono<ApiResponse<Shop>> fetchShopInfo() {
        log.debug("[ShopService] Fetching shop info from: {}", API_SHOP_FIRST);
        
        return webClientCommon.get(API_SHOP_FIRST, SHOP_RESPONSE_TYPE)
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
            .flatMap(response -> {
                if (response.isSuccess() && response.hasData()) {
                    log.debug("[ShopService] Shop fetched: {}", response.data());
                    return Mono.just(response.data());
                } else {
                    log.warn("[ShopService] Failed to fetch shop: {}", response.message());
                    return Mono.empty();
                }
            });
    }
}