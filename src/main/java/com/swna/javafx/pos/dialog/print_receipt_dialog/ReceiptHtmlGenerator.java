package com.swna.javafx.pos.dialog.print_receipt_dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.shop.Shop;

import lombok.extern.slf4j.Slf4j;

/**
 * 영수증 HTML 생성기
 * 
 * <p>SaleModel과 SaleItemModel 데이터를 기반으로
 * 영수증 출력용 HTML을 생성합니다.</p>
 */
@Slf4j
@Component
public class ReceiptHtmlGenerator {

    private static final int LINE_WIDTH = 40;
    private static final String ROW_FORMAT = "%20s %-" + (LINE_WIDTH - 21) + "s";
    private static final String NL = "\n";

    /**
     * 영수증 HTML 생성
     * 
     * @param saleModel 영수증 정보
     * @param items 영수증 품목 리스트
     * @param shop 매장 정보
     * @return 영수증 HTML 문자열
     */
    public String generateReceiptHTML(SaleModel saleModel, List<SaleItemModel> items, Shop shop) {
        log.info("[ReceiptHtmlGenerator] Generating HTML for receipt: {}", 
            saleModel != null ? saleModel.getReceiptNo() : "null");
        
        StringBuilder html = new StringBuilder();
        
        html.append(buildHeader());
        html.append(buildStyle());
        html.append("<body>\n");
        
        // 매장 정보
        ShopInfo shopInfo = extractShopInfo(shop);
        html.append(buildShopHeader(shopInfo));
        html.append(buildDivider());
        
        // 영수증 기본 정보
        html.append(buildReceiptInfo(saleModel));
        html.append(buildDivider());
        
        // 품목 목록
        html.append(buildItemsTable(items));
        html.append(buildDivider());
        
        // 결제 정보
        html.append(buildPaymentInfo(saleModel));
        html.append(buildDivider());
        
        // 푸터
        html.append(buildFooter());
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }

    // =========================================================================
    // HTML Structure Methods
    // =========================================================================
    
    private String buildHeader() {
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n";
    }

    private String buildStyle() {
        return "<style>\n" +
               "body { font-family: 'Courier New', monospace; font-size: 11px; " +
               "width: 280px; margin: 0 auto; padding: 5px; }\n" +
               ".header { text-align: left; margin-bottom: 5px; }\n" +
               ".shop-name { font-size: 16px; font-weight: bold; }\n" +
               ".divider { border-top: 1px solid #000; margin: 4px 0; }\n" +
               ".row { display: flex; justify-content: space-between; margin: 1px 0; }\n" +
               ".item-row { display: flex; justify-content: space-between; " +
               "margin: 2px 0; font-size: 10px; }\n" +
               ".items-header { display: flex; justify-content: space-between; " +
               "font-weight: bold; font-size: 10px; border-bottom: 1px dashed #000; " +
               "padding: 2px 0; }\n" +
               "</style>\n</head>\n";
    }

    // =========================================================================
    // Shop Information
    // =========================================================================
    
    private ShopInfo extractShopInfo(Shop shop) {
        if (shop == null) {
            return ShopInfo.defaultInfo();
        }
        return ShopInfo.builder()
            .name(getSafeValue(shop.getCompany(), "MY STORE").toUpperCase())
            .address(getSafeValue(shop.getAddress(), "123 Main Street, Suite 100"))
            .phone(getSafeValue(shop.getPhone(), "(123) 456-7890"))
            .businessNo(getSafeValue(shop.getBusinessNo(), "1234567890"))
            .build();
    }

    private String getSafeValue(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private String buildShopHeader(ShopInfo shopInfo) {
        return "<div class='header'>\n" +
               "<div class='shop-name'>" + escapeHtml(shopInfo.getName()) + "</div>\n" +
               "<div>" + escapeHtml(shopInfo.getAddress()) + "</div>\n" +
               "<div>Tel: " + escapeHtml(shopInfo.getPhone()) + "</div>\n" +
               "<div>GST: " + escapeHtml(shopInfo.getBusinessNo()) + "</div>\n" +
               "</div>\n";
    }

    // =========================================================================
    // Receipt Information
    // =========================================================================
    
    private String buildReceiptInfo(SaleModel saleModel) {
        if (saleModel == null) {
            return "<div class='row'><span>No data available</span></div>\n";
        }
        
        String date = saleModel.getPaymentDateTime() != null
            ? saleModel.getPaymentDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "";
        
        return "<div class='row'><span>Date:</span><span>" + escapeHtml(date) + "</span></div>\n" +
               "<div class='row'><span>Receipt No:</span><span>" + 
               escapeHtml(saleModel.getReceiptNo()) + "</span></div>\n";
    }

    // =========================================================================
    // Items Table
    // =========================================================================
    
    private String buildItemsTable(List<SaleItemModel> items) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<div class='items-header'>\n");
        sb.append("<span>Item</span><span>Price</span><span>Qty</span>");
        sb.append("<span>D/C</span><span>Amount</span>\n");
        sb.append("</div>\n");
        
        if (items == null || items.isEmpty()) {
            sb.append("<div style='text-align:center; padding:10px;'>");
            sb.append("No items found</div>\n");
            return sb.toString();
        }
        
        for (int i = 0; i < items.size(); i++) {
            SaleItemModel item = items.get(i);
            sb.append(buildItemRow(i + 1, item));
        }
        
        return sb.toString();
    }

    private String buildItemRow(int index, SaleItemModel item) {
        double price = getSafeDouble(item.getSalePrice());
        double discount = getSafeDouble(item.getDiscountAmount());
        double amount = getSafeDouble(item.getSaleAmount());
        String description = getSafeValue(item.getDescription(), "Unknown Item");
        int quantity = item.getQuantity();

        return "<div style='margin-top:4px'>" + 
               index + ". " + escapeHtml(description) + "</div>\n" +
               "<div class='item-row'>\n" +
               "<span></span><span>" + formatCurrency(price) + "</span>" +
               "<span>" + quantity + "</span>" +
               "<span>" + (discount > 0 ? formatCurrency(discount) : "-") + "</span>" +
               "<span>" + formatCurrency(amount) + "</span>\n" +
               "</div>\n";
    }

    // =========================================================================
    // Payment Information
    // =========================================================================
    
    private String buildPaymentInfo(SaleModel saleModel) {
        if (saleModel == null) {
            return "<div>No payment information</div>\n";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: monospace; white-space: pre; font-size: 12px;'>");
        
        // 총액 및 GST
        BigDecimal saleAmount = getSafeBigDecimal(saleModel.getSaleAmount());
        BigDecimal gst = calculateGst(saleAmount);
        String finalAmountStr = String.format("%s (GST: %s)", 
            formatCurrency(saleAmount), 
            formatCurrency(gst.doubleValue()));
        sb.append(String.format(ROW_FORMAT, "Final Amount : ", finalAmountStr)).append(NL);
        
        // 할인 정보
        BigDecimal discount = getSafeBigDecimal(saleModel.getDiscountAmount());
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format(ROW_FORMAT, "D/C : ", formatCurrency(discount))).append(NL);
        }
        
        // 결제 방식별 정보
        String paymentType = saleModel.getPaymentType();
        sb.append(buildPaymentDetails(paymentType, saleModel));
        
        sb.append("</div>\n");
        return sb.toString();
    }

    private String buildPaymentDetails(String paymentType, SaleModel saleModel) {
        StringBuilder sb = new StringBuilder();
        BigDecimal cashAmount = getSafeBigDecimal(saleModel.getCashAmount());
        BigDecimal cashoutAmount = getSafeBigDecimal(saleModel.getCashoutAmount());
        BigDecimal creditAmount = getSafeBigDecimal(saleModel.getCreditAmount());
        BigDecimal receivedAmount = getSafeBigDecimal(saleModel.getReceivedAmount());
        BigDecimal changeAmount = getSafeBigDecimal(saleModel.getChangeAmount());

        if ("CASH".equalsIgnoreCase(paymentType)) {
            sb.append(String.format(ROW_FORMAT, "Cash Paid : ", 
                formatCurrency(receivedAmount))).append(NL);
            if (changeAmount.doubleValue() > 0) {
                sb.append(String.format(ROW_FORMAT, "Balance : ", 
                    formatCurrency(changeAmount.doubleValue()))).append(NL);
            }
        }
        
        if (paymentType != null && paymentType.contains("CASHOUT")) {
            sb.append(String.format(ROW_FORMAT, "EFT : ", 
                formatCurrency(creditAmount))).append(NL);
            if (cashoutAmount.doubleValue() > 0) {
                sb.append(String.format(ROW_FORMAT, "Cash Out : ", 
                    formatCurrency(cashoutAmount))).append(NL);
            }
        }
        
        if (paymentType != null && paymentType.contains("CARD")) {
            sb.append(String.format(ROW_FORMAT, "EFT : ", 
                formatCurrency(receivedAmount))).append(NL);
            if (cashAmount.doubleValue() > 0) {
                sb.append(String.format(ROW_FORMAT, "Cash Paid : ", 
                    formatCurrency(cashAmount))).append(NL);
            }
        }
        
        return sb.toString();
    }

    // =========================================================================
    // Footer
    // =========================================================================
    
    private String buildFooter() {
        return "<div style='text-align:center; font-size:9px;'>" +
               "Goods sold are not refundable</div>\n" +
               "<div style='text-align:center; font-size:9px;'>" +
               "For exchange, please bring receipt</div>\n" +
               "<div style='text-align:center; font-size:9px;'>" +
               "Thank you for your visit!</div>\n";
    }

    private String buildDivider() {
        return "<div class='divider'></div>\n";
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    private BigDecimal calculateGst(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        // GST = 금액 - (금액 / 1.15)
        return amount.subtract(amount.divide(BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal getSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double getSafeDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? formatCurrency(amount.doubleValue()) : "$0.00";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // =========================================================================
    // Inner Class: ShopInfo
    // =========================================================================
    
    @lombok.Builder
    @lombok.Getter
    private static class ShopInfo {
        private final String name;
        private final String address;
        private final String phone;
        private final String businessNo;

        public static ShopInfo defaultInfo() {
            return ShopInfo.builder()
                .name("MY STORE")
                .address("123 Main Street, Suite 100")
                .phone("(123) 456-7890")
                .businessNo("1234567890")
                .build();
        }
    }
}
