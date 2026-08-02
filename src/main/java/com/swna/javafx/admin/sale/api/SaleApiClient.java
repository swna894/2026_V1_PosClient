package com.swna.javafx.admin.sale.api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.admin.sale.dto.SaleItemResponse;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
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
    
    // OS/시스템 OS 기본 타임존 적용
    private static final ZoneId SYSTEM_ZONE_ID = ZoneId.systemDefault();
    
    // API Endpoint 상수
    private static final String API_SALES_DATE_RANGE = "/sales/date-range";
    private static final String API_SALES_ITEMS = "/sales/%d/items";
    
    // 타임아웃 및 재시도 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    
    // 날짜 포맷 (ISO 형식)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * 특정 판매 건(saleId)의 아이템 목록 조회
     * @param saleId 판매 고유 ID
     * @return 판매 아이템 DTO 리스트 Mono
     */
    public Mono<List<SaleItemResponse>> getSaleItemsBySaleId(Long saleId) {
        String url = String.format(API_SALES_ITEMS, saleId);
        
        log.debug("[Sale API] Fetching sale items for saleId: {} from: {}", saleId, url);
        
        return webClientCommon.get(url, TypeReferences.list(SaleItemResponse.class))
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(response -> unwrapListResponse(response, "sale items"))
            .onErrorResume(e -> {
                log.error("[Sale API] Failed to fetch sale items for saleId: {}", saleId, e);
                return Mono.just(List.of());
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
        
        return webClientCommon.get(url, TypeReferences.list(SaleDto.class))
            .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
            .retry(RETRY_COUNT)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(response -> unwrapListResponse(response, "sales"))
            .onErrorResume(e -> {
                log.error("[Sale API] Failed to fetch sales by date range: {} to {}", startDate, endDate, e);
                return Mono.just(List.of());
            });
    }
    
    /**
     * 오늘 판매 목록 조회
     */
    public Mono<List<SaleDto>> getTodaySales() {
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE_ID);
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);
        
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
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE_ID);
        LocalDateTime startOfWeek = now.withHour(0).withMinute(0).withSecond(0)
                .minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfWeek, endOfWeek);
    }
    
    /**
     * 이번달 판매 목록 조회
     */
    public Mono<List<SaleDto>> getThisMonthSales() {
        LocalDateTime now = LocalDateTime.now(SYSTEM_ZONE_ID);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfMonth, endOfMonth);
    }

    /**
     * 공통 List 형태의 ApiResponse 언래핑 헬퍼 메서드
     */
    private <T> Mono<List<T>> unwrapListResponse(ApiResponse<List<T>> response, String resourceName) {
        if (response == null) {
            log.warn("[Sale API] Received null {} response", resourceName);
            return Mono.just(List.of());
        }
        
        if (response.isSuccess() && response.hasData()) {
            List<T> data = response.data();
            log.debug("[Sale API] Success - Fetched {} {}", data.size(), resourceName);
            return Mono.just(data);
        } else {
            log.warn("[Sale API] Failed to fetch {} - Code: {}, Message: {}", resourceName, response.code(), response.message());
            return Mono.just(List.of());
        }
    }
}