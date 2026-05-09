package com.swna.javafx.pos.listener;


import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.event.PaymentSuccessEvent;
import com.swna.javafx.pos.event.PrintFailureEvent;
import com.swna.javafx.pos.print.ReceiptPrinter;
import com.swna.javafx.pos.print.ReceiptStyle;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
@RequiredArgsConstructor // Automatically injects ReceiptPrinter
public class ReceiptPrintListener {

    private final ApplicationEventPublisher eventPublisher;
    private final ReceiptPrinter receiptPrinter;
    private final Shop shop;

    @Async("printExecutor") // Asynchronous processing to prevent UI freezing
    @EventListener // Triggered when PaymentSuccessEvent is published
    public void printReceipt(PaymentSuccessEvent event) {
        String receiptNo = event.result().getSaleResponse().receiptNo();
        log.info("Starting receipt printing - Receipt No: {}", receiptNo);
        
        try {
            // Execute the actual printing logic using the ReceiptPrinter component
            // Parameters: PaymentResult, Item List, Shop Info, Paper Size, Footer Message
            receiptPrinter.printInvoice(
                event.result(), 
                event.soldItems(), 
                shop, 
                ReceiptStyle.SIZE_80MM, 
                "Thank you for your visit!"
            );
            
            log.info("Print command successfully sent to the hardware - Receipt No: {}", receiptNo);
            
        } catch (Exception e) {
            e.printStackTrace();
            // 그 외 모든 예외
            publishPrintFailure(receiptNo, "Printing Failed");
        }
    }

    // 중복 코드를 줄이기 위한 헬퍼 메서드
    private void publishPrintFailure(String receiptNo, String customMessage) {
        eventPublisher.publishEvent(new PrintFailureEvent(receiptNo, customMessage));
    }
}
