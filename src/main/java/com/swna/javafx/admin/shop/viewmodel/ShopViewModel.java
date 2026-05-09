package com.swna.javafx.admin.shop.viewmodel;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.service.ShopService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopViewModel {

    private final ShopService shopService;

    // 클라이언트 측 메모리 캐시
    private Shop cachedShop;

    /**
     * 앱 초기화 시 호출하여 데이터를 로컬 캐시에 저장
     */

    public void loadInitialData() {
        if (this.cachedShop != null) return; 

        shopService.fetchShopInfo()
            .subscribe(
                response -> {
                    // 1. Check if API call was successful and contains data
                    if (response != null && response.isSuccess() && response.hasData()) {
                        this.cachedShop = response.data();
                        log.info("Shop information cached successfully: {}", this.cachedShop.getName());
                    } else {

                        String code = response != null ? response.code() : "UNKNOWN_ERROR";
                        String message = response != null ? response.message() : "No response from server";
                        log.warn("Failed to load shop information. Code: {}, Message: {}", code, message);
                    }
                },
                error -> {
                    // 3. Handle system-level errors (Network, Timeout, etc.)
                    log.error("System error occurred while loading shop data: {}", error.getMessage());
                }
            );
    }
    /**
     * 캐싱된 정보 반환 (영수증 출력 시 사용)
     */
    public Shop getCachedShop() {
        return this.cachedShop;
    }
}