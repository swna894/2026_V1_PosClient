package com.swna.javafx.pos.print;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.model.PosItem;

import javafx.print.Printer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReceiptPrinter {
    
    // ESC/POS 명령어 상수화
    private static final byte[] CMD_INIT = { 0x1B, 0x40 };           // 초기화
    private static final byte[] CMD_CUT = { 0x1D, 0x56, 0x01 };      // 용지 자르기
    private static final byte[] CMD_ALIGN_CENTER = { 0x1B, 0x61, 0x01 }; // 중앙 정렬
    private static final byte[] CMD_ALIGN_LEFT = { 0x1B, 0x61, 0x00 };    // 왼쪽 정렬
    private static final byte[] FONT_58MM = { 0x1B, 0x21, 0x06 };     // 58mm 폰트
    private static final byte[] FONT_80MM = { 0x1B, 0x21, 0x03 };     // 80mm 폰트
    private static final byte[] CR_LF = { 0x0D, 0x0A };               // Carriage Return + Line Feed

    private final PrinterService printerService;
    private final ReceiptFormatter formatter;
    private String printerName;

    public ReceiptPrinter(PrinterService printerService, ReceiptFormatter formatter) {
        this.printerService = printerService;
        this.formatter = formatter;
    }

    public void printInvoice(SaleRequest saleRequest, PaymentResult result, 
                            List<PosItem> posItems, Shop shop, 
                            ReceiptStyle style, String inform) {
        
        log.info("=== ReceiptPrinter.printInvoice() START ===");
        
        initializePrinterName();

        // 1. 텍스트 콘텐츠 생성
        String content = formatter.buildContent(saleRequest, result, posItems, shop, style, inform);
        
        // 2. 모든 인쇄 데이터를 하나의 바이트 배열로 구성
        byte[] allPrintData = buildFullPrintData(content, style, result);
        
        // 3. 한 번에 전송 (여러 번 호출하지 않음)
        printerService.printBytes(printerName, allPrintData);
        
        log.info("=== ReceiptPrinter.printInvoice() END ===");
    }
    
    /**
     * 전체 인쇄 데이터를 하나의 바이트 배열로 구성
     */
    private byte[] buildFullPrintData(String content, ReceiptStyle style, PaymentResult result) {
        List<byte[]> parts = new ArrayList<>();
        
        // 1. 프린터 초기화
        parts.add(CMD_INIT);
        
        // 2. 폰트 설정
        parts.add((style == ReceiptStyle.SIZE_80MM) ? FONT_80MM : FONT_58MM);
        
        // 3. 중앙 정렬로 본문 시작
        parts.add(CMD_ALIGN_CENTER);
        
        // 4. 본문 내용 (UTF-8)
        parts.add(content.getBytes(StandardCharsets.UTF_8));
        
        // 5. 여백
        parts.add(CR_LF);
        parts.add(CR_LF);
        
        // 6. 바코드 출력
        String receiptNo = (result != null && result.getSaleResponse() != null) 
            ? result.getSaleResponse().receiptNo() : null;
            
        if (receiptNo != null && !receiptNo.isBlank()) {
            parts.add(CMD_ALIGN_CENTER);
            parts.add(CR_LF);
            parts.add(getBarcodeBytes(receiptNo));
            parts.add(CR_LF);
            parts.add(CR_LF);
        }
        
        // 7. 왼쪽 정렬로 복원
        parts.add(CMD_ALIGN_LEFT);
        
        // 8. 용지 자르기 전 여백
        parts.add(CR_LF);
        parts.add(CR_LF);
        parts.add(CR_LF);
        
        // 9. 용지 자르기
        parts.add(CMD_CUT);
        
        // 전체 합치기
        byte[] resultData = combine(parts);
        
        log.info("Total print data size: {} bytes ({} parts)", resultData.length, parts.size());
        
        return resultData;
    }
    
    /**
     * 여러 바이트 배열을 하나로 합치기 (List 버전)
     */
    private byte[] combine(List<byte[]> parts) {
        int totalLength = 0;
        for (byte[] part : parts) {
            if (part != null) {
                totalLength += part.length;
            }
        }
        
        byte[] result = new byte[totalLength];
        int position = 0;
        for (byte[] part : parts) {
            if (part != null) {
                System.arraycopy(part, 0, result, position, part.length);
                position += part.length;
            }
        }
        return result;
    }
    

    private void initializePrinterName() {
        if (this.printerName == null) {
            Printer defaultPrinter = Printer.getDefaultPrinter();
            this.printerName = (defaultPrinter != null) ? defaultPrinter.getName() : "POS-80";
            log.info("Printer initialized: {}", this.printerName);
        }
    }

    public byte[] getBarcodeBytes(String data) {
        if (data == null || data.isEmpty()) return new byte[0];
        byte[] bData = data.getBytes(StandardCharsets.US_ASCII);
        
        // GS k 73 (CODE128) + data
        byte[] barcode = new byte[4 + bData.length];
        barcode[0] = 0x1D;  // GS
        barcode[1] = 0x6B;  // k
        barcode[2] = 0x49;  // CODE128
        barcode[3] = (byte) bData.length;
        System.arraycopy(bData, 0, barcode, 4, bData.length);
        
        return barcode;
    }
}