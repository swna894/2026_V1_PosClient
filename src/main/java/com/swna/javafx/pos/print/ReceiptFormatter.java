package com.swna.javafx.pos.print;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.PaymentRequest;
import com.swna.javafx.pos.service.PaymentResult;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReceiptFormatter {
    private static final String NL = "\n";
    private static final DecimalFormat CURRENCY_DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter SRC_DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DST_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildContent(PaymentResult result, List<PosItem> posItems, Shop shop, ReceiptStyle style, String inform) {
        StringBuilder sb = new StringBuilder();

        String receiptNo = result.getSaleResponse().receiptNo();
        String date = formatReceiptDate(receiptNo);

        // [Header]
        sb.append(style.center(shop.getName())).append(NL);
        sb.append(style.center(shop.getAddress())).append(NL);
        sb.append(style.getLine(false)).append(NL);
        sb.append(style.justify("Date:", date)).append(NL);
        sb.append(style.justify("Receipt No:", receiptNo)).append(NL);
        sb.append(style.getLine(false)).append(NL);

        // [Body]
        for (PosItem item : posItems) {
            sb.append(item.getDescription()).append(NL);
            String qtyPrice = String.format("  %d x %s", item.getQty(), CURRENCY_DF.format(item.getSellingPrice()));
            sb.append(style.justify(qtyPrice, CURRENCY_DF.format(item.getFinalAmount()))).append(NL);
        }
        sb.append(style.getLine(false)).append(NL);

        // [Footer] - 합계 및 결제 정보
        sb.append(style.justify("TOTAL AMOUNT", CURRENCY_DF.format(result.getSaleResponse().totalAmount()))).append(NL);
        
        //System.out.println("result.getSaleRequest() =" + result,getSaleRequest());
        
        for (PaymentRequest p : result.getSaleRequest().payments()) {
            String label = p.type().equals("CASH") ? "CASH PAID" : "CARD PAID";
            sb.append(style.justify(label, CURRENCY_DF.format(p.receivedAmount()))).append(NL);
        }

        // [Notice]
        if (inform != null && !inform.isBlank()) {
            sb.append(style.getLine(true)).append(NL);
            sb.append(style.getNoticeLine("Notice")).append(NL);
            sb.append(wrapText(inform, style.getWidth())).append(NL);
        }

        return sb.toString();
    }

    private String formatReceiptDate(String receiptNo) {
        try {
            if (receiptNo == null || !receiptNo.contains("_")) return "N/A";
            return LocalDateTime.parse(receiptNo.split("_")[0], SRC_DTF).format(DST_DTF);
        } catch (Exception e) {
            return "Invalid Date";
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