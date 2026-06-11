package com.swna.javafx.pos.infrastructure;

import java.io.FileNotFoundException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.swna.javafx.pos.dto.request.CardAuthRequest;
import com.swna.javafx.pos.dto.request.CardAuthResult;

import DotNetInterop.ArgumentException;
import DotNetInterop.ArgumentNullException;
import DotNetInterop.InvalidOperationException;
import Verifone.Vault.Common.TransactionResult;
import Verifone.Vault.POSInterface.CardInfo;
import Verifone.Vault.POSInterface.PurchaseTransaction;
import Verifone.Vault.POSInterface.RefundTransaction;
import Verifone.Vault.POSInterface.TransactionInfo;
import Verifone.Vault.POSInterface.VaultSessionEx;
import jakarta.annotation.PreDestroy;

/**
 * VaultService (SDK 4.x용)
 * --------------------------------------------
 * - 결제: PurchaseTransaction
 * - 환불: RefundTransaction
 * - Cash Out 지원
 * - 세션: VaultSessionEx (자동 세션관리, 스레드 안전)
 */
@Service
public class VaultService {

    private static final Logger logger = LoggerFactory.getLogger(VaultService.class);

    private VaultSessionEx cachedSession;
    private final ReentrantLock sessionLock = new ReentrantLock();

    // =========================================================
    // Public API - CardAuthRequest 기반 거래 처리
    // =========================================================

    /**
     * 통합 카드 거래 처리
     * 
     * @param request 카드 승인 요청 DTO
     * @return 카드 승인 결과 DTO
     */
    public CardAuthResult processTransaction(CardAuthRequest request) {
        logger.info("[Vault] Transaction request - txId: {}, amount: {}, type: {}", 
            request.transactionId(), request.amount(), request.type());

        try {
            if (request.isRefund()) {
                return doRefund(request);
            } else {
                return doPurchase(request);
            }
        } catch (Exception e) {
            logger.error("[Vault] Transaction failed", e);
            return CardAuthResult.failure("Transaction failed: " + e.getMessage());
        }
    }

    /**
     * 구매 거래 (Purchase / Cash Out)
     */
    private CardAuthResult doPurchase(CardAuthRequest request) {
        try {
            VaultSessionEx session = getOrCreateSession();

            String receiptNo = request.transactionId() != null ? 
                request.transactionId() : UUID.randomUUID().toString();

            // 카드번호 및 종류 기록  -> handleSuccess
            PurchaseTransaction purchaseTx = new PurchaseTransaction(receiptNo, request.amount());
            String cardNumber = handleSuccess(purchaseTx);
            purchaseTx.setTransactionCurrency(request.currency());

            if (request.hasCashOut()) {
                // 6월 11일 : 카드 송부 금액 오류 : amount에 cashout 포함 되어 있는데, 
                // 추가로 cashout 금액 추가로 총 요청금액 요류 발생으로 제외함
                // purchaseTx.setCashOutAmount(request.cashOutAmount());
                logger.info("[Vault] Cash Out amount: {}", request.cashOutAmount());
            }

            TransactionResult result = session.executeTransaction(purchaseTx);

            // switch 표현식을 사용한 결과 처리
            return switch (result) {
                case Success -> {
                    TransactionInfo info = purchaseTx.getTxInfo();
                    if (request.hasCashOut()) {
                        yield CardAuthResult.successWithCashOut(
                            info.getAuthCode(),
                            info.getAcquirerReference(),
                            request.amount(),
                            request.cashOutAmount(),
                            cardNumber
                        );
                    } else {
                        yield CardAuthResult.success(
                            info.getAuthCode(),
                            info.getAcquirerReference(),
                            request.amount(),
                            cardNumber
                        );
                    }
                }
                case Cancelled -> CardAuthResult.cancelled();
                case TimedOut -> CardAuthResult.timeout();
                default -> CardAuthResult.failure("Transaction failed: " + result);
            };

        } catch (Exception e) {
            logger.error("[Vault] Purchase failed", e);
            return CardAuthResult.failure("Purchase failed: " + e.getMessage());
        }
    }


    private String handleSuccess(PurchaseTransaction purchaseTx) {
        String cardNumber = null;
        CardInfo cardInfo = purchaseTx.getCardInfo();
        
        if (cardInfo != null) {
            cardNumber = cardInfo.getPAN();
            if (cardNumber == null || cardNumber.isBlank()) {
                cardNumber = cardInfo.getPAN() + " " + cardInfo.getExpiryDate();
            }
            
            // cardInfo가 null이 아닐 때만 로깅
            logger.info("CardType = {}", cardInfo.getCardType());
        } else {
            logger.warn("CardInfo is null - no card information available");
        }
        
        // TxInfo 안전하게 처리
        TransactionInfo txInfo = purchaseTx.getTxInfo();
        if (txInfo != null && txInfo.getDuplicateReceiptImage() != null) {
            logger.info("Duplicate Receipt = \n{}", txInfo.getDuplicateReceiptImage());
        } else {
            logger.debug("No duplicate receipt image available");
        }
        
        logger.info("Transaction succeeded");
        
        return cardNumber;
    }

    /**
     * 환불 거래
     */
    private CardAuthResult doRefund(CardAuthRequest request) {
        try {
            VaultSessionEx session = getOrCreateSession();

            RefundTransaction refundTx = new RefundTransaction(request.transactionId(), request.amount());
            refundTx.setTransactionCurrency(request.currency());
            TransactionResult result = session.executeTransaction(refundTx);

            // switch 표현식을 사용한 결과 처리
            return switch (result) {
                case Success -> {
                    TransactionInfo info = refundTx.getTxInfo();
                    yield CardAuthResult.success(
                        info.getAuthCode(),
                        info.getAcquirerReference(),
                        request.amount(),
                        null
                    );
                }
                case Cancelled -> CardAuthResult.cancelled();
                case TimedOut -> CardAuthResult.timeout();
                default -> CardAuthResult.failure("Refund failed: " + result);
            };

        } catch (Exception e) {
            logger.error("[Vault] Refund failed", e);
            return CardAuthResult.failure("Refund failed: " + e.getMessage());
        }
    }

    // =========================================================
    // VaultSession 관리 (기존 코드 유지)
    // =========================================================

    public VaultSessionEx getOrCreateSession() {
        sessionLock.lock();
        try {
            if (cachedSession == null) {
                cachedSession = openNewSession();
                logger.info("Vault session opened.");
            }
            return cachedSession;
        } catch (Exception e) {
            logger.warn("Vault session invalid. Recreating session.", e);
            closeQuietly(cachedSession);
            cachedSession = openNewSession();
            logger.info("Vault session re-opened after failure.");
            return cachedSession;
        } finally {
            sessionLock.unlock();
        }
    }

    private VaultSessionEx openNewSession() {
        try {
            VaultSessionEx session = new VaultSessionEx();
            session.open();
            return session;
        } catch (FileNotFoundException | InvalidOperationException |
                 ArgumentException | ArgumentNullException |
                 TransformerException | ParserConfigurationException e) {
            throw new IllegalStateException("Failed to open Vault session", e);
        }
    }

    private void closeQuietly(VaultSessionEx session) {
        if (session == null) return;
        try {
            session.dispose();
        } catch (Exception e) {
            logger.debug("Failed to close Vault session.", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        sessionLock.lock();
        try {
            if (cachedSession != null) {
                try {
                    cachedSession.dispose();
                } catch (Exception ignored) {}
                cachedSession = null;
                logger.info("Vault session shutdown complete");
            }
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * 예외 복구 가능 여부 확인
     */
    public boolean isRecoverable(Exception ex) {
        String msg = ex.getMessage();
        return msg != null && (msg.contains("Session is not valid") ||
                               msg.contains("Connection lost") ||
                               msg.contains("Timeout"));
    }
}