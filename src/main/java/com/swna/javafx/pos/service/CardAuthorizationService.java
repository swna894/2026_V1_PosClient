package com.swna.javafx.pos.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.swna.javafx.pos.dto.request.CardAuthRequest;
import com.swna.javafx.pos.dto.request.CardAuthResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardAuthorizationService {

    private final VaultService vaultService;

    public CardAuthResult authorize(CardAuthRequest request) {
        log.info("[CardAuth] 승인 요청 - type: {}, txId: {}, amount: {}, hasCashOut: {}", 
            request.type(), request.transactionId(), request.amount(), request.hasCashOut());
        
        validateBusinessRules(request);
        
        CardAuthResult result = vaultService.processTransaction(request);
        
        postProcessResult(request, result);
        
        return result;
    }
    
    private void validateBusinessRules(CardAuthRequest request) {
        // 비즈니스 규칙 검증 로직
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        // 일일 한도, 시간 제한 등 추가 검증
    }
    
    private void postProcessResult(CardAuthRequest request, CardAuthResult result) {
        switch (result.getResult()) {
            case SUCCESS -> {
                log.info("[CardAuth] ✅ 승인 성공 - authCode: {}, txId: {}", 
                    result.getAuthCode(), result.getTransactionId());
                // 영수증 발행
                // 재고 차감
                // 거래 내역 저장
            }
            case CANCELLED -> {
                log.info("[CardAuth] ❌ 사용자 취소 - txId: {}", request.transactionId());
                // 취소 내역 기록
            }
            case TIMED_OUT -> {
                log.warn("[CardAuth] ⏰ 시간 초과 - txId: {}", request.transactionId());
                // 재시도 로직
            }
            case UNKNOWN -> {
                log.warn("[CardAuth] ❓ 결과 불명 - txId: {}", request.transactionId());
                // 관리자 알림
            }
            case FAILURE -> {
                log.error("[CardAuth] 💥 승인 실패 - txId: {}, message: {}", 
                    request.transactionId(), result.getMessage());
                // 실패 Handling
            }
            default -> log.error("[CardAuth] 알 수 없는 결과 상태: {}", result.getResult());
        }
    }
}