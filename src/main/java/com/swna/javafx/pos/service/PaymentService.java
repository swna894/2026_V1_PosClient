package com.swna.javafx.pos.service;

import com.swna.javafx.common.api.ApiEndpointMapper;
import com.swna.javafx.common.api.CommonApiClient;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.*;
import com.swna.javafx.pos.dto.response.SaleResponse;

import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    private final CommonApiClient commonApiClient;
    private final ApiEndpointMapper apiEndpointMapper;
    
    public PaymentService(CommonApiClient commonApiClient, ApiEndpointMapper apiEndpointMapper) {
        this.commonApiClient = commonApiClient;
        this.apiEndpointMapper = apiEndpointMapper;
    }

    // ========== Public Async Methods ==========
    
    /**
     * 현금 결제 (비동기)
     */
    public Mono<PaymentResult> processCashPayment(
            ObservableList<PosItem> items,
            BigDecimal totalAfterDiscount,
            BigDecimal receivedCash) {

        // 1. 입력 검증
        if (receivedCash == null) {
            return Mono.just(PaymentResult.fail("Received cash amount cannot be null"));
        }
        
        if (totalAfterDiscount == null || totalAfterDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just(PaymentResult.fail("Total amount is invalid"));
        }
        
        BigDecimal change = receivedCash.subtract(totalAfterDiscount);
        
        if (change.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.just(PaymentResult.fail("Insufficient cash received"));
        }
        
        // 2. SaleRequest 생성
        BigDecimal totalDiscount = calculateTotalDiscount(items);
        SaleRequest saleRequest = buildCashPaymentSaleRequest(
            items, receivedCash, totalAfterDiscount, totalDiscount
        );
        
        // 3. API 호출 - 메타데이터 타입을 ApiResponse<SaleResponse>로 지정
        ApiEndpointMapper.DomainMetadata<ApiResponse<SaleResponse>> metadata = 
            apiEndpointMapper.getMetadata("sale_create");
        
        return commonApiClient.postForData(metadata, saleRequest, Map.of(), Map.of())
            .map(saleResponse -> {
                if (saleResponse == null) {
                    log.error("[Payment] SaleResponse is null");
                    return PaymentResult.fail("Empty response from server", saleRequest);
                }
                
                log.info("[Payment] Cash payment success - Receipt: {}, Change: {}, Sale ID: {}", 
                    saleResponse.receiptNo(), change, saleResponse.id());
                return PaymentResult.success(saleResponse, change);
            })
            .onErrorResume(error -> {
                log.error("[Payment] API call failed: {}", error.getMessage(), error);
                return Mono.just(PaymentResult.fail(
                    error.getMessage(), saleRequest
                ));
            });
    }
    /**
     * 현금 인출(Cashout) 결제 (비동기)
     */
    public Mono<PaymentResult> processCashoutPayment(
            ObservableList<PosItem> items,
            BigDecimal totalAfterDiscount,
            BigDecimal cashoutAmount) {
        
        // 1. 입력 검증
        if (cashoutAmount == null) {
            return Mono.just(PaymentResult.fail("Cashout amount cannot be null"));
        }
        
        if (cashoutAmount.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.just(PaymentResult.fail("Cashout amount cannot be negative"));
        }
        
        if (totalAfterDiscount == null || totalAfterDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just(PaymentResult.fail("Total amount is invalid"));
        }
        
        // 2. SaleRequest 생성
        BigDecimal totalDiscount = calculateTotalDiscount(items);
        SaleRequest saleRequest = buildCashoutSaleRequest(items, totalAfterDiscount, cashoutAmount, totalDiscount);
        
        // 3. API 호출 - 메타데이터 타입을 ApiResponse<SaleResponse>로 지정
        ApiEndpointMapper.DomainMetadata<ApiResponse<SaleResponse>> metadata = 
            apiEndpointMapper.getMetadata("sale_create");
        
        return commonApiClient.postForData(metadata, saleRequest, Map.of(), Map.of())
            .map(saleResponse -> {
                if (saleResponse == null) {
                    log.error("[Payment] SaleResponse is null");
                    return PaymentResult.fail("Empty response from server", saleRequest);
                }
                
                log.info("[Payment] Cashout payment success - Receipt: {}, Card: {}, Cashout: {}", 
                    saleResponse.receiptNo(), totalAfterDiscount, cashoutAmount);
                return PaymentResult.success(saleResponse);
            })
            .onErrorResume(error -> {
                log.error("[Payment] API call failed: {}", error.getMessage(), error);
                return Mono.just(PaymentResult.fail(
                    error.getMessage(), saleRequest
                ));
            });
    }
    
    /**
     * 혼합 결제 (비동기)
     */

public Mono<PaymentResult> processMixedPayment(
        ObservableList<PosItem> items,
        BigDecimal totalAfterDiscount,
        BigDecimal cashPart,
        BigDecimal creditPart) {
    
    // 1. 입력 검증
    if (cashPart == null || creditPart == null) {
        return Mono.just(PaymentResult.fail("Payment amounts cannot be null"));
    }
    
    if (totalAfterDiscount == null || totalAfterDiscount.compareTo(BigDecimal.ZERO) <= 0) {
        return Mono.just(PaymentResult.fail("Total amount is invalid"));
    }
    
    BigDecimal totalPayment = cashPart.add(creditPart);
    
    if (totalPayment.compareTo(totalAfterDiscount) != 0) {
        return Mono.just(PaymentResult.fail(
            String.format("Amount mismatch: payment=%s, expected=%s", totalPayment, totalAfterDiscount)
        ));
    }
    
    // 2. SaleRequest 생성
    BigDecimal totalDiscount = calculateTotalDiscount(items);
    SaleRequest saleRequest = buildMixedPaymentSaleRequest(items, cashPart, creditPart, totalDiscount);
    
    // 3. API 호출 - 메타데이터 타입을 ApiResponse<SaleResponse>로 지정
    ApiEndpointMapper.DomainMetadata<ApiResponse<SaleResponse>> metadata = 
        apiEndpointMapper.getMetadata("sale_create");
    
    return commonApiClient.postForData(metadata, saleRequest, Map.of(), Map.of())
        .map(saleResponse -> {
            if (saleResponse == null) {
                log.error("[Payment] SaleResponse is null");
                return PaymentResult.fail("Empty response from server", saleRequest);
            }
            
            log.info("[Payment] Mixed payment success - Receipt: {}, Cash: {}, Credit: {}", 
                saleResponse.receiptNo(), cashPart, creditPart);
            return PaymentResult.success(saleResponse);
        })
        .onErrorResume(error -> {
            log.error("[Payment] API call failed: {}", error.getMessage(), error);
            return Mono.just(PaymentResult.fail(
                error.getMessage(), saleRequest
            ));
        });
}
    
    // ========== Private Builder Methods ==========
    
    private SaleRequest buildCashPaymentSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal receivedCash,
            BigDecimal totalAfterDiscount,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
        log.debug("[Cash] Building sale request with {} items", saleItems.size());
        
        List<PaymentRequest> payments = List.of(
            new PaymentRequest("CASH", totalAfterDiscount, receivedCash, BigDecimal.ZERO, null)
        );
        
        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    private SaleRequest buildCashoutSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal creditAmount,
            BigDecimal cashoutAmount,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
        log.debug("[Cashout] Building sale request with {} items", saleItems.size());

        List<PaymentRequest> payments = List.of(
            new PaymentRequest("CARD", creditAmount, creditAmount, cashoutAmount, 
                "CASHOUT_" + System.currentTimeMillis())
        );

        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    private SaleRequest buildMixedPaymentSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal cashPart,
            BigDecimal creditPart,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
        log.debug("[Mix] Building sale request with {} items", saleItems.size());

        List<PaymentRequest> payments = buildMixedPayments(cashPart, creditPart);
        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    // ========== Common Helper Methods ==========
    
    private BigDecimal calculateTotalDiscount(ObservableList<PosItem> items) {
        return items.stream()
            .map(item -> BigDecimal.valueOf(item.getDiscountTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private List<SaleItemRequest> buildSaleItemRequests(ObservableList<PosItem> items) {
        return items.stream()
            .map(item -> {
                BigDecimal originalValue = BigDecimal.valueOf(item.getOriginalPrice());
                BigDecimal sellingValue = BigDecimal.valueOf(item.getSellingPrice());
                BigDecimal discountValue = BigDecimal.valueOf(item.getUnitDiscount());
                DiscountType discountType = item.getDiscountType();
                
                if (discountValue.compareTo(BigDecimal.ZERO) == 0) {
                    discountType = DiscountType.NONE;
                }
                
                return new SaleItemRequest(
                    item.getBarcode(),
                    item.getQty(),
                    originalValue,
                    sellingValue,
                    discountValue,
                    discountType,
                    item.getComment() != null ? item.getComment() : ""
                );
            })
            .toList();
    }
    
    private List<PaymentRequest> buildMixedPayments(BigDecimal cashPart, BigDecimal creditPart) {
        List<PaymentRequest> payments = new ArrayList<>();
        
        if (cashPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest("CASH", cashPart, cashPart, BigDecimal.ZERO, null));
        }
        
        if (creditPart.compareTo(BigDecimal.ZERO) > 0) {
            payments.add(new PaymentRequest("CARD", creditPart, creditPart, BigDecimal.ZERO, 
                "CREDIT_" + System.currentTimeMillis()));
        }
        
        return payments;
    }
    
    private List<DiscountRequest> buildDiscountRequests(BigDecimal totalDiscount) {
        if (totalDiscount == null || totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        
        return List.of(
            DiscountRequest.fixed(totalDiscount, "Cart total discount")
        );
    }
}