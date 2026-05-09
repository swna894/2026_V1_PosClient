package com.swna.javafx.pos.print;

import com.swna.javafx.admin.shop.Shop;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.dto.request.SaleRequest;
import com.swna.javafx.pos.service.PaymentResult;
import javafx.print.Printer;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ReceiptPrinter {
    
    // ESC/POS 명령어 상수화
    private static final byte[] CMD_CUT = { 0x1d, 0x56, 0x01 };
    private static final byte[] CMD_ALIGN_CENTER = { 0x1b, 0x61, 0x01 };
    private static final byte[] CMD_ALIGN_LEFT = { 0x1b, 0x61, 0x00 };
    private static final byte[] FONT_58MM = { 0x1b, 0x21, 0x06 };
    private static final byte[] FONT_80MM = { 0x1b, 0x21, 0x03 };
    private static final byte[] LF = { 0x0a }; // Line Feed (한 줄 바꿈)

    private final PrinterService printerService;
    private final ReceiptFormatter formatter;
    private String printerName;

    public ReceiptPrinter(PrinterService printerService, ReceiptFormatter formatter) {
        this.printerService = printerService;
        this.formatter = formatter;
    }

    public void printInvoice(SaleRequest saleRequest, PaymentResult result, List<PosItem> posItems, Shop shop, ReceiptStyle style, String inform) {
        initializePrinterName();

        // 1. 텍스트 콘텐츠 생성
        String content = formatter.buildContent(saleRequest, result, posItems, shop, style, inform);
        
        // 2. 폰트 설정 및 본문 출력
        byte[] fontSetting = (style == ReceiptStyle.SIZE_80MM) ? FONT_80MM : FONT_58MM;
        printerService.printBytes(printerName, fontSetting);
        printerService.printBytes(printerName, content.getBytes(StandardCharsets.UTF_8));
        
        // 본문 종료 후 여백 확보
        printerService.printBytes(printerName, LF);

        // 3. 바코드 출력 (영수증 번호 기준)
        String receiptNo = result.getSaleResponse().receiptNo();
        if (receiptNo != null && !receiptNo.isBlank()) {
            printerService.printBytes(printerName, CMD_ALIGN_CENTER);
            printerService.printBytes(printerName, LF); // 바코드 위쪽 여백
            
            printerService.printBytes(printerName, getBarcodeBytes(receiptNo));
            
            // 바코드 아래쪽 여백 및 정렬 원복
            printerService.printBytes(printerName, LF);
            printerService.printBytes(printerName, LF);
            printerService.printBytes(printerName, CMD_ALIGN_LEFT);
        }

        // 4. 용지 피딩 및 컷팅
        // 헤드와 커터 사이의 거리를 고려하여 용지를 밀어 올려줍니다 (Feed before cut)
        printerService.printBytes(printerName, LF);
        printerService.printBytes(printerName, LF);
        printerService.printBytes(printerName, LF);
        printerService.printBytes(printerName, CMD_CUT);
    }

    private void initializePrinterName() {
        if (this.printerName == null) {
            Printer defaultPrinter = Printer.getDefaultPrinter();
            this.printerName = (defaultPrinter != null) ? defaultPrinter.getName() : "";
        }
    }

    public byte[] getBarcodeBytes(String data) {
        if (data == null) return new byte[0];
        byte[] bData = data.getBytes(StandardCharsets.US_ASCII);

        return combine(
            new byte[]{ 0x1D, 0x48, 0x02 }, // HRI 위치 아래
            new byte[]{ 0x1D, 0x77, 0x02 }, // 폭
            new byte[]{ 0x1D, 0x68, 0x50 }, // 높이
            new byte[]{ 0x1D, 0x6B, 0x49, (byte) bData.length }, // CODE128 헤더
            bData
        );
    }

    private byte[] combine(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) length += array.length;
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }
}