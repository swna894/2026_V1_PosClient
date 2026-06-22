package com.swna.javafx.pos.print;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
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

    // =========================================================================
    // ESC/POS Control Codes
    // =========================================================================
    
    private static final String ESC = "\u001B";
    private static final String BOLD_ON = ESC + "E" + (char) 1;
    private static final String BOLD_OFF = ESC + "E" + (char) 0;
    private static final String NEW_LINE = "\n";

    // =========================================================================
    // Payment Type Constants
    // =========================================================================
    
    private static final String PAYMENT_TYPE_CASH = "CASH";
    private static final String PAYMENT_TYPE_CARD = "CARD";
    private static final String PAYMENT_TYPE_CASHOUT = "CASHOUT";

    // =========================================================================
    // Payment Label Constants
    // =========================================================================
    
    private static final String LABEL_EFT = "EFT : ";
    private static final String LABEL_CASH_PAID = "Cash Paid : ";
    private static final String LABEL_BALANCE = "Balance : ";
    private static final String LABEL_CASH_OUT = "Cash Out : ";
    private static final String LABEL_FINAL_AMOUNT = "Final Amount : ";
    private static final String LABEL_ORIGINAL_AMOUNT = "ORIGINAL AMOUNT";
    private static final String LABEL_DISCOUNT = "DISCOUNT";
    private static final String LABEL_TOTAL_AMOUNT = "TOTAL AMOUNT";

    // =========================================================================
    // Default Values
    // =========================================================================
    
    private static final String DEFAULT_SHOP_NAME = "MY STORE";
    private static final String DEFAULT_SHOP_ADDRESS = "123 Main Street, Suite 100";
    private static final String ZERO_AMOUNT = "$0.00";

    // =========================================================================
    // Formatters (통화 기호가 패턴에 포함됨)
    // =========================================================================
    
    /** 통화 포맷터 - 패턴에 $ 기호 포함 */
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("$#,##0.00");
    
    private static final DateTimeFormatter SOURCE_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // Column Width Constants
    // =========================================================================
    
    private static final int QTY_COLUMN_WIDTH = 9;
    private static final int PRICE_COLUMN_WIDTH = 6;
    private static final int DISCOUNT_COLUMN_WIDTH = 9;
    private static final int AMOUNT_COLUMN_WIDTH = 9;
    private static final int ITEM_COLUMN_OFFSET = 5;

    // =========================================================================
    // Public Main Methods
    // =========================================================================
    
    public String buildReceiptContent(SaleRequest saleRequest, PaymentResult paymentResult,
                                      List<PosItem> posItems, Shop shop,
                                      ReceiptStyle style, String inform) {
        log.info("영수증 생성 시작 (SaleRequest 기반)");
        ReceiptData data = extractReceiptData(saleRequest, paymentResult, posItems);
        return buildReceiptContentInternal(data, shop, style, inform);
    }

    public String buildReceiptContent(SaleModel saleModel, List<SaleItemModel> itemModels,
                                      Shop shop, ReceiptStyle style, String inform) {
        log.info("영수증 생성 시작 (SaleModel 기반) - 영수증 번호: {}", 
            saleModel != null ? saleModel.getReceiptNo() : "null");
        ReceiptData data = extractReceiptData(saleModel, itemModels);
        return buildReceiptContentInternal(data, shop, style, inform);
    }

    // =========================================================================
    // Internal Receipt Build Method
    // =========================================================================
    
    private String buildReceiptContentInternal(ReceiptData data, Shop shop,
                                               ReceiptStyle style, String inform) {
        StringBuilder receipt = new StringBuilder();

        buildHeaderSection(receipt, data, shop, style);
        buildBodySection(receipt, data, style);
        buildFooterSection(receipt, data, style);
        buildPaymentSection(receipt, data, style);
        buildNoticeSection(receipt, style, inform);

        log.info("영수증 생성 완료 - 영수증 번호: {}", data.getReceiptNo());
        return receipt.toString();
    }

    // =========================================================================
    // Data Extraction Methods
    // =========================================================================
    
    private ReceiptData extractReceiptData(SaleRequest saleRequest, 
                                           PaymentResult paymentResult,
                                           List<PosItem> posItems) {
        String receiptNo = extractReceiptNumber(paymentResult);
        String date = formatReceiptDate(receiptNo);
        double subtotal = extractSubtotal(paymentResult, posItems);
        double discountAmount = extractDiscountAmount(paymentResult, saleRequest);
        double finalAmount = extractFinalAmount(paymentResult, subtotal, discountAmount);

        return ReceiptData.builder()
            .receiptNo(receiptNo)
            .date(date)
            .subtotal(subtotal)
            .discountAmount(discountAmount)
            .finalAmount(finalAmount)
            .saleRequest(saleRequest)
            .posItems(posItems)
            .build();
    }

    private ReceiptData extractReceiptData(SaleModel saleModel, 
                                           List<SaleItemModel> itemModels) {
        String receiptNo = saleModel.getReceiptNo();
        String date = saleModel.getFormattedPaymentDateTime();
        double subtotal = getSafeDouble(saleModel.getSaleAmount());
        double discountAmount = getSafeDouble(saleModel.getDiscountAmount());
        double finalAmount = getSafeDouble(saleModel.getSaleAmount());

        return ReceiptData.builder()
            .receiptNo(receiptNo)
            .date(date)
            .subtotal(subtotal)
            .discountAmount(discountAmount)
            .finalAmount(finalAmount)
            .saleModel(saleModel)
            .saleItemModels(itemModels)
            .build();
    }

    private String extractReceiptNumber(PaymentResult paymentResult) {
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
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.discountAmount() != null) {
                return response.discountAmount().doubleValue();
            }
        }

        if (saleRequest != null && saleRequest.discounts() != null) {
            return saleRequest.discounts().stream()
                .mapToDouble(d -> d.value() != null ? d.value().doubleValue() : 0.0)
                .sum();
        }

        return 0.0;
    }

    private double extractFinalAmount(PaymentResult paymentResult, 
                                      double subtotal, double discountAmount) {
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
                return LocalDateTime.now().format(DISPLAY_DATE_FORMATTER);
            }
            String datePart = receiptNo.split("_")[0];
            LocalDateTime dateTime = LocalDateTime.parse(datePart, SOURCE_DATE_FORMATTER);
            return dateTime.format(DISPLAY_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("영수증 날짜 파싱 실패: {}", receiptNo, e);
            return LocalDateTime.now().format(DISPLAY_DATE_FORMATTER);
        }
    }

    // =========================================================================
    // Section Builders
    // =========================================================================
    
    private void buildHeaderSection(StringBuilder sb, ReceiptData data, 
                                    Shop shop, ReceiptStyle style) {
        String shopName = extractShopName(shop);
        String shopAddress = extractShopAddress(shop);
        String phoneNumber = extractPhoneNumber(shop);
        String businessNumber = extractBusinessNumber(shop);

        sb.append(BOLD_ON)
          .append(shopName).append(NEW_LINE)
          .append(BOLD_OFF);

        appendShopDetails(sb, shopAddress, phoneNumber, businessNumber);

        sb.append(style.getLine(true)).append(NEW_LINE);
        sb.append(style.justify("Date:", data.getDate())).append(NEW_LINE);
        sb.append(style.justify("Receipt No:", data.getReceiptNo())).append(NEW_LINE);
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    private void buildBodySection(StringBuilder sb, ReceiptData data, ReceiptStyle style) {
        int totalWidth = style.getWidth();
        int itemWidth = calculateItemWidth(totalWidth);
        String rowFormat = createRowFormat(itemWidth);
        
        sb.append(String.format(rowFormat, "Item", "Price", "Qty", "D/C", "Amount")).append(NEW_LINE);
        sb.append(style.getLine(false)).append(NEW_LINE);

        if (data.isEmpty()) {
            sb.append(style.center("No items found for this receipt")).append(NEW_LINE);
            return;
        }

        printItems(sb, data, rowFormat);
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    private void buildFooterSection(StringBuilder sb, ReceiptData data, ReceiptStyle style) {
        if (data.getDiscountAmount() > 0) {
            sb.append(style.justify(LABEL_ORIGINAL_AMOUNT, 
                formatCurrency(data.getSubtotal() + data.getDiscountAmount()))).append(NEW_LINE);
            sb.append(style.justify(LABEL_DISCOUNT, 
                "-" + formatCurrency(data.getDiscountAmount()))).append(NEW_LINE);
        } else {
            sb.append(style.justify(LABEL_TOTAL_AMOUNT, 
                formatCurrency(data.getFinalAmount()))).append(NEW_LINE);
        }
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    private void buildPaymentSection(StringBuilder sb, ReceiptData data, ReceiptStyle style) {
        if (data.hasSaleRequest()) {
            buildPaymentSectionFromRequest(sb, data.getSaleRequest(), style);
        } else if (data.hasSaleModel()) {
            buildPaymentSectionFromModel(sb, data.getSaleModel(), style);
        }
    }

    private void buildNoticeSection(StringBuilder sb, ReceiptStyle style, String inform) {
        sb.append(style.center("Goods sold are not refundable")).append(NEW_LINE);
        sb.append(style.center("For exchange, please bring receipt")).append(NEW_LINE);

        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(false)).append(NEW_LINE);
            appendInformMessage(sb, inform, style);
        }
    }

    // =========================================================================
    // Payment Section Builders
    // =========================================================================
    
    private void buildPaymentSectionFromRequest(StringBuilder sb, SaleRequest saleRequest, 
                                                ReceiptStyle style) {
        if (saleRequest == null || saleRequest.payments() == null || saleRequest.payments().isEmpty()) {
            return;
        }

        int width = style.getWidth();
        String rowFormat = "%20s %-" + (width - 21) + "s";

        BigDecimal totalAmount = calculatePaymentTotal(saleRequest, PaymentRequest::amount);
        BigDecimal totalReceived = calculatePaymentTotal(saleRequest, 
            p -> p.receivedAmount() != null ? p.receivedAmount() : BigDecimal.ZERO);
        BigDecimal balance = totalReceived.subtract(totalAmount);
        BigDecimal gst = calculateGst(totalAmount);

        String finalAmountDisplay = String.format("%s (GST: %s)",
            formatCurrency(totalAmount.doubleValue()),
            formatCurrency(gst.doubleValue()));
        sb.append(String.format(rowFormat, LABEL_FINAL_AMOUNT, finalAmountDisplay)).append(NEW_LINE);

        for (PaymentRequest payment : saleRequest.payments()) {
            appendPaymentDetail(sb, payment, balance, rowFormat);
        }

        sb.append(style.getLine(true)).append(NEW_LINE);
    }

    private void buildPaymentSectionFromModel(StringBuilder sb, SaleModel saleModel, 
                                              ReceiptStyle style) {
        if (saleModel == null) {
            return;
        }

        int width = style.getWidth();
        String rowFormat = "%20s %-" + (width - 21) + "s";

        BigDecimal totalAmount = getSafeBigDecimal(saleModel.getSaleAmount());
        BigDecimal gst = calculateGst(totalAmount);

        String finalAmountDisplay = String.format("%s (GST: %s)",
            formatCurrency(totalAmount.doubleValue()),
            formatCurrency(gst.doubleValue()));
        sb.append(String.format(rowFormat, LABEL_FINAL_AMOUNT, finalAmountDisplay)).append(NEW_LINE);

        sb.append(buildPaymentDetailsFromModel(saleModel, rowFormat));

        sb.append(style.getLine(true)).append(NEW_LINE);
    }

    // =========================================================================
    // Helper Methods for Body Section
    // =========================================================================
    
    private int calculateItemWidth(int totalWidth) {
        return totalWidth - (QTY_COLUMN_WIDTH + PRICE_COLUMN_WIDTH + 
                             DISCOUNT_COLUMN_WIDTH + AMOUNT_COLUMN_WIDTH + ITEM_COLUMN_OFFSET);
    }

    private String createRowFormat(int itemWidth) {
        return String.format("%%-%ds %%%ds %%%ds %%%ds  %%%ds",
            itemWidth, PRICE_COLUMN_WIDTH, QTY_COLUMN_WIDTH, 
            DISCOUNT_COLUMN_WIDTH, AMOUNT_COLUMN_WIDTH);
    }

    private void printItems(StringBuilder sb, ReceiptData data, String rowFormat) {
        if (data.hasPosItems()) {
            printPosItems(sb, data.getPosItems(), rowFormat);
        } else if (data.hasSaleItemModels()) {
            printSaleItemModels(sb, data.getSaleItemModels(), rowFormat);
        }
    }

    private void printPosItems(StringBuilder sb, List<PosItem> items, String rowFormat) {
        int index = 1;
        for (PosItem item : items) {
            appendPosItemRow(sb, item, index++, rowFormat);
        }
    }

    private void printSaleItemModels(StringBuilder sb, List<SaleItemModel> items, 
                                     String rowFormat) {
        int index = 1;
        for (SaleItemModel item : items) {
            appendSaleItemModelRow(sb, item, index++, rowFormat);
        }
    }

    private void appendPosItemRow(StringBuilder sb, PosItem item, int index, 
                                  String rowFormat) {
        sb.append(String.format("%2d. %s", index, item.getDescription())).append(NEW_LINE);

        double discount = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
        String discountDisplay = (discount > 0) ? formatCurrency(discount) : "-";

        String detailRow = String.format(rowFormat,
            "",
            formatCurrency(item.getSellingPrice()),
            String.valueOf(item.getQty()),
            discountDisplay,
            formatCurrency(item.getFinalAmount())
        );
        sb.append(detailRow).append(NEW_LINE);
    }

    private void appendSaleItemModelRow(StringBuilder sb, SaleItemModel item, 
                                        int index, String rowFormat) {
        sb.append(String.format("%2d. %s", index, item.getDescription())).append(NEW_LINE);

        double discount = getSafeDouble(item.getDiscountAmount());
        String discountDisplay = (discount > 0) ? formatCurrency(discount) : "-";

        String detailRow = String.format(rowFormat,
            "",
            formatCurrency(getSafeDouble(item.getSalePrice())),
            String.valueOf(item.getQuantity()),
            discountDisplay,
            formatCurrency(getSafeDouble(item.getSaleAmount()))
        );
        sb.append(detailRow).append(NEW_LINE);
    }

    // =========================================================================
    // Payment Helper Methods
    // =========================================================================
    
    private BigDecimal calculatePaymentTotal(SaleRequest saleRequest, 
                                             Function<PaymentRequest, BigDecimal> mapper) {
        return saleRequest.payments().stream()
            .map(mapper)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateGst(BigDecimal totalAmount) {
        return  totalAmount.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
    }

    private void appendPaymentDetail(StringBuilder sb, PaymentRequest payment,
                                     BigDecimal balance, String rowFormat) {
        String paymentType = payment.type().toUpperCase();
        double amount = payment.amount().doubleValue();
        double cashoutAmount = payment.cashoutAmount().doubleValue();

        switch (paymentType) {
            case PAYMENT_TYPE_CARD:
                sb.append(String.format(rowFormat, LABEL_EFT, formatCurrency(amount))).append(NEW_LINE);
                break;

            case PAYMENT_TYPE_CASH:
                sb.append(String.format(rowFormat, LABEL_CASH_PAID,
                    formatCurrency(payment.receivedAmount().doubleValue()))).append(NEW_LINE);
                if (balance.doubleValue() > 0) {
                    sb.append(String.format(rowFormat, LABEL_BALANCE,
                        formatCurrency(balance.doubleValue()))).append(NEW_LINE);
                }
                break;

            case PAYMENT_TYPE_CASHOUT:
                sb.append(String.format(rowFormat, LABEL_EFT, formatCurrency(amount))).append(NEW_LINE);
                if (cashoutAmount > 0) {
                    sb.append(String.format(rowFormat, LABEL_CASH_OUT,
                        formatCurrency(cashoutAmount))).append(NEW_LINE);
                }
                break;

            default:
                log.debug("알 수 없는 결제 유형: {}", paymentType);
                break;
        }
    }

    private String buildPaymentDetailsFromModel(SaleModel saleModel, String rowFormat) {
        StringBuilder sb = new StringBuilder();
        String paymentType = saleModel.getPaymentType();
        
        double cashAmount = getSafeDouble(saleModel.getCashAmount());
        double cashoutAmount = getSafeDouble(saleModel.getCashoutAmount());
        double creditAmount = getSafeDouble(saleModel.getCreditAmount());
        double receivedAmount = getSafeDouble(saleModel.getReceivedAmount());
        double changeAmount = getSafeDouble(saleModel.getChangeAmount());

        if (PAYMENT_TYPE_CASH.equalsIgnoreCase(paymentType)) {
            sb.append(String.format(rowFormat, LABEL_CASH_PAID, 
                formatCurrency(receivedAmount))).append(NEW_LINE);
            if (changeAmount > 0) {
                sb.append(String.format(rowFormat, LABEL_BALANCE, 
                    formatCurrency(changeAmount))).append(NEW_LINE);
            }
        }

        if (paymentType != null && paymentType.toUpperCase().contains(PAYMENT_TYPE_CASHOUT)) {
            sb.append(String.format(rowFormat, LABEL_EFT, 
                formatCurrency(creditAmount))).append(NEW_LINE);
            if (cashoutAmount > 0) {
                sb.append(String.format(rowFormat, LABEL_CASH_OUT, 
                    formatCurrency(cashoutAmount))).append(NEW_LINE);
            }
        }

        if (paymentType != null && paymentType.toUpperCase().contains(PAYMENT_TYPE_CARD)) {
            sb.append(String.format(rowFormat, LABEL_EFT, 
                formatCurrency(receivedAmount))).append(NEW_LINE);
            if (cashAmount > 0) {
                sb.append(String.format(rowFormat, LABEL_CASH_PAID, 
                    formatCurrency(cashAmount))).append(NEW_LINE);
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // Shop Information Helper Methods
    // =========================================================================
    
    private String extractShopName(Shop shop) {
        if (shop != null && shop.getCompany() != null) {
            return shop.getCompany().toUpperCase();
        }
        return DEFAULT_SHOP_NAME;
    }

    private String extractShopAddress(Shop shop) {
        if (shop != null && shop.getAddress() != null) {
            return shop.getAddress();
        }
        return DEFAULT_SHOP_ADDRESS;
    }

    private String extractPhoneNumber(Shop shop) {
        if (shop != null && shop.getPhone() != null) {
            return shop.getPhone();
        }
        return "";
    }

    private String extractBusinessNumber(Shop shop) {
        if (shop != null && shop.getBusinessNo() != null) {
            return shop.getBusinessNo();
        }
        return "";
    }

    private void appendShopDetails(StringBuilder sb, String address, 
                                   String phone, String businessNo) {
        if (!address.isEmpty()) {
            sb.append(address).append(NEW_LINE);
        }
        if (!phone.isEmpty()) {
            sb.append("Tel: ").append(phone).append(NEW_LINE);
        }
        if (!businessNo.isEmpty()) {
            sb.append("GST: ").append(businessNo).append(NEW_LINE);
        }
    }

    // =========================================================================
    // Notice Message Helper
    // =========================================================================
    
    private void appendInformMessage(StringBuilder sb, String inform, ReceiptStyle style) {
        String truncatedInform = truncateText(inform, style.getWidth() * 2);
        String wrappedInform = wrapTextByWidth(truncatedInform, style.getWidth());

        for (String line : wrappedInform.split(NEW_LINE)) {
            sb.append(style.center(line)).append(NEW_LINE);
        }
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    /**
     * 금액을 통화 형식으로 포맷팅
     * DecimalFormat 패턴에 "$"가 포함되어 있어 별도 연결 불필요
     * 
     * @param amount 포맷팅할 금액
     * @return 통화 형식의 문자열 (예: $1,234.56)
     */
    private String formatCurrency(double amount) {
        return CURRENCY_FORMATTER.format(amount);
    }

    /**
     * 금액을 통화 형식으로 포맷팅 (BigDecimal)
     * 
     * @param amount 포맷팅할 금액
     * @return 통화 형식의 문자열
     */
    private String formatCurrency(BigDecimal amount) {
        return amount != null ? CURRENCY_FORMATTER.format(amount) : ZERO_AMOUNT;
    }

    private BigDecimal getSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double getSafeDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String wrapTextByWidth(String text, int width) {
        if (text == null) {
            return "";
        }
        StringBuilder wrapped = new StringBuilder();
        for (int i = 0; i < text.length(); i += width) {
            int end = Math.min(i + width, text.length());
            wrapped.append(text, i, end).append(NEW_LINE);
        }
        return wrapped.toString().trim();
    }

    // =========================================================================
    // Inner Class: ReceiptData
    // =========================================================================
    
    @lombok.Builder
    @lombok.Getter
    private static class ReceiptData {
        private final String receiptNo;
        private final String date;
        private final double subtotal;
        private final double discountAmount;
        private final double finalAmount;
        
        private final SaleRequest saleRequest;
        private final List<PosItem> posItems;
        
        private final SaleModel saleModel;
        private final List<SaleItemModel> saleItemModels;

        public boolean isEmpty() {
            if (hasPosItems()) {
                return posItems == null || posItems.isEmpty();
            }
            if (hasSaleItemModels()) {
                return saleItemModels == null || saleItemModels.isEmpty();
            }
            return true;
        }

        public boolean hasSaleRequest() {
            return saleRequest != null;
        }

        public boolean hasPosItems() {
            return posItems != null && !posItems.isEmpty();
        }

        public boolean hasSaleModel() {
            return saleModel != null;
        }

        public boolean hasSaleItemModels() {
            return saleItemModels != null && !saleItemModels.isEmpty();
        }
    }
}