package com.swna.javafx.pos.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.swna.javafx.pos.dto.request.CardAuthRequest;
import com.swna.javafx.pos.dto.request.CardAuthResult;
import com.swna.javafx.pos.infrastructure.VaultService;
import com.swna.javafx.pos.service.config.PosToggleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentService {
    
    private final VaultService vaultService;
    private final PosToggleService posToggleService;
    
    /**
     * 일반 카드 결제
     */
    public CardAuthResult purchase(BigDecimal amount) {
        String transactionId = generateTransactionId();
        return purchase(transactionId, amount);
    }
    
    public CardAuthResult purchase(String transactionId, BigDecimal amount) {
        log.info("[CardClient] 카드 결제 요청 - txId: {}, amount: ${}", transactionId, amount);
        
        // POS 결제가 OFF인 경우
        //TODO CardAuthResult.virtualSuccess 결과 확인
        if (!posToggleService.isPosEnabled()) {
            log.info("[CardClient] POS 결제 비활성 상태 - 가상 승인 처리");
            // 가상의 성공 결과 반환 (테스트용)
            return CardAuthResult.virtualSuccess(transactionId, amount); 
        }

        CardAuthRequest request = CardAuthRequest.purchase(transactionId, amount);
        CardAuthResult result = vaultService.processTransaction(request);
        
        logResult("카드 결제", result);
        return result;
    }
    
    /**
     * 현금 인출 결제
     */
    public CardAuthResult purchaseWithCashOut(BigDecimal amount, BigDecimal cashOutAmount) {
        String transactionId = generateTransactionId();
        return purchaseWithCashOut(transactionId, amount, cashOutAmount);
    }
    
    public CardAuthResult purchaseWithCashOut(String transactionId, BigDecimal amount, BigDecimal cashOutAmount) {
        log.info("[CardClient] 현금인출 결제 요청 - txId: {}, amount: ${}, cashOut: ${}", 
            transactionId, amount, cashOutAmount);
        
        // POS 결제가 OFF인 경우
        //TODO CardAuthResult.virtualSuccess 결과 확인
        if (!posToggleService.isPosEnabled()) {
            log.info("[CardClient] POS 결제 비활성 상태 - 가상 승인 처리");
            // 가상의 성공 결과 반환 (테스트용)
            return CardAuthResult.virtualSuccess(transactionId, amount); 
        }

        CardAuthRequest request = CardAuthRequest.cashOut(transactionId, amount, cashOutAmount);
        CardAuthResult result = vaultService.processTransaction(request);
        
        logResult("현금인출 결제", result);
        return result;
    }
    
    /**
     * 환불
     */
    public CardAuthResult refund(String originalTransactionId, BigDecimal amount) {
        log.info("[CardClient] 환불 요청 - originalTxId: {}, amount: ${}", originalTransactionId, amount);
        
        CardAuthRequest request = CardAuthRequest.refund(originalTransactionId, amount);
        CardAuthResult result = vaultService.processTransaction(request);
        
        logResult("환불", result);
        return result;
    }
    
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
    }
    
    private void logResult(String type, CardAuthResult result) {
        if (result.isSuccess()) {
            log.info("[CardClient] {} 성공 - authCode: {}, txId: {}", 
                type, result.getAuthCode(), result.getTransactionId());
        } else if (result.isCancelled()) {
            log.info("[CardClient] {} 사용자 취소", type);
        } else if (result.isTimeout()) {
            log.warn("[CardClient] {} 시간 초과", type);
        } else {
            log.error("[CardClient] {} 실패 - {}", type, result.getMessage());
        }
    }
}