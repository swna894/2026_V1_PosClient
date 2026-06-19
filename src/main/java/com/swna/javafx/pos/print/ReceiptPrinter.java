package com.swna.javafx.pos.print;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.dto.response.PaymentResult;
import com.swna.javafx.pos.model.PosItem;
import com.swna.javafx.pos.service.config.PrintToggleService;

import javafx.print.Printer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReceiptPrinter {
    
    private static final byte[] CMD_INIT = { 0x1B, 0x40 };           
    private static final byte[] CMD_CUT = { 0x1D, 0x56, 0x01 };      
    private static final byte[] CMD_ALIGN_CENTER = { 0x1B, 0x61, 0x01 }; 
    private static final byte[] CMD_ALIGN_LEFT = { 0x1B, 0x61, 0x00 };    
    private static final byte[] FONT_58MM = { 0x1B, 0x21, 0x06 };     
    private static final byte[] FONT_80MM = { 0x1B, 0x21, 0x06 };     
    private static final byte[] CR_LF = { 0x0D, 0x0A };               

    private final PrintToggleService printToggleService;
    private final PrinterService printerService;
    private final ReceiptFormatter formatter;
    private String printerName;

    public ReceiptPrinter(PrinterService printerService, ReceiptFormatter formatter, PrintToggleService printToggleService) {
        this.printToggleService = printToggleService;
        this.printerService = printerService;
        this.formatter = formatter;
    }


    // Pos 에서 프린트 요청
    public void printInvoice(SaleRequest saleRequest, PaymentResult result, 
                            List<PosItem> posItems, Shop shop, 
                            ReceiptStyle style, String inform) {
        
      String receiptNo = (result != null && result.getSaleResponse() != null) 
            ? result.getSaleResponse().receiptNo() : null;
            
        log.info("=== ReceiptPrinter.printInvoice() START ===");
        
        initializePrinterName();

        String content = formatter.buildReceiptContent(saleRequest, result, posItems, shop, style, inform);
        byte[] allPrintData = buildFullPrintData(content, style, receiptNo);
        printerService.printBytes(printerName, allPrintData);
        
        log.info("=== ReceiptPrinter.printInvoice() END ===");
    }
    

    //  Receipt 확인에서 프리트 요청
    public void printInvoice(SaleModel saleModel, List<SaleItemModel> itemModels, Shop shop, ReceiptStyle style, String inform) {
        String receiptNo = saleModel.getReceiptNo();

        initializePrinterName();

        String content = formatter.buildReceiptContent(saleModel, itemModels, shop, style, inform);
        byte[] allPrintData = buildFullPrintData(content, style, receiptNo);
        printerService.printBytes(printerName, allPrintData);

        log.info("=== Dialog requset : ReceiptPrinter.printInvoice() START ===");
    }





    /**
     * 전체 인쇄 데이터를 하나의 바이트 배열로 구성 (★수정됨: 하드웨어 바코드 환경설정 집합 추가)
     */
    private byte[] buildFullPrintData(String content, ReceiptStyle style, String receiptNo) {
        List<byte[]> parts = new ArrayList<>();
        
        parts.add(CMD_INIT);
        parts.add((style == ReceiptStyle.SIZE_80MM) ? FONT_80MM : FONT_58MM);
        parts.add(content.getBytes(StandardCharsets.UTF_8));
            
        if (receiptNo != null && !receiptNo.isBlank() && printToggleService.isBarcodeEnabled()) {
            parts.add(CMD_ALIGN_CENTER);
            
            // ✨ [안전장치 하드웨어 명령어 추가] 바코드의 자체 폰트 겹침을 방지하고 모양을 견고하게 정렬합니다.
            parts.add(new byte[] { 0x1D, 0x48, 0x00 }); // GS H 0 : 바코드 기본 내장 HRI 글자 숨김 (포맷터 글자와 이중 겹침 방지)
            parts.add(new byte[] { 0x1D, 0x77, 0x02 }); // GS w 2 : 바코드 검은 선 가로 폭 굵기 설정
            parts.add(new byte[] { 0x1D, 0x68, 0x50 }); // GS h 80: 바코드 그래픽 바 세로 높이 지정 (80 픽셀)
            
            parts.add(getBarcodeBytes(receiptNo));
            parts.add(CR_LF);
            parts.add(CR_LF);
        }
        
        parts.add(CMD_ALIGN_LEFT);
        parts.add(CR_LF);
        parts.add(CR_LF);
        parts.add(CR_LF);
         if (!printToggleService.isBarcodeEnabled()) {
            parts.add(CR_LF);
            parts.add(CR_LF);
        }
        parts.add(CMD_CUT);
        
        byte[] resultData = combine(parts);
        log.info("Total print data size: {} bytes ({} parts)", resultData.length, parts.size());
        return resultData;
    }
    
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

    /**
     * CODE128 규격 바이트 반환 (★수정됨: Code Set B 조합 규격 전면 수정)
     */
    public byte[] getBarcodeBytes(String data) {
        if (data == null || data.isEmpty()) return new byte[0];
        
        // 원본 텍스트를 순수 ASCII 표준 바이트배열로 변경
        byte[] bData = data.getBytes(StandardCharsets.US_ASCII);
        
        // 영수증 문자열 내부의 문자 및 기호(_) 호환 출력을 위해 
        // CODE128의 'Code Set B' 진입 제어 식별 문자 헤더인 {B (0x7B, 0x42) 를 선언합니다.
        byte[] codePrefix = new byte[] { 0x7B, 0x42 };
        
        // 프리픽스와 원본 데이터를 한 군데로 병합
        byte[] payload = new byte[codePrefix.length + bData.length];
        System.arraycopy(codePrefix, 0, payload, 0, codePrefix.length);
        System.arraycopy(bData, 0, payload, codePrefix.length, bData.length);
        
        // ESC/POS 표준 무손식 바코드 선언 규격인 GS k 73 양식 조립
        byte[] barcode = new byte[4 + payload.length];
        barcode[0] = 0x1D;  // GS
        barcode[1] = 0x6B;  // k
        barcode[2] = 0x49;  // CODE128 대분류 타입 번호 (73)
        barcode[3] = (byte) payload.length; // 전송할 데이터 전체 바이트 수 지정
        System.arraycopy(payload, 0, barcode, 4, payload.length);
        
        return barcode;
    }

}