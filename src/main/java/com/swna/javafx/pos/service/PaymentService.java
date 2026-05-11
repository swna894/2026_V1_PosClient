package com.swna.javafx.pos.service;

import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.SaleResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    private final CommonApiClient commonApiClient;
    private final ApiEndpointMapper apiEndpointMapper;

    private static final String API_SALE_CREATE = "sale_create";

    public PaymentService(CommonApiClient commonApiClient, ApiEndpointMapper apiEndpointMapper) {
        this.commonApiClient = commonApiClient;
        this.apiEndpointMapper = apiEndpointMapper;
    }

    // ========== Core Execution Method ==========
    
    /**
     * 결제 실행 (SaleRequest를 파라미터로 받음)
     * ViewModel에서 생성된 SaleRequest를 그대로 사용하여 API 호출
     *
     * @param request ViewModel에서 생성된 SaleRequest
     * @param paymentType 결제 유형 (로그용)
     * @return PaymentResult Mono
     */
    public Mono<PaymentResult> executePayment(SaleRequest request, String paymentType) {
        // 1. API 호출을 위한 메타데이터 조회
        ApiEndpointMapper.DomainMetadata<ApiResponse<SaleResponse>> metadata = 
            apiEndpointMapper.getMetadata(API_SALE_CREATE);

        log.error("======== payment ========");
        request.items().forEach(item -> log.error("{}", item));
        request.payments().forEach(payment -> log.error("{}", payment));
        log.error("{}", request.discounts());

        // 2. API 호출 및 결과 처리
        return commonApiClient.postForData(metadata, request, Map.of(), Map.of())
            .map(response -> {
                log.info("[Payment] {} success - Receipt: {}, SaleID: {}", 
                    paymentType, response.receiptNo(), response.id());
                return PaymentResult.success(response, BigDecimal.ZERO);
            })
            .onErrorResume(e -> {
                log.error("[Payment] {} failed: {}", paymentType, e.getMessage());
                return Mono.just(PaymentResult.fail(e.getMessage(), request));
            });
    }

    /**
     * 결제 실행 (기본 paymentType "Payment" 사용)
     */
    public Mono<PaymentResult> executePayment(SaleRequest request) {
        return executePayment(request, "Payment");
    }
}