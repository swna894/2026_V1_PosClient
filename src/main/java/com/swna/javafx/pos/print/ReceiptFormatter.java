package com.swna.javafx.pos.print;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.dto.response.SaleResponse;
import com.swna.javafx.pos.model.PosItem;

import lombok.extern.slf4j.Slf4j;

/**
 * 영수증 포맷팅을 담당하는 클래스
 * ESC/POS 프린터 제어 코드를 포함한 영수증 문자열을 생성합니다.
 */
@Slf4j
@Component
public class ReceiptFormatter {
    
    // ==================== ESC/POS 제어 코드 상수 ====================
    private static final String ESC = "\u001B";
    private static final String BOLD_ON = ESC + "E" + (char) 1;   // 굵게 시작
    private static final String BOLD_OFF = ESC + "E" + (char) 0;  // 굵게 종료
    private static final String NEW_LINE = "\n";

    // ==================== 포맷터 상수 ====================
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SOURCE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ==================== 컬럼 너비 상수 ====================
    private static final int QTY_COLUMN_WIDTH = 9;
    private static final int PRICE_COLUMN_WIDTH = 6;
    private static final int DISCOUNT_COLUMN_WIDTH = 9;
    private static final int AMOUNT_COLUMN_WIDTH = 9;
    private static final int ITEM_COLUMN_OFFSET = 5;

    // ==================== Utility Methods ====================
    
    /**
     * 금액을 통화 형식(예: $1,234.56)으로 포맷팅합니다.
     * 
     * @param amount 포맷팅할 금액
     * @return 통화 형식의 문자열
     */
    private String formatCurrency(double amount) {
        return "$" + CURRENCY_FORMATTER.format(amount);
    }
    
    /**
     * 지정된 길이로 텍스트를 자르고 말줄임표(...)를 추가합니다.
     * 
     * @param text      원본 텍스트
     * @param maxLength 최대 허용 길이
     * @return 잘린 텍스트 또는 원본 텍스트
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 긴 텍스트를 지정된 너비로 줄바꿈 처리합니다.
     * 
     * @param text  줄바꿈할 텍스트
     * @param width 줄 너비
     * @return 줄바꿈된 텍스트
     */
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

    // ==================== Data Extraction Methods ====================
    
    /**
     * PaymentResult에서 영수증 번호를 추출합니다.
     * 
     * @param paymentResult 결제 결과
     * @return 영수증 번호 (없으면 "N/A")
     */
    private String extractReceiptNumber(PaymentResult paymentResult) {
        if (paymentResult == null || paymentResult.getSaleResponse() == null) {
            return "N/A";
        }
        return paymentResult.getSaleResponse().receiptNo();
    }
    
    /**
     * PaymentResult 또는 PosItem 목록에서 소계(subtotal)를 추출합니다.
     * 
     * @param paymentResult 결제 결과
     * @param posItems      판매 품목 목록
     * @return 소계 금액
     */
    private double extractSubtotal(PaymentResult paymentResult, List<PosItem> posItems) {
        // 1. PaymentResult에서 추출 시도
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.totalAmount() != null) {
                return response.totalAmount().doubleValue();
            }
        }
        
        // 2. PosItem 목록에서 계산
        if (posItems != null) {
            return posItems.stream()
                    .mapToDouble(item -> item.getOriginalPrice() * item.getQty())
                    .sum();
        }
        
        return 0.0;
    }
    
    /**
     * PaymentResult 또는 SaleRequest에서 할인 금액을 추출합니다.
     * 
     * @param paymentResult 결제 결과
     * @param saleRequest   판매 요청
     * @return 할인 금액
     */
    private double extractDiscountAmount(PaymentResult paymentResult, SaleRequest saleRequest) {
        // 1. PaymentResult에서 추출 시도
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.discountAmount() != null) {
                return response.discountAmount().doubleValue();
            }
        }
        
        // 2. SaleRequest에서 추출
        if (saleRequest != null && saleRequest.discounts() != null) {
            return saleRequest.discounts().stream()
                    .mapToDouble(d -> d.value() != null ? d.value().doubleValue() : 0.0)
                    .sum();
        }
        
        return 0.0;
    }
    
    /**
     * PaymentResult 또는 계산된 값에서 최종 금액을 추출합니다.
     * 
     * @param paymentResult  결제 결과
     * @param subtotal       소계
     * @param discountAmount 할인 금액
     * @return 최종 금액
     */
    private double extractFinalAmount(PaymentResult paymentResult, double subtotal, double discountAmount) {
        if (paymentResult != null && paymentResult.getSaleResponse() != null) {
            SaleResponse response = paymentResult.getSaleResponse();
            if (response.finalAmount() != null) {
                return response.finalAmount().doubleValue();
            }
        }
        return subtotal - discountAmount;
    }

    /**
     * 영수증 번호에서 날짜를 추출하여 표시 형식으로 변환합니다.
     * 영수증 번호 형식: "yyyyMMddHHmm_일련번호"
     * 
     * @param receiptNo 영수증 번호
     * @return 표시용 날짜/시간 문자열
     */
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

    // ==================== Payment Calculation Methods ====================
    
    /**
     * SaleRequest에서 결제 금액의 합계를 계산합니다.
     * 
     * @param saleRequest 판매 요청
     * @param mapper      금액 추출 매퍼
     * @return 합계 금액 (BigDecimal)
     */
    private BigDecimal calculatePaymentTotal(SaleRequest saleRequest, Function<PaymentRequest, BigDecimal> mapper) {
        return saleRequest.payments().stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 총 금액에서 GST(세금)를 계산합니다.
     * GST는 총금액의 15%입니다.
     * 
     * @param totalAmount 총 금액
     * @return GST 금액
     */
    private BigDecimal calculateGst(BigDecimal totalAmount) {
        // GST = 총금액 - (총금액 / 1.15)
        return totalAmount.subtract(totalAmount.divide(BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP));
    }

    // ==================== Section Builders ====================
    
    /**
     * 영수증 헤더를 빌드합니다.
     * - 매장명 (굵게 표시)
     * - 매장 주소, 전화번호, 사업자번호
     * - 날짜, 영수증 번호
     * 
     * @param sb         문자열 빌더
     * @param style      영수증 스타일
     * @param receiptNo  영수증 번호
     * @param date       날짜/시간
     * @param shop       매장 정보
     */
    private void buildHeaderSection(StringBuilder sb, ReceiptStyle style, 
                                    String receiptNo, String date, Shop shop) {
        // 매장 정보 추출 (기본값 제공)
        String shopName = extractShopName(shop);
        String shopAddress = extractShopAddress(shop);
        String phoneNumber = extractPhoneNumber(shop);
        String businessNumber = extractBusinessNumber(shop);
        
        // 매장명 (굵게)
        sb.append(BOLD_ON)
          .append(shopName).append(NEW_LINE)
          .append(BOLD_OFF);
        
        // 매장 상세 정보
        appendShopDetails(sb, shopAddress, phoneNumber, businessNumber);
        
        // 구분선 및 헤더 정보
        sb.append(style.getLine(true)).append(NEW_LINE);
        sb.append(style.justify("Date:", date)).append(NEW_LINE);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NEW_LINE);
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    /**
     * 매장명을 추출합니다.
     */
    private String extractShopName(Shop shop) {
        if (shop != null && shop.getCompany() != null) {
            return shop.getCompany().toUpperCase();
        }
        return "MY STORE";
    }

    /**
     * 매장 주소를 추출합니다.
     */
    private String extractShopAddress(Shop shop) {
        if (shop != null && shop.getAddress() != null) {
            return shop.getAddress();
        }
        return "123 Main Street, Suite 100";
    }

    /**
     * 매장 전화번호를 추출합니다.
     */
    private String extractPhoneNumber(Shop shop) {
        if (shop != null && shop.getPhone() != null) {
            return shop.getPhone();
        }
        return "";
    }

    /**
     * 매장 사업자번호를 추출합니다.
     */
    private String extractBusinessNumber(Shop shop) {
        if (shop != null && shop.getBusinessNo() != null) {
            return shop.getBusinessNo();
        }
        return "";
    }

    /**
     * 매장 상세 정보를 추가합니다.
     */
    private void appendShopDetails(StringBuilder sb, String address, String phone, String businessNo) {
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

    /**
     * 영수증 본문(품목 목록)을 빌드합니다.
     * 
     * @param sb        문자열 빌더
     * @param posItems  판매 품목 목록
     * @param style     영수증 스타일
     */
    private void buildBodySection(StringBuilder sb, List<PosItem> posItems, ReceiptStyle style) {
        int totalWidth = style.getWidth();
        
        // 각 컬럼 너비 계산
        int itemWidth = totalWidth - (QTY_COLUMN_WIDTH + PRICE_COLUMN_WIDTH + DISCOUNT_COLUMN_WIDTH + AMOUNT_COLUMN_WIDTH + ITEM_COLUMN_OFFSET);
        String rowFormat = String.format("%%-%ds %%%ds %%%ds %%%ds  %%%ds", 
                itemWidth, PRICE_COLUMN_WIDTH, QTY_COLUMN_WIDTH, DISCOUNT_COLUMN_WIDTH, AMOUNT_COLUMN_WIDTH);
        
        // 컬럼 헤더 출력
        sb.append(String.format(rowFormat, "Item", "Price", "Qty", "D/C", "Amount")).append(NEW_LINE);
        sb.append(style.getLine(false)).append(NEW_LINE);
        
        // 품목이 없는 경우
        if (posItems == null || posItems.isEmpty()) {
            sb.append(style.center("No items found for this receipt")).append(NEW_LINE);
            return;
        }
        
        // 각 품목 출력
        int itemIndex = 1;
        for (PosItem item : posItems) {
            appendItemRow(sb, item, itemIndex++, rowFormat, style);
        }
        
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    /**
     * 단일 품목 행을 추가합니다.
     */
    private void appendItemRow(StringBuilder sb, PosItem item, int index, String rowFormat, ReceiptStyle style) {
        // 품목명 (인덱스 포함)
        sb.append(String.format("%2d. %s", index, item.getDescription())).append(NEW_LINE);
        
        // 할인 금액 계산
        double discount = (item.getOriginalPrice() - item.getSellingPrice()) * item.getQty();
        String discountDisplay = (discount > 0) ? formatCurrency(discount) : "-";
        
        // 품목 상세 정보 (가격, 수량, 할인, 금액)
        String detailRow = String.format(rowFormat,
                "",                                     // Item 컬럼은 비움 (이미 위에 표시됨)
                formatCurrency(item.getSellingPrice()), // Price
                String.valueOf(item.getQty()),          // Qty
                discountDisplay,                        // Discount
                formatCurrency(item.getFinalAmount())   // Amount
        );
        sb.append(detailRow).append(NEW_LINE);
    }

    /**
     * 영수증 푸터(합계 정보)를 빌드합니다.
     * 
     * @param sb              문자열 빌더
     * @param style           영수증 스타일
     * @param subtotal        소계
     * @param discountAmount  할인 금액
     * @param finalAmount     최종 금액
     */
    private void buildFooterSection(StringBuilder sb, ReceiptStyle style, 
                                    double subtotal, double discountAmount, double finalAmount) {
        if (discountAmount > 0) {
            sb.append(style.justify("ORIGINAL AMOUNT", formatCurrency(subtotal))).append(NEW_LINE);
            sb.append(style.justify("DISCOUNT", "-" + formatCurrency(discountAmount))).append(NEW_LINE);
        } else {
            sb.append(style.justify("TOTAL AMOUNT", formatCurrency(finalAmount))).append(NEW_LINE);
        }
        sb.append(style.getLine(false)).append(NEW_LINE);
    }

    /**
     * 영수증 결제 정보를 빌드합니다.
     * 
     * @param sb           문자열 빌더
     * @param saleRequest  판매 요청
     * @param style        영수증 스타일
     */
    private void buildPaymentSection(StringBuilder sb, SaleRequest saleRequest, ReceiptStyle style) {
        if (saleRequest == null || saleRequest.payments() == null || saleRequest.payments().isEmpty()) {
            return;
        }

        // 결제 총액 계산
        BigDecimal totalAmount = calculatePaymentTotal(saleRequest, PaymentRequest::amount);
        BigDecimal totalReceived = calculatePaymentTotal(saleRequest, p -> 
                p.receivedAmount() != null ? p.receivedAmount() : BigDecimal.ZERO);
        BigDecimal balance = totalReceived.subtract(totalAmount);
        BigDecimal gst = calculateGst(totalAmount);

        // 최종 금액 표시
        int width = style.getWidth();
        String rowFormat = "%20s %-" + (width - 21) + "s";
        
        String finalAmountDisplay = String.format("%s (GST: %s)", 
                formatCurrency(totalAmount.doubleValue()), 
                formatCurrency(gst.doubleValue()));
        sb.append(String.format(rowFormat, "Final Amount : ", finalAmountDisplay)).append(NEW_LINE);

        // 각 결제 수단별 상세 정보
        for (PaymentRequest payment : saleRequest.payments()) {
            appendPaymentDetail(sb, payment, balance, rowFormat);
        }
        
        sb.append(style.getLine(true)).append(NEW_LINE);
    }

    /**
     * 단일 결제 상세 정보를 추가합니다.
     */
    private void appendPaymentDetail(StringBuilder sb, PaymentRequest payment, 
                                     BigDecimal balance, String rowFormat) {
        String paymentType = payment.type().toUpperCase();
        double amount = payment.amount().doubleValue();

        switch (paymentType) {
            case "CARD":
                sb.append(String.format(rowFormat, "EFT : ", formatCurrency(amount))).append(NEW_LINE);
                break;
                
            case "CASH":
                sb.append(String.format(rowFormat, "Cash Paid : ", 
                        formatCurrency(payment.receivedAmount().doubleValue()))).append(NEW_LINE);
                if (balance.doubleValue() > 0) {
                    sb.append(String.format(rowFormat, "Balance : ", 
                            formatCurrency(balance.doubleValue()))).append(NEW_LINE);
                }
                break;
                
            case "CASHOUT":
                sb.append(String.format(rowFormat, "EFT : ", formatCurrency(amount))).append(NEW_LINE);
                if (payment.cashoutAmount().doubleValue() > 0) {
                    sb.append(String.format(rowFormat, "Cash Out : ", 
                            formatCurrency(payment.cashoutAmount().doubleValue()))).append(NEW_LINE);
                }
                break;
                
            default:
                log.debug("알 수 없는 결제 유형: {}", paymentType);
                break;
        }
    }

    /**
     * 영수증 하단 공지사항을 빌드합니다.
     * 
     * @param sb      문자열 빌더
     * @param style   영수증 스타일
     * @param inform  추가 공지사항 (선택적)
     */
    private void buildNoticeSection(StringBuilder sb, ReceiptStyle style, String inform) {
        // 기본 공지사항
        sb.append(style.center("Goods sold are not refundable")).append(NEW_LINE);
        sb.append(style.center("For exchange, please bring receipt")).append(NEW_LINE);
        
        // 추가 공지사항 (있는 경우)
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(false)).append(NEW_LINE);
            
            String truncatedInform = truncateText(inform, style.getWidth() * 2);
            String wrappedInform = wrapTextByWidth(truncatedInform, style.getWidth());
            
            for (String line : wrappedInform.split(NEW_LINE)) {
                sb.append(style.center(line)).append(NEW_LINE);
            }
        }
    }

    // ==================== Public Main Method ====================
    
    /**
     * 전체 영수증 내용을 생성합니다.
     * 
     * @param saleRequest    판매 요청 정보
     * @param paymentResult  결제 결과
     * @param posItems       판매 품목 목록
     * @param shop           매장 정보
     * @param style          영수증 스타일
     * @param inform         추가 공지사항 (선택적)
     * @return 완성된 영수증 문자열
     */
    public String buildReceiptContent(SaleRequest saleRequest, PaymentResult paymentResult, 
                                      List<PosItem> posItems, Shop shop, 
                                      ReceiptStyle style, String inform) {
        
        log.info("영수증 생성 시작 - ReceiptFormatter.buildReceiptContent()");
        
        // 1. 필요한 데이터 추출
        String receiptNo = extractReceiptNumber(paymentResult);
        String date = formatReceiptDate(receiptNo);
        double subtotal = extractSubtotal(paymentResult, posItems);
        double discountAmount = extractDiscountAmount(paymentResult, saleRequest);
        double finalAmount = extractFinalAmount(paymentResult, subtotal, discountAmount);
        
        // 2. 영수증 빌드
        StringBuilder receipt = new StringBuilder();
        
        buildHeaderSection(receipt, style, receiptNo, date, shop);
        buildBodySection(receipt, posItems, style);
        buildFooterSection(receipt, style, subtotal, discountAmount, finalAmount);
        buildPaymentSection(receipt, saleRequest, style);
        buildNoticeSection(receipt, style, inform);
        
        log.info("영수증 생성 완료 - 영수증 번호: {}", receiptNo);
        return receipt.toString();
    }
}