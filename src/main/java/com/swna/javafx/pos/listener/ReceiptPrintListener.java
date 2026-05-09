package com.swna.javafx.pos.listener;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.admin.shop.viewmodel.ShopViewModel;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.print.ReceiptPrinter;
import com.swna.javafx.pos.print.ReceiptStyle;
import com.swna.javafx.pos.service.PaymentResult;

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

    @Async("printExecutor")
    @EventListener
    public void printReceipt(PaymentSuccessEvent event) {
        SaleRequest saleRequest = event.getSaleRequest();
        PaymentResult paymentResult = event.getPaymentResult();
        List<PosItem> posItems = event.getPosItems();

        // ✅ ShopViewModel의 getShopBlocking() 사용
        Shop shop = getShopInfo();
        System.out.println("shop: " + shop);
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
            
            log.info("Print command successfully sent to the hardware - Receipt No: {}", receiptNo);
            
        } catch (Exception e) {
            log.error("Printing failed for receipt: {}", receiptNo, e);
            publishPrintFailure(receiptNo, "Printing Failed: " + e.getMessage());
        }
    }

    /**
     * Shop 정보 가져오기
     * - 캐시 우선
     * - 캐시 없으면 ShopViewModel.getShopBlocking() 사용 (자동으로 기본값 반환)
     */
    private Shop getShopInfo() {
        // 1. 먼저 캐시 확인
        Shop cachedShop = shopViewModel.getCachedShop();
        if (cachedShop != null) {
            log.debug("Using cached shop: {}", cachedShop.getName());
            return cachedShop;
        }
        
        // 2. 캐시가 없으면 ShopViewModel의 블로킹 메서드 사용
        //    (내부에서 API 호출 후 실패 시 기본 Shop 반환)
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
    
    /**
     * 기본 Shop 생성 (최후의 방법)
     */
    private Shop createDefaultShop() {
        log.debug("Creating default shop using factory method");
        return Shop.create(
            "My Store",           // name
            "Store Address",      // address
            "000-0000-0000",      // phone
            "000-00-00000"        // businessNo
        );
    }

    private void publishPrintFailure(String receiptNo, String customMessage) {
        eventPublisher.publishEvent(new PrintFailureEvent(receiptNo, customMessage));
    }
}