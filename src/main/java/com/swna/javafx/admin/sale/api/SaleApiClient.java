package com.swna.javafx.admin.sale.api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.admin.sale.dto.SaleItemResponse;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 판매 API 클라이언트
 * WebClientCommon을 사용하여 API 통신 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleApiClient {

    private final SimpleApiClient webClientCommon;
    
    // API Endpoint 상수
    private static final String API_SALES_DATE_RANGE = "/sales/date-range";
    private static final String API_SALES_ITEMS = "/sales/%d/items"; // 2. 타겟 엔드포인트 URL 포맷 상수 추가
    
    // 타임아웃 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    
    // 날짜 포맷 (ISO 형식)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // ParameterizedTypeReference 상수
    private static final ParameterizedTypeReference<ApiResponse<List<SaleDto>>> SALE_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<SaleDto>>>() {};
        
    // 3. SaleItemResponse 목록을 받기 위한 ParameterizedTypeReference 상수를 추가합니다.
    private static final ParameterizedTypeReference<ApiResponse<List<SaleItemResponse>>> SALE_ITEM_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<SaleItemResponse>>>() {};
    
    /**
     * 특정 판매 건(saleId)의 아이템 목록 조회 🚀 (새로 추가된 메서드)
     * * @param saleId 판매 고유 ID
     * @return 판매 아이템 DTO 리스트 Mono
     */
    public Mono<List<SaleItemResponse>> getSaleItemsBySaleId(Long saleId) {
        // 서버의 @GetMapping("/{saleId}/items") 구조와 일치하는 URL 구성
        String url = String.format(API_SALES_ITEMS, saleId);
        
        log.debug("[Sale API] Fetching sale items for saleId: {} from: {}", saleId, url);
        
        return webClientCommon.get(url, SALE_ITEM_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(this::unwrapSaleItemsResponse) // 전용 언래핑 메서드 호출
            .onErrorResume(e -> {
                log.error("[Sale API] Failed to fetch sale items for saleId: {}", saleId, e);
                return Mono.just(List.of()); // 에러 발생 시 빈 리스트 리턴 보장
            });
    }
    
    /**
     * 기간별 판매 목록 조회
     */
    public Mono<List<SaleDto>> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String url = String.format("%s?startDate=%s&endDate=%s", 
            API_SALES_DATE_RANGE,
            startDate.format(DATE_TIME_FORMATTER),
            endDate.format(DATE_TIME_FORMATTER));
        
        log.debug("[Sale API] Fetching sales by date range: {} to {} from: {}", startDate, endDate, url);
        
        return webClientCommon.get(url, SALE_LIST_TYPE)
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(this::unwrapResponse)
            .onErrorResume(e -> {
                log.error("[Sale API] Failed to fetch sales by date range: {} to {}", startDate, endDate, e);
                return Mono.just(List.of());
            });
    }
    
    /**
     * 오늘 판매 목록 조회
     */
    public Mono<List<SaleDto>> getTodaySales() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * 특정 일자 판매 목록 조회
     */
    public Mono<List<SaleDto>> getSalesByDate(LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * 이번주 판매 목록 조회 (월요일 ~ 일요일)
     */
    public Mono<List<SaleDto>> getThisWeekSales() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.withHour(0).withMinute(0).withSecond(0)
                .minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfWeek, endOfWeek);
    }
    
    /**
     * 이번달 판매 목록 조회
     */
    public Mono<List<SaleDto>> getThisMonthSales() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfMonth, endOfMonth);
    }
    
    /**
     * ApiResponse<List<SaleItemResponse>> 언래핑 🚀 (새로 추가된 메서드)
     */
    private Mono<List<SaleItemResponse>> unwrapSaleItemsResponse(ApiResponse<List<SaleItemResponse>> response) {
        if (response == null) {
            log.warn("[Sale API] Received null sale items response");
            return Mono.just(List.of());
        }
        
        // JavaFX 클라이언트 측 ApiResponse 클래스 명세(Record 스펙 혹은 메서드 형식)에 맞춰 
        // 기존 unwrapResponse와 동일하게 .isSuccess() 및 .hasData() 검증 후 data()를 추출합니다.
        if (response.isSuccess() && response.hasData()) {
            List<SaleItemResponse> data = response.data();
            log.debug("[Sale API] Success - Fetched {} sale items", data.size());
            return Mono.just(data);
        } else {
            log.warn("[Sale API] Failed to fetch sale items - Code: {}, Message: {}", response.code(), response.message());
            return Mono.just(List.of());
        }
    }

    /**
     * ApiResponse<List<SaleDto>> 언래핑
     */
    private Mono<List<SaleDto>> unwrapResponse(ApiResponse<List<SaleDto>> response) {
        if (response == null) {
            log.warn("[Sale API] Received null response");
            return Mono.just(List.of());
        }
        
        if (response.isSuccess() && response.hasData()) {
            List<SaleDto> data = response.data();
            log.debug("[Sale API] Success - Fetched {} sales", data.size());
            return Mono.just(data);
        } else {
            log.warn("[Sale API] Failed - Code: {}, Message: {}", response.code(), response.message());
            return Mono.just(List.of());
        }
    }
}