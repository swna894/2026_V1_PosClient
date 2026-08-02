package com.swna.javafx.admin.shop.viewmodel;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.api.ShopApiClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ShopViewModel {

    private final ShopApiClient shopService;

    // 클라이언트 측 메모리 캐시
    private Shop cachedShop;
    private boolean isLoading = false;
    private Mono<Shop> loadingMono = null;

    public ShopViewModel(ShopApiClient shopService) {
        this.shopService = shopService;
    }

    /**
     * 앱 초기화 시 호출하여 데이터를 로컬 캐시에 저장
     */
    public void loadInitialData() {
        if (this.cachedShop != null || isLoading) {
            log.debug("Shop already loaded or loading in progress");
            return;
        }

        isLoading = true;
        log.info("Loading shop information...");

        fetchShopAndCache()
            .subscribe(
                shop -> {
                    this.cachedShop = shop;
                    isLoading = false;
                    log.info("Shop information cached successfully: {}", shop.getName());
                },
                error -> {
                    isLoading = false;
                    log.error("Failed to load shop information: {}", error.getMessage());
                }
        );
    }

    /**
     * Shop 정보를 Mono로 반환 (캐시 우선)
     */
    public Mono<Shop> getShop() {
        if (cachedShop != null) {
            return Mono.just(cachedShop);
        }
        
        if (loadingMono != null) {
            return loadingMono;
        }
        
        loadingMono = fetchShopAndCache()
            .doOnSuccess(shop -> {
                this.cachedShop = shop;
                loadingMono = null;
            })
            .doOnError(error -> loadingMono = null)
            .cache();
        
        return loadingMono;
    }

    /**
     * 서버에서 Shop 정보를 가져와 캐시에 저장
     */
    private Mono<Shop> fetchShopAndCache() {
        return shopService.fetchShopInfo()
            .flatMap(response -> {
                if (response != null && response.isSuccess() && response.hasData()) {
                    return Mono.just(response.data());
                } else {
                    String message = response != null ? response.message() : "Unknown error";
                    return Mono.error(new RuntimeException("Failed to load shop: " + message));
                }
            });
    }

    /**
     * 동기적으로 Shop 정보 가져오기 (블로킹 - 주의해서 사용)
     */
    public Shop getShopBlocking() {
        if (cachedShop != null) {
            return cachedShop;
        }
        
        try {
            Shop shop = fetchShopAndCache().block();
            if (shop != null) {
                this.cachedShop = shop;
                return shop;
            }
        } catch (Exception e) {
            log.error("Error blocking loading shop: {}", e.getMessage());
        }
        
        return createDefaultShop();
    }

    /**
     * 캐싱된 정보 반환 (영수증 출력 시 사용)
     */
    public Shop getCachedShop() {
        return this.cachedShop;
    }
    
    /**
     * 기본 Shop 생성 (API 실패 시 사용)
     */
    private Shop createDefaultShop() {
        log.debug("Creating default shop using factory method");
        return Shop.create(
            "My Store",
            "Store Address",
            "000-0000-0000",
            "000-00-00000",
            "company",
            "email",
            "000-0000-0000"
        );
    }
}