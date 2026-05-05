package com.swna.javafx.pos.service;

import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.*;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PaymentService {

    // ========== Public Methods ==========
    
    public PaymentResult processCashPayment(
            ObservableList<PosItem> items,
            BigDecimal totalAfterDiscount,
            BigDecimal receivedCash) {
        
        try {
            BigDecimal change = receivedCash.subtract(totalAfterDiscount);
            
            if (change.compareTo(BigDecimal.ZERO) < 0) {
                return PaymentResult.fail("Insufficient cash received");
            }
            
            BigDecimal totalDiscount = calculateTotalDiscount(items);
            SaleRequest saleRequest = buildCashPaymentSaleRequest(items, receivedCash, totalAfterDiscount, totalDiscount);
            
            log.info("[Payment] Cash payment success - Received: {}, Change: {}", receivedCash, change);
            return PaymentResult.success(saleRequest, change);
            
        } catch (Exception e) {
            log.error("[Payment] Cash payment failed: {}", e.getMessage());
            return PaymentResult.fail(e.getMessage());
        }
    }
    
    public PaymentResult processCashoutPayment(
            ObservableList<PosItem> items,
            BigDecimal totalAfterDiscount,
            BigDecimal cashoutAmount) {
        
        try {
            if (cashoutAmount.compareTo(BigDecimal.ZERO) < 0) {
                return PaymentResult.fail("Cashout amount cannot be negative");
            }
            
            BigDecimal totalDiscount = calculateTotalDiscount(items);
            SaleRequest saleRequest = buildCashoutSaleRequest(items, totalAfterDiscount, cashoutAmount, totalDiscount);
           
            log.info("[Payment] Cashout payment success - Card: {}, Cashout: {}", totalAfterDiscount, cashoutAmount);
            return PaymentResult.success(saleRequest);
            
        } catch (Exception e) {
            log.error("[Payment] Cashout payment failed: {}", e.getMessage());
            return PaymentResult.fail(e.getMessage());
        }
    }
    
    public PaymentResult processMixedPayment(
            ObservableList<PosItem> items,
            BigDecimal totalAfterDiscount,
            BigDecimal cashPart,
            BigDecimal creditPart) {
        
        try {
            BigDecimal totalPayment = cashPart.add(creditPart);
            
            if (totalPayment.compareTo(totalAfterDiscount) != 0) {
                return PaymentResult.fail(
                    String.format("Amount mismatch: payment=%s, expected=%s", totalPayment, totalAfterDiscount)
                );
            }
            
            BigDecimal totalDiscount = calculateTotalDiscount(items);
            SaleRequest saleRequest = buildMixedPaymentSaleRequest(items, cashPart, creditPart, totalDiscount);
            
            log.info("[Payment] Mixed payment success - Cash: {}, Credit: {}", cashPart, creditPart);
            return PaymentResult.success(saleRequest);
            
        } catch (Exception e) {
            log.error("[Payment] Mixed payment failed: {}", e.getMessage());
            return PaymentResult.fail(e.getMessage());
        }
    }
    
    // ========== Private Builder Methods ==========
    
    /**
     * 현금 결제용 SaleRequest 빌드
     */
    private SaleRequest buildCashPaymentSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal receivedCash,
            BigDecimal totalAfterDiscount,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
        List<PaymentRequest> payments = List.of(
            new PaymentRequest("CASH", totalAfterDiscount, receivedCash, BigDecimal.ZERO, null)
        );
        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    /**
     * 현금 인출(Cashout) 결제용 SaleRequest 빌드
     * ✅ DiscountRequest 추가됨
     */
    private SaleRequest buildCashoutSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal creditAmount,
            BigDecimal cashoutAmount,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
            // ✅ 로그로 변경 - SaleItemRequest 목록 출력
        log.debug("[Cashout] SaleItemRequest 목록:");
        saleItems.forEach(item -> log.info("  - {}", item));

        List<PaymentRequest> payments = List.of(
            new PaymentRequest("CARD", creditAmount, creditAmount, cashoutAmount, 
                "CASHOUT_" + System.currentTimeMillis())
        );

         // ✅ 로그로 변경 - PaymentRequest 목록 출력
        log.debug("[Cashout] DiscountRequest 목록:");
        payments.forEach(item -> log.info("  - {}", item));

        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
              
        // ✅ 로그로 변경 - DiscountRequest 목록 출력
        log.debug("[Cashout] DiscountRequest 목록:");
        discounts.forEach(item -> log.info("  - {}", item));
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    /**
     * 혼합 결제용 SaleRequest 빌드 (현금 + 카드)
     * ✅ DiscountRequest 추가됨
     */
    private SaleRequest buildMixedPaymentSaleRequest(
            ObservableList<PosItem> items,
            BigDecimal cashPart,
            BigDecimal creditPart,
            BigDecimal totalDiscount) {
        
        List<SaleItemRequest> saleItems = buildSaleItemRequests(items);
        List<PaymentRequest> payments = buildMixedPayments(cashPart, creditPart);
        List<DiscountRequest> discounts = buildDiscountRequests(totalDiscount);
        
        return new SaleRequest(saleItems, payments, discounts);
    }
    
    // ========== Common Helper Methods ==========
    
    /**
     * 장바구니 전체 할인 총액 계산
     * 개별 아이템의 할인 금액(unitDiscount * qty) 합계
     */
    private BigDecimal calculateTotalDiscount(ObservableList<PosItem> items) {
        return items.stream()
            .map(item -> BigDecimal.valueOf(item.getDiscountTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * SaleItemRequest 목록 빌드
     * 각 아이템의 개별 할인 정보 포함
     */
    private List<SaleItemRequest> buildSaleItemRequests(ObservableList<PosItem> items) {
        return items.stream()
            .map(item -> {
                BigDecimal discountValue = BigDecimal.valueOf(item.getUnitDiscount());
                DiscountType discountType = item.getDiscountType();  // ✅ 이제 정상 동작
                
                // 할인 금액이 0이면 NONE으로 통일
                if (discountValue.compareTo(BigDecimal.ZERO) == 0) {
                    discountType = DiscountType.NONE;
                }
                
                return new SaleItemRequest(
                    item.getBarcode(),
                    item.getQty(),
                    discountValue,
                    discountType,
                    item.getComment() != null ? item.getComment() : ""
                );
            })
            .toList();
    }
    
    /**
     * 아이템의 할인 유형 결정
     * - 할인 금액이 0 이하: NONE
     * - 퍼센트 할인이 적용된 경우: PERCENT
     * - 그 외: AMOUNT
     */
    private DiscountType determineDiscountType(PosItem item) {
        double unitDiscount = item.getUnitDiscount();
        
        // 할인이 없는 경우
        if (unitDiscount <= 0) {
            return DiscountType.NONE;
        }
        
        // 퍼센트 할인이 적용된 경우 (PosItem에 저장된 정보 사용)
        if (item.getDiscountType() != null && item.getDiscountType() == DiscountType.PERCENT) {
            return DiscountType.PERCENT;
        }
        
        // 금액 할인
        return DiscountType.AMOUNT;
    }
    /**
     * 혼합 결제용 PaymentRequest 목록 빌드
     */
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
    
    /**
     * 전체 장바구니 레벨 할인 정보 빌드
     * @param totalDiscount 전체 할인 총액 (0보다 클 때만 추가)
     */
    private List<DiscountRequest> buildDiscountRequests(BigDecimal totalDiscount) {
        if (totalDiscount == null || totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();  // 할인이 없으면 빈 리스트 반환
        }
        
        return List.of(
            DiscountRequest.fixed(totalDiscount, "Cart total discount")
        );
    }
    
    // ========== Inner Classes ==========
    
    @lombok.Value
    public static class PaymentResult {
        boolean success;
        String message;
        SaleRequest saleRequest;
        BigDecimal change;
        
        public static PaymentResult success(SaleRequest saleRequest) {
            return new PaymentResult(true, "Success", saleRequest, BigDecimal.ZERO);
        }
        
        public static PaymentResult success(SaleRequest saleRequest, BigDecimal change) {
            return new PaymentResult(true, "Success", saleRequest, change);
        }
        
        public static PaymentResult fail(String message) {
            return new PaymentResult(false, message, null, BigDecimal.ZERO);
        }
    }
}