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

    private String formatCurrency(double amount) {
        return "$" + CURRENCY_DF.format(amount);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // ========== Header Builder (HTML 폼의 대문자 및 레이아웃 반영) ==========
    private void buildHeader(StringBuilder sb, ReceiptStyle style, 
                             String receiptNo, String date, Shop shop) {
        // HTML 폼의 대문자 상호명 반영
        String shopName = (shop != null && shop.getName() != null) ? shop.getName().toUpperCase() : "MY STORE";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "123 Main Street, Suite 100";
        
        sb.append(style.center(shopName)).append(NL);
        if (!shopAddress.isEmpty()) {
            sb.append(style.center(shopAddress)).append(NL);
        }
        // HTML의 고정된 기본 정보 추가 레이아웃 반영
        sb.append(style.center("Tel: (123) 456-7890")).append(NL);
        sb.append(style.center("GST: 1234567890")).append(NL);
        
        sb.append(style.getLine(false)).append(NL); // divider-solid 역할
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Body Builder (★핵심 리팩토링: HTML처럼 가로 1줄 레이아웃으로 변경) ==========
    private void buildBody(StringBuilder sb, List<PosItem> posItems, ReceiptStyle style) {
        // 1. HTML의 4컬럼 헤더 레이아웃 재현 (Item, Qty, Price, Amount)
        // 총 가로폭(예: 42자) 내에서 우측 3개 컬럼의 폭을 고정하고 나머지를 상품명에 배분
        int totalWidth = style.getWidth();
        int qtyWidth = 4;    // Qty 폭
        int priceWidth = 8;  // Price 폭
        int amountWidth = 9; // Amount 폭
        int itemWidth = totalWidth - (qtyWidth + priceWidth + amountWidth + 3); // 공백 포함 계산

        // 헤더 타이틀 한 줄 배치
        String itemHeader = String.format("%-" + itemWidth + "s %" + qtyWidth + "s %" + priceWidth + "s %" + amountWidth + "s", 
            "Item", "Qty", "Price", "Amount");
        sb.append(itemHeader).append(NL);
        sb.append(style.getLine(true)).append(NL); // divider-dash 역할

        if (posItems == null || posItems.isEmpty()) {
            sb.append(style.center("No items found for this receipt")).append(NL);
            return;
        }
        
        AtomicInteger counter = new AtomicInteger(1);
        for (PosItem item : posItems) {
            int number = counter.getAndIncrement();
            String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
            
            // "번호. 상품명" 형태로 합친 뒤 컬럼 너비에 맞춰 자르기
            String fullDesc = number + ". " + description;
            String truncatedDesc = truncate(fullDesc, itemWidth);
            
            String qtyStr = String.valueOf(item.getQty());
            String priceStr = formatCurrency(item.getSellingPrice());
            String amountStr = formatCurrency(item.getFinalAmount());
            
            // ★ HTML 폼처럼 4개 데이터가 가로로 완벽히 정렬된 1줄 레이아웃 완성
            String itemRow = String.format("%-" + itemWidth + "s %" + qtyWidth + "s %" + priceWidth + "s %" + amountWidth + "s", 
                truncatedDesc, qtyStr, priceStr, amountStr);
            sb.append(itemRow).append(NL);
            
            // 할인 정보 레이아웃 반영 (HTML의 discount-row 스타일 적용)
            boolean isDiscounted = item.getOriginalPrice() != item.getSellingPrice();
            if (isDiscounted) {
                double discountPerItem = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
                if (discountPerItem > 0) {
                    String discountLabel = "  Discount";
                    String discountAmountStr = "-" + formatCurrency(discountPerItem);
                    // 우측 정렬 형태로 맞춤
                    sb.append(style.justify(discountLabel, discountAmountStr)).append(NL);
                }
            }
        }
        sb.append(style.getLine(true)).append(NL); // divider-dash
    }

    // ========== Footer Builder (HTML의 영문 타이틀 일치) ==========
    private void buildFooter(StringBuilder sb, ReceiptStyle style, 
                             double subtotal, double discountAmount, double finalAmount) {
        
        // HTML 폼의 대문자 타이틀 텍스트와 완벽 일치 ("TOTAL AMOUNT", "ORIGINAL AMOUNT")
        if (discountAmount > 0) {
            sb.append(style.justify("ORIGINAL AMOUNT", formatCurrency(subtotal))).append(NL);
            sb.append(style.justify("DISCOUNT", "-" + formatCurrency(discountAmount))).append(NL);
            sb.append(style.getLine(true)).append(NL);
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        } else {
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        }
        sb.append(style.getLine(true)).append(NL);
    }

    // ========== Payment Builder (HTML 타이틀 매핑) ==========
    private void buildPaymentInfo(StringBuilder sb, SaleRequest saleRequest, ReceiptStyle style) {
        if (saleRequest == null || saleRequest.payments() == null || saleRequest.payments().isEmpty()) {
            return;
        }
        
        for (PaymentRequest p : saleRequest.payments()) {
            double amount = p.amount().doubleValue();
            double cashout = p.cashoutAmount() != null ? p.cashoutAmount().doubleValue() : 0.0;
            
            // HTML 폼의 명칭 문구("CASH PAID", "CARD PAID")로 1:1 매칭 수정
            if ("CASH".equals(p.type())) {
                sb.append(style.justify("CASH PAID", formatCurrency(amount))).append(NL);
            } else if ("CARD".equals(p.type())) {
                sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                if (cashout > 0) {
                    sb.append(style.justify("CASHOUT", formatCurrency(cashout))).append(NL);
                }
            }
        }
        sb.append(style.getLine(false)).append(NL); // divider-solid 역할
    }

    // ========== Barcode Builder (명령어는 유지하되 하단 텍스트 양식 일치) ==========
    private void buildBarcode(StringBuilder sb, String receiptNo, ReceiptStyle style) {
        if (receiptNo == null || receiptNo.isBlank()) {
            return;
        }
        
        sb.append(NL);
        
        // 하드웨어 바코드 인쇄 명령어 시퀀스는 기계 출력을 위해 원본 유지
        sb.append(GS).append('h').append((char)162);
        sb.append(GS).append('w').append((char)3);
        sb.append(GS).append('H').append((char)2);  
        sb.append(GS).append('k').append((char)73);  
        sb.append((char)receiptNo.length());          
        sb.append(receiptNo);                         
        sb.append((char)0);                           
        
        sb.append(NL);
        
        // ★ HTML 폼 레이아웃의 바코드 대체 텍스트 포맷인 "* 영수증번호 *" 형태로 중앙 정렬 반영
        sb.append(style.center("* " + receiptNo + " *")).append(NL);
    }

    // ========== Notice Builder (HTML 고정 푸터 문구 반영) ==========
    private void buildNotice(StringBuilder sb, ReceiptStyle style, String inform) {
        // HTML의 고정 하단 텍스트 레이아웃 구조 반영
        sb.append(style.center("** Tax Invoice **")).append(NL);
        sb.append(style.getLine(true)).append(NL);
        sb.append(style.center("Thank you for your visit!")).append(NL);
        sb.append(style.center("Goods sold are not refundable")).append(NL);
        sb.append(style.center("For exchange, please bring receipt")).append(NL);
        
        // 사용자 정의 메시지가 추가로 전송된 경우 하단에 래핑하여 추가 출력
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(true)).append(NL);
            String truncatedInform = truncate(inform, style.getWidth() * 2);
            sb.append(wrapText(truncatedInform, style.getWidth())).append(NL);
        }
    }

    // ========== Main Build Method ==========
    public String buildContent(SaleRequest saleRequest, PaymentResult paymentResult, 
                               List<PosItem> posItems, Shop shop, 
                               ReceiptStyle style, String inform) {
        
        log.info("ReceiptFormatter.buildContent() - Refactored for HTML Layout matching");
        
        StringBuilder sb = new StringBuilder();
        
        String receiptNo = extractReceiptNo(paymentResult);
        String date = formatReceiptDate(receiptNo);
        double subtotal = extractSubtotal(paymentResult, posItems);
        double discountAmount = extractDiscountAmount(paymentResult, saleRequest);
        double finalAmount = extractFinalAmount(paymentResult, subtotal, discountAmount);
        
        // 변경된 레이아웃 순서대로 빌드
        buildHeader(sb, style, receiptNo, date, shop);
        buildBody(sb, posItems, style);
        buildFooter(sb, style, subtotal, discountAmount, finalAmount);
        buildPaymentInfo(sb, saleRequest, style);
        
        // 바코드와 알림 문구 레이아웃 처리
        buildBarcode(sb, receiptNo, style);
        buildNotice(sb, style, inform);
        
        return sb.toString();
    }
    
    // Helper Methods (원본 유지)
    private String extractReceiptNo(PaymentResult paymentResult) {
        if (paymentResult == null || paymentResult.getSaleResponse() == null) return "N/A";
        return paymentResult.getSaleResponse().receiptNo();
    }
    
    private double extractSubtotal(PaymentResult paymentResult, List<PosItem> posItems) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.totalAmount() != null) return response.totalAmount().doubleValue();
        }
        if (posItems != null) {
            return posItems.stream().mapToDouble(item -> item.getOriginalPrice() * item.getQty()).sum();
        }
        return 0.0;
    }
    
    private double extractDiscountAmount(PaymentResult paymentResult, SaleRequest saleRequest) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.discountAmount() != null) return response.discountAmount().doubleValue();
        }
        if (saleRequest != null && saleRequest.discounts() != null) {
            return saleRequest.discounts().stream().mapToDouble(d -> d.value() != null ? d.value().doubleValue() : 0.0).sum();
        }
        return 0.0;
    }
    
    private double extractFinalAmount(PaymentResult paymentResult, double subtotal, double discountAmount) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.finalAmount() != null) return response.finalAmount().doubleValue();
        }
        return subtotal - discountAmount;
    }

    private String formatReceiptDate(String receiptNo) {
        try {
            if (receiptNo == null || !receiptNo.contains("_")) return LocalDateTime.now().format(DST_DTF);
            return LocalDateTime.parse(receiptNo.split("_")[0], SRC_DTF).format(DST_DTF);
        } catch (Exception e) {
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