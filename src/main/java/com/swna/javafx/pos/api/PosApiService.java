package com.swna.javafx.pos.api;

import com.swna.javafx.common.api.WebClientCommon;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.SaleResponse;
import com.swna.javafx.pos.dto.response.PaymentResult;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PosApiService {

    private final WebClientCommon webClientCommon;
    
    // API Endpoint 상수 직접 정의
    private static final String API_SALE_CREATE = "/sales";

    // ParameterizedTypeReference 상수로 정의 (재사용)
    private static final ParameterizedTypeReference<ApiResponse<SaleResponse>> SALE_RESPONSE_TYPE = 
        new ParameterizedTypeReference<ApiResponse<SaleResponse>>() {};

    public PosApiService(WebClientCommon webClientCommon) {
        this.webClientCommon = webClientCommon;
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
        log.info("[Payment] {} request - URL: {}", paymentType, API_SALE_CREATE);

        return webClientCommon.post(API_SALE_CREATE, request, SALE_RESPONSE_TYPE)
            .map(response -> {
                if (response.isSuccess() && response.hasData()) {
                    SaleResponse saleResponse = response.data();
                    log.info("[Payment] {} success - Receipt: {}, SaleID: {}", 
                        paymentType, saleResponse.receiptNo(), saleResponse.id());
                    return PaymentResult.success(saleResponse, BigDecimal.ZERO);
                } else {
                    log.error("[Payment] {} failed - Code: {}, Message: {}", 
                        paymentType, response.code(), response.message());
                    return PaymentResult.fail(response.message(), request);
                }
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