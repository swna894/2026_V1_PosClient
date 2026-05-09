package com.swna.javafx.pos.print;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class PrinterService {

    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);
    
    private static final DocFlavor FLAVOR = DocFlavor.BYTE_ARRAY.AUTOSENSE;
    
    // Console 디버그 모드 (개발 환경에서는 true로 설정)
    @Value("${print.debug.console:true}")
    private boolean debugConsole;
    
    @Value("${print.debug.real:false}")
    private boolean realPrint;

    @Async("printExecutor")
    public void printBytes(String printerName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            logger.warn("인쇄 데이터가 없습니다.");
            return;
        }

        // Console 디버그 출력
        if (debugConsole) {
            printToConsole(printerName, bytes);
        }

        // 실제 프린터 출력
        if (realPrint) {
            printToRealPrinter(printerName, bytes);
        } else {
            logger.info("[DEV MODE] 실제 프린터로 출력하지 않음 (realPrint=false)");
        }
    }

    /**
     * Console에 프린트 내용 출력 (디버깅용)
     */
    private void printToConsole(String printerName, byte[] bytes) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🖨️ [PRINT DEBUG] - Printer: " + printerName);
        System.out.println("📊 Data size: " + bytes.length + " bytes");
        System.out.println("=".repeat(70));
        
        try {
            String content = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(content);
        } catch (Exception e) {
            System.out.println("⚠️ 디코딩 실패: " + e.getMessage());
        }
        
        System.out.println("=".repeat(70));
        System.out.println("✅ Console output only\n");
    }

    /**
     * 실제 프린터로 출력
     */
    private void printToRealPrinter(String printerName, byte[] bytes) {
        PrintService service = findPrintService(printerName);
        if (service == null) {
            logger.error("인쇄 실패: 프린터 '{}'를 찾을 수 없습니다.", printerName);
            return;
        }

        try {
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            pras.add(new JobName("POS-Receipt-" + System.currentTimeMillis(), Locale.getDefault()));

            DocPrintJob job = service.createPrintJob();
            Doc doc = new SimpleDoc(bytes, FLAVOR, null);

            logger.info("프린터 '{}'로 데이터 전송 시작 ({} bytes)", service.getName(), bytes.length);
            job.print(doc, pras);
            
            Thread.sleep(100); 

        } catch (PrintException e) {
            logger.error("인쇄 중 하드웨어 에러 발생: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void listAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(FLAVOR, null);
        for (PrintService s : services) {
            logger.info("사용 가능한 프린터: {}", s.getName());
        }
    }

    private PrintService findPrintService(String printerName) {
        if (printerName == null || printerName.isEmpty()) {
            return PrintServiceLookup.lookupDefaultPrintService();
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(FLAVOR, null);
        for (PrintService service : services) {
            if (service.getName().contains(printerName)) {
                return service;
            }
        }
        
        logger.warn("'{}'를 찾지 못해 기본 프린터를 사용합니다.", printerName);
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}