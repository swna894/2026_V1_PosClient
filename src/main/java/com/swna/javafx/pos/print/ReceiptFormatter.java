package com.swna.javafx.pos.print;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.service.PaymentResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ReceiptFormatter {
    private static final String NL = "\n";
    private static final DecimalFormat CURRENCY_DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SRC_DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DST_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // 상품명 최대 길이 (영수증 너비에 맞게 조정)
    private static final int MAX_DESCRIPTION_LENGTH = 30;

    /**
     * 금액을 $ 표시와 함께 포맷팅
     */
    private String formatCurrency(double amount) {
        return "$" + CURRENCY_DF.format(amount);
    }
    
    /**
     * description을 지정된 길이로 자르고 ... 추가
     */
    private String truncateDescription(String description, int maxLength) {
        if (description == null) return "";
        if (description.length() <= maxLength) return description;
        return description.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * description을 영수증 너비에 맞게 자르기 (style 기반)
     */
    private String truncateDescriptionForStyle(String description, ReceiptStyle style) {
        // 번호(4자: "1. ") + 공백(1자) + 상품명
        int maxLength = style.getWidth() - 6;  // 6 = 번호(4) + 여백(2)
        return truncateDescription(description, maxLength);
    }

    public String buildContent(SaleRequest saleRequest, PaymentResult paymentResult, 
                               List<PosItem> posItems, Shop shop, 
                               ReceiptStyle style, String inform) {
        
        log.info("ReceiptFormatter.buildContent() - posItems size: {}", 
            posItems != null ? posItems.size() : 0);
        
        StringBuilder sb = new StringBuilder();

        // null 안전 처리
        String receiptNo = (paymentResult != null && paymentResult.getSaleResponse() != null) 
            ? paymentResult.getSaleResponse().receiptNo() : "N/A";
        String date = formatReceiptDate(receiptNo);
        
        String shopName = (shop != null && shop.getName() != null) ? shop.getName() : "My Store";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "";
        
        // Shop 이름도 너무 길면 자르기
        if (shopName.length() > style.getWidth()) {
            shopName = truncateDescription(shopName, style.getWidth());
        }

        // [Header]
        sb.append(style.center(shopName)).append(NL);
        if (!shopAddress.isEmpty()) {
            if (shopAddress.length() > style.getWidth()) {
                shopAddress = truncateDescription(shopAddress, style.getWidth());
            }
            sb.append(style.center(shopAddress)).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);
        
        // [Body] - 상품 목록 (번호 추가)
        if (posItems != null && !posItems.isEmpty()) {
            AtomicInteger counter = new AtomicInteger(1);
            
            for (PosItem item : posItems) {
                int number = counter.getAndIncrement();
                String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
                
                // ✅ description을 영수증 너비에 맞게 자르기
                String truncatedDesc = truncateDescriptionForStyle(description, style);
                
                // 번호를 포함한 상품명
                sb.append(String.format("%d. %s", number, truncatedDesc)).append(NL);
                
                // ✅ 금액에 $ 추가
                String qtyPrice = String.format("    %d x %s", item.getQty(), formatCurrency(item.getSellingPrice()));
                sb.append(style.justify(qtyPrice, formatCurrency(item.getFinalAmount()))).append(NL);
            }
        } else {
            sb.append(style.center("No Items")).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);

        // [Footer] - 합계 및 결제 정보
        double totalAmount = (paymentResult != null && paymentResult.getSaleResponse() != null)
            ? paymentResult.getSaleResponse().totalAmount().doubleValue() : 0.0;
        
        sb.append(style.justify("TOTAL AMOUNT", formatCurrency(totalAmount))).append(NL);
        
        // 결제 정보
        if (saleRequest != null && saleRequest.payments() != null) {
            for (PaymentRequest p : saleRequest.payments()) {
                double amount = p.amount().doubleValue();
                double cashout = p.cashoutAmount() != null ? p.cashoutAmount().doubleValue() : 0.0;
                
                if ("CASH".equals(p.type())) {
                    sb.append(style.justify("CASH PAID", formatCurrency(amount))).append(NL);
                    
                } else if ("CARD".equals(p.type())) {
                    if (cashout > 0) {
                        double totalCharged = amount + cashout;
                        sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                        sb.append(style.justify("  + CASHOUT", formatCurrency(cashout))).append(NL);
                        sb.append(style.getLine(false)).append(NL);
                        sb.append(style.justify("TOTAL CHARGED", formatCurrency(totalCharged))).append(NL);
                    } else {
                        sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                    }
                }
            }
        }

        // [Notice]
        if (inform != null && !inform.isBlank()) {
            String truncatedInform = truncateDescription(inform, style.getWidth() * 2);
            sb.append(style.getNoticeLine("Notice")).append(NL);
            sb.append(wrapText(truncatedInform, style.getWidth())).append(NL);
        }

        String result = sb.toString();
        log.info("Content built, length: {} chars", result.length());
        
        return result;
    }

    private String formatReceiptDate(String receiptNo) {
        try {
            if (receiptNo == null || !receiptNo.contains("_")) return LocalDateTime.now().format(DST_DTF);
            return LocalDateTime.parse(receiptNo.split("_")[0], SRC_DTF).format(DST_DTF);
        } catch (Exception e) {
            log.warn("Date parsing failed for receiptNo: {}", receiptNo);
            return LocalDateTime.now().format(DST_DTF);
        }
    }

    private String wrapText(String text, int width) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += width) {
            int end = Math.min(i + width, text.length());
            sb.append(text, i, end).append(NL);
        }
        return sb.toString().trim();
    }
}