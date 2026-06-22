package com.swna.javafx.pos.print;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PrinterService {

    
    private static final DocFlavor FLAVOR = DocFlavor.BYTE_ARRAY.AUTOSENSE;
    
    // ESC/POS 표준 돈통 오픈 명령어 고정값 (2번 핀 Pulse 신호)
    private static final byte[] ESC_POS_OPEN_DRAWER = new byte[]{ 27, 112, 48, 55, 121 };

    // 비동기(@Async) 프록시 내부 호출 문제를 안전하게 처리하기 위한 자기 자신(Self) 주입
    private final ObjectProvider<PrinterService> selfProvider;
    
    // Console 디버그 모드 (개발 환경에서는 true로 설정)
    @Value("${print.debug.console:true}")
    private boolean debugConsole;
    
    @Value("${print.debug.real:false}")
    private boolean realPrint;

    public PrinterService(ObjectProvider<PrinterService> selfProvider) {
        this.selfProvider = selfProvider;
    }

    /**
     * [신규 추가] 파라미터 없이 현금 영수증 통(돈통) Open
     * OS에 설정된 '기본 프린터'를 대상으로 돈통 오픈 신호를 보냅니다.
     */
    public void openCashDrawer() {
        // 이름 대신 null을 전달하여 내부적으로 기본 프린터를 찾도록 유도합니다.
        openCashDrawer(null);
    }
    
    /**
     * [신규 추가] 현금 영수증 통(돈통) Open
     * 기존 printBytes 메서드를 직접 호출(this)하지 않고, 주입된 의존성(Proxy)을 통해 비동기로 호출합니다.
     */
    public void openCashDrawer(String printerName) {
        log.info("현금 돈통 Open 신호 전송 시작 - 프린터: {}", printerName);
        
        PrinterService self = selfProvider.getIfAvailable();
        if (self != null) {
            // 스프링이 제공하는 프록시 객체(self)를 거쳐 호출하므로 printBytes의 @Async가 정상 작동합니다.
            self.printBytes(printerName, ESC_POS_OPEN_DRAWER);
        } else {
            // Fallback: 컨텍스트 라이프사이클 이슈 발생 시 직접 호출 처리
           log.error("돈통 오픈 실패: PrinterService 프록시 빈을 로드할 수 없습니다.");
        }
    }

    @Async("printExecutor")
    public void printBytes(String printerName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.warn("인쇄 데이터가 없습니다.");
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
            log.info("[DEV MODE] 실제 프린터로 출력하지 않음 (realPrint=false)");
        }
    }

   /**
     * Console에 프린트 내용 출력 (디버깅용) - 조건부 로깅 사용
     */
    private void printToConsole(String printerName, byte[] bytes) {
        // 조건부 로깅: 로그 레벨이 INFO 이상일 때만 실행
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n").append("=".repeat(70)).append("\n");
            sb.append("🖨️ [PRINT DEBUG] - Printer: ").append(printerName).append("\n");
            sb.append("📊 Data size: ").append(bytes.length).append(" bytes\n");
            sb.append("=".repeat(70)).append("\n");
            
            try {
                String content = new String(bytes, StandardCharsets.UTF_8);
                sb.append(content).append("\n");
            } catch (Exception e) {
                sb.append("⚠️ 디코딩 실패: ").append(e.getMessage()).append("\n");
            }
            
            sb.append("=".repeat(70)).append("\n");
            sb.append("✅ Console output only\n");
            
            log.info(sb.toString());
        }
    }
    

    /**
     * 실제 프린터로 출력
     */
    private void printToRealPrinter(String printerName, byte[] bytes) {
        PrintService service = findPrintService(printerName);
        if (service == null) {
            log.error("인쇄 실패: 프린터 '{}'를 찾을 수 없습니다.", printerName);
            return;
        }

        try {
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            pras.add(new JobName("POS-Receipt-" + System.currentTimeMillis(), Locale.getDefault()));

            DocPrintJob job = service.createPrintJob();
            Doc doc = new SimpleDoc(bytes, FLAVOR, null);

            log.info("프린터 '{}'로 데이터 전송 시작 ({} bytes)", service.getName(), bytes.length);
            job.print(doc, pras);
            
            Thread.sleep(100); 

        } catch (PrintException e) {
            log.error("인쇄 중 하드웨어 에러 발생: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void listAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(FLAVOR, null);
        for (PrintService s : services) {
            log.info("사용 가능한 프린터: {}", s.getName());
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
        
        log.warn("'{}'를 찾지 못해 기본 프린터를 사용합니다.", printerName);
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}