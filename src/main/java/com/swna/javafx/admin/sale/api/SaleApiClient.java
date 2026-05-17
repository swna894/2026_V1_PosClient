package com.swna.javafx.admin.sale.api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.common.api.WebClientCommon;
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

    private final WebClientCommon webClientCommon;
    
    // API Endpoint 상수
    private static final String API_SALES_DATE_RANGE = "/sales/date-range";
    
    // 타임아웃 설정
    private static final int API_TIMEOUT_SECONDS = 30;
    private static final int RETRY_COUNT = 3;
    
    // 날짜 포맷 (ISO 형식)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // ParameterizedTypeReference 상수
    private static final ParameterizedTypeReference<ApiResponse<List<SaleDto>>> SALE_LIST_TYPE = 
        new ParameterizedTypeReference<ApiResponse<List<SaleDto>>>() {};
    
    /**
     * 기간별 판매 목록 조회
     * 
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @return 판매 DTO 리스트 Mono
     */
    public Mono<List<SaleDto>> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // URL 생성 (쿼리 파라미터 포함)
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
     * 
     * @return 판매 DTO 리스트 Mono
     */
    public Mono<List<SaleDto>> getTodaySales() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * 특정 일자 판매 목록 조회
     * 
     * @param date 조회할 날짜
     * @return 판매 DTO 리스트 Mono
     */
    public Mono<List<SaleDto>> getSalesByDate(LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * 이번주 판매 목록 조회 (월요일 ~ 일요일)
     * 
     * @return 판매 DTO 리스트 Mono
     */
    public Mono<List<SaleDto>> getThisWeekSales() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.withHour(0).withMinute(0).withSecond(0)
                .minusDays(now.getDayOfWeek().getValue() - 1); // 월요일로 이동
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfWeek, endOfWeek);
    }
    
    /**
     * 이번달 판매 목록 조회
     * 
     * @return 판매 DTO 리스트 Mono
     */
    public Mono<List<SaleDto>> getThisMonthSales() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        return getSalesByDateRange(startOfMonth, endOfMonth);
    }
    
    /**
     * ApiResponse 언래핑
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