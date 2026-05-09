package com.swna.javafx.admin.shop.service;

import org.springframework.stereotype.Service;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final CommonApiClient apiClient;
    private final ApiEndpointMapper endpointMapper;

    /**
     * 서버에서 매장 정보를 가져와 Mono<Shop>으로 반환
     */
    public Mono<ApiResponse<Shop>> fetchShopInfo() {
        // 1. 메타데이터 가져오기 (ApiResponse<Shop> 타입 명시)
        ApiEndpointMapper.DomainMetadata<ApiResponse<Shop>> metadata = endpointMapper.getMetadata("first_shop_get");

        return apiClient.getForResponse(metadata, null, null)
                    .doOnError(e -> log.error("API call failed: {}", e.getMessage()))
                    .onErrorResume(e -> Mono.just(ApiResponse.error(
                            ApiResponse.ERROR_CODE_NETWORK_ERROR, 
                            "Unable to connect to the server."
                    )));
    }
}