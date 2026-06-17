package com.swna.javafx.pos.print;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    // 제어 코드 상수 정의
    private static final String ESC = "\u001B";
    private static final String BOLD_ON = ESC + "E" + (char) 1; // 굵게 시작: ESC E 1
    private static final String BOLD_OFF = ESC + "E" + (char) 0; // 굵게 종료: ESC E 0
    private static final String NL = "\n";

    private static final DecimalFormat CURRENCY_DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SRC_DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter DST_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String formatCurrency(double amount) {
        return "$" + CURRENCY_DF.format(amount);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // ========== Header Builder ==========
    private void buildHeader(StringBuilder sb, ReceiptStyle style, 
                             String receiptNo, String date, Shop shop) {
        String shopName = (shop != null && shop.getCompany() != null) ? shop.getCompany().toUpperCase() : "MY STORE";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "123 Main Street, Suite 100";
        String phone = (shop != null && shop.getPhone() != null) ? shop.getPhone() : "";
        String bussinessNo = (shop != null && shop.getBusinessNo() != null) ? shop.getBusinessNo() : "123 Main Street, Suite 100";

        
        // 1. 굵게 + 크게 설정 적용
        sb.append(BOLD_ON);
        sb.append(shopName).append(NL);
        
        // 2. 스타일 원복 (다음 라인부터는 기본 폰트로 출력되도록)
        sb.append(BOLD_OFF);
        if (!shopAddress.isEmpty()) {
            sb.append(shopAddress).append(NL);
        }
        sb.append("Tel: " + phone).append(NL);
        sb.append("GST: " + bussinessNo).append(NL);
        
        sb.append(style.getLine(true)).append(NL); 
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Body Builder ==========
    private void buildBody(StringBuilder sb, List<PosItem> posItems, ReceiptStyle style) {
        int totalWidth = style.getWidth();
        int qtyWidth = 4;
        int priceWidth = 7;
        int discountWidth = 7;
        int amountWidth = 7;
        int itemWidth = totalWidth - (qtyWidth + priceWidth + discountWidth + amountWidth + 4);

        // 헤더 패턴 정의 (재사용 가능하게 변수화)
        String rowFormat = String.format("%%-%ds %%%ds %%%ds %%%ds  %%%ds", itemWidth, priceWidth, qtyWidth, discountWidth,  amountWidth);
        
        sb.append(String.format(rowFormat, "Item", "Price", "Qty", "D/C",  "Amount")).append(NL);
        sb.append(style.getLine(false)).append(NL);

        if (posItems == null || posItems.isEmpty()) {
            sb.append(style.center("No items found for this receipt")).append(NL);
            return;
        }

        int index = 1;

        for (PosItem item : posItems) {
            // 1. 항목명 출력
            String headerRow = String.format("%2d. %s", index++, item.getDescription());
            sb.append(headerRow).append(NL);

            // 2. 할인 계산 (할인이 없으면 "-", 있으면 금액 표시)
            double discount = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
            String dcStr = (discount > 0) ? formatCurrency(discount) : "-";
            
            // 3. 한 줄로 데이터 포맷팅
            // rowFormat은 상단에서 정의한: String.format
            String infoRow = String.format(rowFormat, 
                "",                                     // Item 자리는 비움
                formatCurrency(item.getSellingPrice()), // Price
                String.valueOf(item.getQty()),          // Qty
                dcStr,                                  // DC ("-" 또는 금액)
                formatCurrency(item.getFinalAmount())   // Amount
            );
            
            sb.append(infoRow).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Footer Builder ==========
    private void buildFooter(StringBuilder sb, ReceiptStyle style, 
                             double subtotal, double discountAmount, double finalAmount) {
        if (discountAmount > 0) {
            sb.append(style.justify("ORIGINAL AMOUNT", formatCurrency(subtotal))).append(NL);
            sb.append(style.justify("DISCOUNT", "-" + formatCurrency(discountAmount))).append(NL);
        } else {
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);
    }

    // ========== Payment Builder ==========
private void buildPaymentInfo(StringBuilder sb, SaleRequest saleRequest, ReceiptStyle style) {
    if (saleRequest == null || saleRequest.payments() == null || saleRequest.payments().isEmpty()) {
        return;
    }
  
    int width = style.getWidth();
    // 왼쪽 칸(항목)은 우측 정렬(15자), 오른쪽 칸(금액)은 좌측 정렬(나머지 너비)
    String rowFormat = "%20s %-" + (width - 21) + "s";

    // 1. 계산 로직
    BigDecimal totalAmount = saleRequest.payments().stream()
            .map(PaymentRequest::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info("========================================= {}", saleRequest);
    BigDecimal totalReceived = saleRequest.payments().stream()
            .map(p -> p.receivedAmount() != null ? p.receivedAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal balance = totalReceived.subtract(totalAmount);

    // GST 추출 (15% 포함 시: 총액 / 1.15 * 0.15)
    BigDecimal gst = totalAmount.subtract(totalAmount.divide(BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP));

    
    // Final Amount에 GST 포함하여 한 줄로 표시
    String finalAmountStr = String.format("%s (GST: %s)", 
                                         formatCurrency(totalAmount.doubleValue()), 
                                         formatCurrency(gst.doubleValue()));
    sb.append(String.format(rowFormat, "Final Amount : ", finalAmountStr)).append(NL);
    
    // 3. 결제 상세 출력
    for (PaymentRequest p : saleRequest.payments()) {
        String type = p.type().toUpperCase();
        double amount = p.amount().doubleValue();
        double cashoutAmount = p.cashoutAmount().doubleValue();

        if(saleRequest.payments().size() == 1) {
                if ("CASH".equalsIgnoreCase(type)) {
                    sb.append(String.format(rowFormat, "Received : ", formatCurrency(p.receivedAmount().doubleValue()))).append(NL);
                    sb.append(String.format(rowFormat, "Cash Paid : ", formatCurrency(p.amount().doubleValue()))).append(NL);
                    sb.append(String.format(rowFormat, "Balance : ", formatCurrency(balance.doubleValue()))).append(NL);
                } 
                if ("CASHOUT".equalsIgnoreCase(type)) {
                    sb.append(String.format(rowFormat, "EFT : ", formatCurrency(amount))).append(NL);
                    sb.append(String.format(rowFormat, "Cash Out : ", formatCurrency(cashoutAmount))).append(NL);
                }

        } else {
                if ("CARD".equals(type)) {
                    sb.append(String.format(rowFormat, "EFT : ", formatCurrency(amount))).append(NL);
                }
                if ("CASH".equals(type)) {
                    sb.append(String.format(rowFormat, "Cash Paid :", formatCurrency(amount))).append(NL);
                } 
        } 
    }
    sb.append(style.getLine(true)).append(NL);
}

    // ========== Notice Builder ==========
    private void buildNotice(StringBuilder sb, ReceiptStyle style, String inform) {
        sb.append(style.center("Goods sold are not refundable")).append(NL);
        sb.append(style.center("For exchange, please bring receipt")).append(NL);
        
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(false)).append(NL);
            String truncatedInform = truncate(inform, style.getWidth() * 2);
            // 1. wrapText로 줄바꿈된 텍스트를 가져옵니다.
            String wrappedText = wrapText(truncatedInform, style.getWidth());
            
            // 2. 줄바꿈 기준(\n)으로 텍스트를 나누어 각각 중앙 정렬합니다.
            String[] lines = wrappedText.split(NL);
            for (String line : lines) {
                sb.append(style.center(line)).append(NL);
            }
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
        
        buildHeader(sb, style, receiptNo, date, shop);
        buildBody(sb, posItems, style);
        buildFooter(sb, style, subtotal, discountAmount, finalAmount);
        buildPaymentInfo(sb, saleRequest, style);
        
        buildNotice(sb, style, inform);
        
        return sb.toString();
    }
    
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