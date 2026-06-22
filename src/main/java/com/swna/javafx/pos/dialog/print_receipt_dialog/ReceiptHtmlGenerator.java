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

@Slf4j
@Component
public class ReceiptHtmlGenerator {

    // =========================================================================
    // Constants (상수)
    // =========================================================================
    
    private static final int LINE_WIDTH = 40;
    private static final String ROW_FORMAT = "%20s %-" + (LINE_WIDTH - 21) + "s";
    private static final String NL = "\n";
    
    /** HTML 구분선 (divider) */
    private static final String DIVIDER_HTML = "<div class='divider'></div>\n";
    
    /**
     * 영수증 하단 푸터 HTML
     * 
     * <p>구성:</p>
     * <ul>
     *   <li>환불 불가 안내</li>
     *   <li>교환 시 영수증 필요 안내</li>
     *   <li>구분선</li>
     *   <li>방문 감사 인사</li>
     * </ul>
     */
    private static final String FOOTER_HTML = """
        <div style='text-align:center; font-size:9px;'>Goods sold are not refundable</div>
        <div style='text-align:center; font-size:9px;'>For exchange, please bring receipt</div>
        <div class='divider'></div>
        <div style='text-align:center; font-size:9px;'>Thank you for your visit!</div>
        """;

    // =========================================================================
    // Public Methods
    // =========================================================================
    
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
        
        ShopInfo shopInfo = extractShopInfo(shop);
        html.append(buildShopHeader(shopInfo));
        html.append(DIVIDER_HTML);
        
        html.append(buildReceiptInfo(saleModel));
        html.append(DIVIDER_HTML);
        
        html.append(buildItemsTable(items));
        html.append(DIVIDER_HTML);
        
        html.append(buildPaymentInfo(saleModel));
        html.append(DIVIDER_HTML);
        
        // 상수 사용
        html.append(FOOTER_HTML);
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }

    // =========================================================================
    // HTML Structure Methods (Text Block - 변수 없음, 안전함)
    // =========================================================================
    
    private String buildHeader() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset='UTF-8'>
            """;
    }

    private String buildStyle() {
        return """
            <style>
            body { font-family: 'Courier New', monospace; font-size: 11px; width: 280px; margin: 0 auto; padding: 5px; }
            .header { text-align: left; margin-bottom: 5px; }
            .shop-name { font-size: 16px; font-weight: bold; }
            .divider { border-top: 1px solid #000; margin: 4px 0; }
            .row { display: flex; justify-content: space-between; margin: 1px 0; }
            .item-row { display: flex; justify-content: space-between; margin: 2px 0; font-size: 10px; }
            .items-header { display: flex; justify-content: space-between; font-weight: bold; font-size: 10px; border-bottom: 1px dashed #000; padding: 2px 0; }
            </style>
            </head>
            """;
    }

    // =========================================================================
    // Shop Information (String.format 사용 - 안전함)
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
        return String.format("""
            <div class='header'>
            <div class='shop-name'>%s</div>
            <div>%s</div>
            <div>Tel: %s</div>
            <div>GST: %s</div>
            </div>
            """,
            escapeHtml(shopInfo.getName()),
            escapeHtml(shopInfo.getAddress()),
            escapeHtml(shopInfo.getPhone()),
            escapeHtml(shopInfo.getBusinessNo())
        );
    }

    // =========================================================================
    // Receipt Information (String.format 사용 - 안전함)
    // =========================================================================
    
    private String buildReceiptInfo(SaleModel saleModel) {
        if (saleModel == null) {
            return "<div class='row'><span>No data available</span></div>\n";
        }
        
        String date = saleModel.getPaymentDateTime() != null
            ? saleModel.getPaymentDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "";
        
        return String.format("""
            <div class='row'><span>Date:</span><span>%s</span></div>
            <div class='row'><span>Receipt No:</span><span>%s</span></div>
            """,
            escapeHtml(date),
            escapeHtml(saleModel.getReceiptNo())
        );
    }

    // =========================================================================
    // Items Table (String.format 사용 - 안전함)
    // =========================================================================
    
    private String buildItemsTable(List<SaleItemModel> items) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("""
            <div class='items-header'>
            <span>Item</span><span>Price</span><span>Qty</span><span>D/C</span><span>Amount</span>
            </div>
            """);
        
        if (items == null || items.isEmpty()) {
            sb.append("<div style='text-align:center; padding:10px;'>No items found</div>\n");
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
        int quantity = getSafeInt(item.getQuantity());
        String discountDisplay = discount > 0 ? formatCurrency(discount) : "-";

        return String.format("""
            <div style='margin-top:4px'>%d. %s</div>
            <div class='item-row'>
            <span></span><span>%s</span>
            <span>%d</span>
            <span>%s</span>
            <span>%s</span>
            </div>
            """,
            index,
            escapeHtml(description),
            formatCurrency(price),
            quantity,
            discountDisplay,
            formatCurrency(amount)
        );
    }

    // =========================================================================
    // Payment Information (StringBuilder 사용 - 안전함)
    // =========================================================================
    
    private String buildPaymentInfo(SaleModel saleModel) {
        if (saleModel == null) {
            return "<div>No payment information</div>";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: monospace; white-space: pre; font-size: 12px;'>");
        
        BigDecimal saleAmount = getSafeBigDecimal(saleModel.getSaleAmount());
        BigDecimal gst = calculateGst(saleAmount);
        String finalAmountStr = String.format("%s(GST:%s)", 
            formatCurrency(saleAmount), 
            formatCurrency(gst.doubleValue()));
        sb.append(String.format(ROW_FORMAT, "Final Amount : ", finalAmountStr)).append(NL);
        
        BigDecimal discount = getSafeBigDecimal(saleModel.getDiscountAmount());
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format(ROW_FORMAT, "D/C : ", formatCurrency(discount))).append(NL);
        }
        
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
    // Utility Methods (null-safe getters)
    // =========================================================================
    
    private BigDecimal getSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double getSafeDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private int getSafeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal calculateGst(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
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