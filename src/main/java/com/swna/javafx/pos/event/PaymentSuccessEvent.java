// PaymentSuccessEvent.java
package com.swna.javafx.pos.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;


import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;

import lombok.Getter;

@Getter
public class PaymentSuccessEvent extends ApplicationEvent {
    
  private static final long serialVersionUID = 1L;
    
    private final transient SaleRequest saleRequest;
    private final transient PaymentResult paymentResult;
    private final transient List<PosItem> posItems;
    
    /**
     * 새로운 생성자 - SaleRequest 직접 전달
     */
    public PaymentSuccessEvent(Object source, SaleRequest saleRequest, PaymentResult paymentResult, List<PosItem> posItems ) {
        super(source);
        this.saleRequest = saleRequest;
        this.paymentResult = paymentResult;
        this.posItems = posItems;
    }
    
    // 편의 메서드
    public String getReceiptNo() {
        return paymentResult != null && paymentResult.getSaleResponse() != null 
            ? paymentResult.getSaleResponse().receiptNo() 
            : null;
    }
    
    public boolean isSuccess() {
        return paymentResult != null && paymentResult.isSuccess();
    }
}