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

@Slf4j
@Component
public class ReceiptFormatter {
    private static final String NL = "\n";
    private static final DecimalFormat CURRENCY_DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SRC_DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DST_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * 금액을 $ 표시와 함께 포맷팅
     */
    private String formatCurrency(double amount) {
        return "$" + CURRENCY_DF.format(amount);
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

        // [Header]
        sb.append(style.center(shopName)).append(NL);
        if (!shopAddress.isEmpty()) {
            sb.append(style.center(shopAddress)).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);
        
        // [Body] - 상품 목록
        if (posItems != null && !posItems.isEmpty()) {
            for (PosItem item : posItems) {
                String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
                sb.append(description).append(NL);
                
                // ✅ 금액에 $ 추가
                String qtyPrice = String.format("  %d x %s", item.getQty(), formatCurrency(item.getSellingPrice()));
                sb.append(style.justify(qtyPrice, formatCurrency(item.getFinalAmount()))).append(NL);
            }
        } else {
            sb.append(style.center("No Items")).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);

        // [Footer] - 합계 및 결제 정보
        double totalAmount = (paymentResult != null && paymentResult.getSaleResponse() != null)
            ? paymentResult.getSaleResponse().totalAmount().doubleValue() : 0.0;
        
        // ✅ TOTAL AMOUNT에 $ 추가
        sb.append(style.justify("TOTAL AMOUNT", formatCurrency(totalAmount))).append(NL);
        
        // 결제 정보
    if (saleRequest != null && saleRequest.payments() != null) {
        for (PaymentRequest p : saleRequest.payments()) {
            double amount = p.amount().doubleValue();
            double cashout = p.cashoutAmount() != null ? p.cashoutAmount().doubleValue() : 0.0;
            
            if ("CASH".equals(p.type())) {
                // 현금 결제 - Cashout 없음
                sb.append(style.justify("CASH PAID", formatCurrency(amount))).append(NL);
                
            } else if ("CARD".equals(p.type())) {
                // 카드 결제
                if (cashout > 0) {
                    // Cashout 있는 경우
                    double totalCharged = amount + cashout;
                    sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                    sb.append(style.justify("  + CASHOUT", formatCurrency(cashout))).append(NL);
                    sb.append(style.getLine(false)).append(NL);
                    sb.append(style.justify("TOTAL CHARGED", formatCurrency(totalCharged))).append(NL);
                } else {
                    // Cashout 없는 일반 카드 결제
                    sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                }
            }
        }
    }

        // [Notice]
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getNoticeLine("Notice")).append(NL);
            sb.append(wrapText(inform, style.getWidth())).append(NL);
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
            sb.append(text, i, Math.min(i + width, text.length())).append(NL);
        }
        return sb.toString().trim();
    }
}