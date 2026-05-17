package com.swna.javafx.pos.print;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.dto.response.SaleResponse;
import com.swna.javafx.pos.model.PosItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReceiptFormatter {
    private static final String NL = "\n";
    private static final DecimalFormat CURRENCY_DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SRC_DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DST_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // ESC/POS 바코드 제어 명령어
    private static final char GS = 0x1D;   // Group Separator

    // ========== Utility Methods ==========
    
    private String formatCurrency(double amount) {
        return "$" + CURRENCY_DF.format(amount);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private String truncateForStyle(String text, ReceiptStyle style) {
        int maxLength = style.getWidth() - 6;
        return truncate(text, maxLength);
    }

    // ========== Header Builder ==========
    
    private void buildHeader(StringBuilder sb, ReceiptStyle style, 
                             String receiptNo, String date, Shop shop) {
        String shopName = (shop != null && shop.getName() != null) ? shop.getName() : "My Store";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "";
        
        if (shopName.length() > style.getWidth()) {
            shopName = truncate(shopName, style.getWidth());
        }
        
        sb.append(style.center(shopName)).append(NL);
        if (!shopAddress.isEmpty()) {
            if (shopAddress.length() > style.getWidth()) {
                shopAddress = truncate(shopAddress, style.getWidth());
            }
            sb.append(style.center(shopAddress)).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Body Builder ==========
    
    private void buildBody(StringBuilder sb, List<PosItem> posItems, ReceiptStyle style) {
        if (posItems == null || posItems.isEmpty()) {
            sb.append(style.center("No Items")).append(NL);
            return;
        }
        
        AtomicInteger counter = new AtomicInteger(1);
        for (PosItem item : posItems) {
            int number = counter.getAndIncrement();
            String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
            String truncatedDesc = truncateForStyle(description, style);
            
            // 제품명 표시
            sb.append(String.format("%d. %s", number, truncatedDesc)).append(NL);
            
            // 할인된 제품인지 확인 (원래 가격과 판매 가격이 다른 경우)
            boolean isDiscounted = item.getOriginalPrice() != item.getSellingPrice();
            
            if (isDiscounted) {
                // 할인된 제품: 원래 가격을 옆에 표시
                String qtyPriceLine = String.format("    %d x %s (was %s)", 
                    item.getQty(), 
                    formatCurrency(item.getSellingPrice()),
                    formatCurrency(item.getOriginalPrice()));
                sb.append(style.justify(qtyPriceLine, formatCurrency(item.getFinalAmount()))).append(NL);
            } else {
                // 일반 제품
                String qtyPriceLine = String.format("    %d x %s", 
                    item.getQty(), formatCurrency(item.getSellingPrice()));
                sb.append(style.justify(qtyPriceLine, formatCurrency(item.getFinalAmount()))).append(NL);
            }
        }
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Footer Builder ==========
    
    private void buildFooter(StringBuilder sb, ReceiptStyle style, 
                             double subtotal, double discountAmount, double finalAmount) {
        
        if (discountAmount > 0) {
            // 할인이 있는 경우: ORIGINAL AMOUNT 표시
            sb.append(style.justify("ORIGINAL AMOUNT", formatCurrency(subtotal))).append(NL);
            sb.append(style.justify("DISCOUNT", "-" + formatCurrency(discountAmount))).append(NL);
            sb.append(style.getLine(false)).append(NL);
            // TOTAL AMOUNT = 최종 결제 금액 (subtotal - discount)
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        } else {
            // 할인이 없는 경우: TOTAL AMOUNT만 표시
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        }
    }

    // ========== Payment Builder ==========
    
    private void buildPaymentInfo(StringBuilder sb, SaleRequest saleRequest, ReceiptStyle style) {
        if (saleRequest == null || saleRequest.payments() == null || saleRequest.payments().isEmpty()) {
            return;
        }
        
        for (PaymentRequest p : saleRequest.payments()) {
            double amount = p.amount().doubleValue();
            double cashout = p.cashoutAmount() != null ? p.cashoutAmount().doubleValue() : 0.0;
            
            if ("CASH".equals(p.type())) {
                sb.append(style.justify("CASH PAID", formatCurrency(amount))).append(NL);
            } else if ("CARD".equals(p.type())) {
                if (cashout > 0) {
                    sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                    sb.append(style.justify("CASHOUT", formatCurrency(cashout))).append(NL);
                } else {
                    sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                }
            }
        }
    }

    // ========== Barcode Builder (추가됨) ==========
    
    /**
     * ESC/POS 명령어로 Code128 바코드 생성
     * @param sb StringBuilder
     * @param receiptNo 영수증 번호 (바코드 데이터)
     * @param style 스타일
     */
    private void buildBarcode(StringBuilder sb, String receiptNo, ReceiptStyle style) {
        if (receiptNo == null || receiptNo.isBlank()) {
            return;
        }
        
        // 바코드 아래에 표시할 여백
        sb.append(NL);
        
        // ESC/POS 바코드 명령어 시퀀스
        // GS k m n [d1...dk] NUL
        // m=73 : CODE128
        
        // 1. 바코드 높이 설정 (기본값 162, 선택사항)
        sb.append(GS).append('h').append((char)162);
        
        // 2. 바코드 너비 설정 (2 ~ 6, 기본값 3)
        sb.append(GS).append('w').append((char)3);
        
        // 3. 바코드 인쇄 위치 아래에 텍스트 표시 (HRI)
        sb.append(GS).append('H').append((char)2);  // 2 = 아래에 표시
        
        // 4. Code128 바코드 인쇄 명령
        sb.append(GS).append('k').append((char)73);  // GS k 73 = Code128
        sb.append((char)receiptNo.length());          // 데이터 길이
        sb.append(receiptNo);                         // 바코드 데이터
        sb.append((char)0);                           // NUL 종료
        
        sb.append(NL);
        
        // 바코드 아래에 텍스트로도 receiptNo 표시 (선택사항, 인식 실패 시 대비)
        sb.append(style.center(receiptNo)).append(NL);
    }

    // ========== Notice Builder ==========
    
    private void buildNotice(StringBuilder sb, ReceiptStyle style, String inform) {
        if (inform == null || inform.isBlank()) {
            // 기본 감사 메시지
            sb.append(style.getNoticeLine("Notice")).append(NL);
            sb.append(style.center("Thank you for your visit!")).append(NL);
            return;
        }
        
        // 사용자 정의 메시지가 있는 경우
        String truncatedInform = truncate(inform, style.getWidth() * 2);
        sb.append(style.getNoticeLine("Notice")).append(NL);
        sb.append(wrapText(truncatedInform, style.getWidth())).append(NL);
    }

    // ========== Main Build Method ==========
    
    public String buildContent(SaleRequest saleRequest, PaymentResult paymentResult, 
                               List<PosItem> posItems, Shop shop, 
                               ReceiptStyle style, String inform) {
        
        log.info("ReceiptFormatter.buildContent() - posItems size: {}", 
            posItems != null ? posItems.size() : 0);
        
        StringBuilder sb = new StringBuilder();
        
        // 데이터 추출
        String receiptNo = extractReceiptNo(paymentResult);
        String date = formatReceiptDate(receiptNo);
        double subtotal = extractSubtotal(paymentResult, posItems);
        double discountAmount = extractDiscountAmount(paymentResult, saleRequest);
        double finalAmount = extractFinalAmount(paymentResult, subtotal, discountAmount);
        
        // 섹션별 빌드
        buildHeader(sb, style, receiptNo, date, shop);
        buildBody(sb, posItems, style);
        buildFooter(sb, style, subtotal, discountAmount, finalAmount);
        buildPaymentInfo(sb, saleRequest, style);
        buildNotice(sb, style, inform);
        
        // 바코드 추가 (영수증 하단)
        sb.append(style.getLine(false)).append(NL);
        buildBarcode(sb, receiptNo, style);
        
        String result = sb.toString();
        log.info("Content built, length: {} chars", result.length());
        
        return result;
    }
    
    // ========== Helper Extraction Methods ==========
    
    private String extractReceiptNo(PaymentResult paymentResult) {
        if (paymentResult == null || paymentResult.getSaleResponse() == null) {
            return "N/A";
        }
        return paymentResult.getSaleResponse().receiptNo();
    }
    
    private double extractSubtotal(PaymentResult paymentResult, List<PosItem> posItems) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.totalAmount() != null) {
                return response.totalAmount().doubleValue();
            }
        }
        
        if (posItems != null) {
            return posItems.stream()
                .mapToDouble(item -> item.getOriginalPrice() * item.getQty())
                .sum();
        }
        return 0.0;
    }
    
    private double extractDiscountAmount(PaymentResult paymentResult, SaleRequest saleRequest) {
        // SaleResponse에서 discountAmount 가져오기
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.discountAmount() != null) {
                return response.discountAmount().doubleValue();
            }
        }
        
        // SaleRequest의 discounts에서 계산 - DiscountRequest는 value() 필드 사용
        if (saleRequest != null && saleRequest.discounts() != null) {
            return saleRequest.discounts().stream()
                .mapToDouble(d -> d.value() != null ? d.value().doubleValue() : 0.0)
                .sum();
        }
        return 0.0;
    }
    
    private double extractFinalAmount(PaymentResult paymentResult, double subtotal, double discountAmount) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.finalAmount() != null) {
                return response.finalAmount().doubleValue();
            }
        }
        
        return subtotal - discountAmount;
    }

    private String formatReceiptDate(String receiptNo) {
        try {
            if (receiptNo == null || !receiptNo.contains("_")) {
                return LocalDateTime.now().format(DST_DTF);
            }
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