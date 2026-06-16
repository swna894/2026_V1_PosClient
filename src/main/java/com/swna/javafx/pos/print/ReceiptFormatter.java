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
        String shopName = (shop != null && shop.getName() != null) ? shop.getName().toUpperCase() : "MY STORE";
        String shopAddress = (shop != null && shop.getAddress() != null) ? shop.getAddress() : "123 Main Street, Suite 100";
        
        sb.append(shopName).append(NL);
        if (!shopAddress.isEmpty()) {
            sb.append(shopAddress).append(NL);
        }
        sb.append("Tel: (123) 456-7890").append(NL);
        sb.append("GST: 1234567890").append(NL);
        
        sb.append(style.getLine(true)).append(NL); 
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(true)).append(NL);
    }

    // ========== Body Builder ==========
    private void buildBody(StringBuilder sb, List<PosItem> posItems, ReceiptStyle style) {
        int totalWidth = style.getWidth();
        int qtyWidth = 4;    
        int priceWidth = 9;  
        int amountWidth = 11; 
        int itemWidth = totalWidth - (qtyWidth + priceWidth + amountWidth + 3); 

        String formatPattern = String.format("%%-%ds %%%ds %%%ds %%%ds", itemWidth, qtyWidth, priceWidth, amountWidth);
        String itemHeader = String.format(formatPattern, "Item", "Qty", "Price", "Amount");

        sb.append(itemHeader).append(NL);
        sb.append(style.getLine(false)).append(NL); 

        if (posItems == null || posItems.isEmpty()) {
            sb.append(style.center("No items found for this receipt")).append(NL);
            return;
        }
        
        AtomicInteger counter = new AtomicInteger(1);
        for (PosItem item : posItems) {
            int number = counter.getAndIncrement();
            String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
            
            String fullDesc = number + ". " + description;
            sb.append(fullDesc).append(NL); 
            
            String qtyStr = String.valueOf(item.getQty());
            String priceStr = formatCurrency(item.getSellingPrice());
            String amountStr = formatCurrency(item.getFinalAmount());
            
            // [수정] 각 데이터에 고정 폭(qtyWidth, priceWidth, amountWidth)을 적용하여 포맷팅
            // 각 필드 간에 1칸씩 공백을 둔다고 가정합니다.
            String infoFormat = String.format("%%%ds %%%ds %%%ds", qtyWidth, priceWidth, amountWidth);
            String formattedInfo = String.format(infoFormat, qtyStr, priceStr, amountStr);
            
            // 전체 폭(style.getWidth())에 맞춰 오른쪽 정렬
            String infoRow = String.format("%" + style.getWidth() + "s", formattedInfo);
            sb.append(infoRow).append(NL);
            
            // 할인 정보 처리 (기존과 동일)
            boolean isDiscounted = item.getOriginalPrice() != item.getSellingPrice();
            if (isDiscounted) {
                double discountPerItem = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
                if (discountPerItem > 0) {
                    sb.append(style.justify("  Discount", "-" + formatCurrency(discountPerItem))).append(NL);
                }
            }
        }

        // for (PosItem item : posItems) {
        //     int number = counter.getAndIncrement();
        //     String description = (item.getDescription() != null) ? item.getDescription() : item.getCode();
            
        //     String fullDesc = number + ". " + description;
        //     String truncatedDesc = truncate(fullDesc, itemWidth);
            
        //     String qtyStr = String.valueOf(item.getQty());
        //     String priceStr = formatCurrency(item.getSellingPrice());
        //     String amountStr = formatCurrency(item.getFinalAmount());
            
        //     String rowPattern = String.format("%%-%ds %%%ds %%%ds %%%ds", itemWidth, qtyWidth, priceWidth, amountWidth);
        //     String itemRow = String.format(rowPattern, truncatedDesc, qtyStr, priceStr, amountStr);
        //     sb.append(itemRow).append(NL);
            
        //     boolean isDiscounted = item.getOriginalPrice() != item.getSellingPrice();
        //     if (isDiscounted) {
        //         double discountPerItem = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
        //         if (discountPerItem > 0) {
        //             String discountLabel = "  Discount";
        //             String discountAmountStr = "-" + formatCurrency(discountPerItem);
        //             sb.append(style.justify(discountLabel, discountAmountStr)).append(NL);
        //         }
        //     }
        // }
        sb.append(style.getLine(true)).append(NL); 
    }

    // ========== Footer Builder ==========
    private void buildFooter(StringBuilder sb, ReceiptStyle style, 
                             double subtotal, double discountAmount, double finalAmount) {
        if (discountAmount > 0) {
            sb.append(style.justify("ORIGINAL AMOUNT", formatCurrency(subtotal))).append(NL);
            sb.append(style.justify("DISCOUNT", "-" + formatCurrency(discountAmount))).append(NL);
            sb.append(style.getLine(false)).append(NL);
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        } else {
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NL);
        }
        sb.append(style.getLine(true)).append(NL);
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
                sb.append(style.justify("CARD PAID", formatCurrency(amount))).append(NL);
                if (cashout > 0) {
                    sb.append(style.justify("CASHOUT", formatCurrency(cashout))).append(NL);
                }
            }
        }
        sb.append(style.getLine(false)).append(NL); 
    }

    // ========== Barcode Builder (★수정됨: 문자열 깨짐 원인 제거) ==========
    private void buildBarcode(StringBuilder sb, String receiptNo, ReceiptStyle style) {
        if (receiptNo == null || receiptNo.isBlank()) {
            return;
        }
        
        sb.append(NL);
        
        // [❌ 기존 변경 기계어 삭제됨] sb.append(GS).append('h').append((char)162); ... 
        // 문자열 상태에서 인코딩을 타면 유니코드 깨짐 현상이 일어나므로 하드웨어 제어 명령을 전부 걷어냈습니다.
        
        // 바코드 하단에 정렬되어 출력될 텍스트만 포맷터에 남겨둡니다.
    }

    // ========== Notice Builder ==========
    private void buildNotice(StringBuilder sb, ReceiptStyle style, String inform) {
        //sb.append(style.center("Thank you for your visit!")).append(NL);
        sb.append(style.center("Goods sold are not refundable")).append(NL);
        sb.append(style.center("For exchange, please bring receipt")).append(NL);
        
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(false)).append(NL);
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
        
        buildHeader(sb, style, receiptNo, date, shop);
        buildBody(sb, posItems, style);
        buildFooter(sb, style, subtotal, discountAmount, finalAmount);
        buildPaymentInfo(sb, saleRequest, style);
        
        buildBarcode(sb, receiptNo, style);
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