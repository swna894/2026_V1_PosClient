package com.swna.javafx.pos.event.listener;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.viewmodel.ShopViewModel;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.event.ReceiptPrintEvent;
import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.print.ReceiptPrinter;
import com.swna.javafx.pos.print.ReceiptStyle;
import com.swna.javafx.pos.service.config.PrintToggleService;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReceiptPrintListener {

    private final ApplicationEventPublisher eventPublisher;
    private final ReceiptPrinter receiptPrinter;
    private final ShopViewModel shopViewModel;
    private final PrintToggleService printToggleService;  // 추가

    @Async("printExecutor")
    @EventListener
    public void printReceipt(PaymentSuccessEvent event) {
        
        // ========== 프린트 활성화 여부 확인 (서비스 사용) ==========
        if (!printToggleService.isCashedBalance() && !printToggleService.isPrintEnabled()) { 
            String receiptNo = event.getPaymentResult().getReceiptNo();
            log.info("Print is DISABLED - Skipping receipt printing. Receipt No: {}", receiptNo);
            return;  // 프린트 안 함
        }
        // =========================================================
        
        SaleRequest saleRequest = event.getSaleRequest();
        PaymentResult paymentResult = event.getPaymentResult();
        List<PosItem> posItems = event.getPosItems();

        Shop shop = getShopInfo();
        String receiptNo = paymentResult.getReceiptNo();
        log.info("Starting receipt printing - Receipt No: {}", receiptNo);
        
        try {
            receiptPrinter.printInvoice(
                saleRequest, 
                paymentResult, 
                posItems,
                shop, 
                ReceiptStyle.SIZE_80MM, 
                "Thank you for your visit!"
            );
            
            printToggleService.setBarcodeEnabled(false);
            // cashBalenceDialog print 실행을 reset 
            printToggleService.setCashBalance(false); 
            log.info("Print command successfully sent to the hardware - Receipt No: {}", receiptNo);
            
        } catch (Exception e) {
            log.error("Printing failed for receipt: {}", receiptNo, e);
            publishPrintFailure(receiptNo, "Printing Failed: " + e.getMessage());
        }
    }

    @Async("printExecutor")
    @EventListener
    public void printReceipt(ReceiptPrintEvent event) {
        String receiptNo = event.getSaleModel().getReceiptNo();

        Shop shop = getShopInfo(); // 기존 공통 메서드 재사용
        log.info("Starting receipt printing (Event-based) - Receipt No: {}", receiptNo);
        
        try {
            // 새로운 모델 기반 출력 호출
            receiptPrinter.printInvoice(
                event.getSaleModel(), 
                event.getItems(), 
                shop, // 공통 메서드로 가져온 상점 정보 전달
                ReceiptStyle.SIZE_80MM, 
                "Thank you for your visit!"
            );
            log.info("Print success - Receipt No: {}",receiptNo);
        } catch (Exception e) {
            log.error("Printing failed - Receipt No: {}", receiptNo, e);
            publishPrintFailure(receiptNo, "Printing Failed: " + e.getMessage());
            
        }
    }
    
    private Shop getShopInfo() {
        // 1. 먼저 캐시 확인
        Shop cachedShop = shopViewModel.getCachedShop();
        if (cachedShop != null) {
            log.debug("Using cached shop: {}", cachedShop.getName());
            return cachedShop;
        }
        
        // 2. 캐시가 없으면 ShopViewModel의 블로킹 메서드 사용
        log.info("No cached shop, loading from API via ShopViewModel...");
        Shop shop = shopViewModel.getShopBlocking();
        
        if (shop != null) {
            log.info("Shop loaded: {}", shop.getName());
        } else {
            log.warn("Shop is null after getShopBlocking(), using default");
            shop = createDefaultShop();
        }
        
        return shop;
    }
    
    private Shop createDefaultShop() {
        log.debug("Creating default shop using factory method");
        return Shop.create(
            "My Store",
            "Store Address",
            "000-0000-0000",
            "000-00-00000",
            "company",
            "email",
            "000-0000-0000"
        );
    }

    private void publishPrintFailure(String receiptNo, String customMessage) {
        eventPublisher.publishEvent(new PrintFailureEvent(receiptNo, customMessage));
    }
}