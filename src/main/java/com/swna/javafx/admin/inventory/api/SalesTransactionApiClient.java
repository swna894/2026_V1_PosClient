package com.swna.javafx.admin.inventory.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.model.SalesTransationResponse;
import com.swna.javafx.common.api.SimpleApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTransactionApiClient {

    private final SimpleApiClient webClientCommon;

    private static final String API_SALES_PRODUCTS = "/sales/products";

    /**
     * 상품 바코드, 집계 주기(weekly, monthly, quarterly), 조회 시작일을 기준으로 판매 통계 조회
     * GET /sales/products/{barcode}?period={period}&startDate={startDate}
     */
    public Mono<List<SalesTransationResponse>> getSalesByPeriod(String barcode, String period, LocalDate startDate) {
        String url = String.format("%s/%s?period=%s&startDate=%s",
                API_SALES_PRODUCTS,
                barcode,
                period != null ? period.toLowerCase() : "monthly",
                startDate != null ? startDate.toString() : LocalDate.now(ZoneId.systemDefault()).withDayOfYear(1).toString());

        log.info("[API] GET {}", url);
        
        // 서버 응답 형태가 순수 List 이므로 ParameterizedTypeReference를 통한 직접 바인딩
        return webClientCommon.get(url, new ParameterizedTypeReference<List<SalesTransationResponse>>() {});
    }
}