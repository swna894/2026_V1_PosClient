package com.swna.javafx.admin.shop.viewmodel;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.api.ShopApiClient;
import com.swna.javafx.admin.shop.dto.Shop;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Shop 정보의 캐시 및 API 응답 처리를 담당한다.
 * ApiResponse 언래핑은 이 클래스에서만 수행하고, 컨트롤러는 Shop/에러만 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopViewModel {

    private final ShopApiClient shopApiClient;
    private final Shop defaultShop; // createDefaultShop() 호출용 빈

    // 클라이언트 측 메모리 캐시
    private volatile Shop cachedShop;
    private Mono<Shop> loadingMono;

    // ==========================================
    // Read
    // ==========================================

    /**
     * 앱 초기화 시 호출하여 데이터를 로컬 캐시에 저장
     */
    public void loadInitialData() {
        log.info("Loading shop information...");
        getShop().subscribe(
            shop -> log.info("Shop information cached successfully: {}", shop.getName()),
            error -> log.error("Failed to load shop information: {}", error.getMessage())
        );
    }

    /**
     * Shop 정보 반환 (캐시 우선, 동시 호출 시 요청 1회로 합침)
     */
    public synchronized Mono<Shop> getShop() {
        if (cachedShop != null) {
            return Mono.just(cachedShop);
        }
        if (loadingMono == null) {
            loadingMono = fetchAndCache()
                .doFinally(signal -> clearLoading())
                .cache();
        }
        return loadingMono;
    }

    /**
     * 캐시를 무시하고 서버에서 다시 조회 (RELOAD 버튼용)
     */
    public Mono<Shop> reload() {
        return fetchAndCache();
    }

    /**
     * 동기적으로 Shop 정보 가져오기 (블로킹 - 주의해서 사용)
     */
    public Shop getShopBlocking() {
        if (cachedShop != null) {
            return cachedShop;
        }
        try {
            Shop shop = fetchAndCache().block();
            if (shop != null) {
                return shop;
            }
        } catch (Exception e) {
            log.error("Error blocking loading shop: {}", e.getMessage());
        }
        return defaultShop.createDefaultShop();
    }

    /**
     * 캐싱된 정보 반환 (영수증 출력 시 사용)
     */
    public Shop getCachedShop() {
        return cachedShop;
    }

    // ==========================================
    // Update
    // ==========================================

    /**
     * 매장 정보 저장. 성공하면 캐시도 함께 갱신하고 저장된 Shop을 emit 한다.
     * 실패 시 Mono.error 로 전달되므로 subscribe 의 error 블록에서 처리한다.
     */
    public Mono<Shop> saveShop(Long id, Shop shop) {
        return shopApiClient.updateShop(id, shop)
            .flatMap(response -> {
                if (isSuccess(response)) {
                    return Mono.just(shop);
                }
                return Mono.error(new RuntimeException(messageOf(response)));
            })
            .doOnNext(saved -> this.cachedShop = saved);
    }

    // ==========================================
    // Internal
    // ==========================================

    private Mono<Shop> fetchAndCache() {
        return shopApiClient.getFirstShop()
            .flatMap(response -> {
                if (isSuccess(response) && response.hasData()) {
                    return Mono.just(response.data());
                }
                return Mono.error(new RuntimeException("Failed to load shop: " + messageOf(response)));
            })
            .doOnNext(shop -> this.cachedShop = shop);
    }

    private synchronized void clearLoading() {
        loadingMono = null;
    }

    private boolean isSuccess(ApiResponse<?> response) {
        return response != null && response.isSuccess();
    }

    private String messageOf(ApiResponse<?> response) {
        return response != null ? response.message() : "Unknown error";
    }
}
