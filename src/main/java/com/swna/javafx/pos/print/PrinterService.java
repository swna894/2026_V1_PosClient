package com.swna.javafx.pos.print;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class PrinterService {

    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);
    
    // 재사용 가능한 속성 설정
    private static final DocFlavor FLAVOR = DocFlavor.BYTE_ARRAY.AUTOSENSE;

    /**
     * 비동기 방식으로 바이트 데이터 출력
     * @Async를 통해 호출 즉시 UI 스레드에 제어권을 반환합니다.
     */
    @Async("printExecutor") // 별도의 ThreadPoolTaskExecutor 사용 권장
    public void printBytes(String printerName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;

        PrintService service = findPrintService(printerName);
        if (service == null) {
            logger.error("인쇄 실패: 프린터 '{}'를 찾을 수 없습니다.", printerName);
            return;
        }

        try {
            // 작업 이름 지정 (윈도우 프린터 스풀러에서 확인 가능)
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            pras.add(new JobName("POS-Receipt-" + System.currentTimeMillis(), Locale.getDefault()));

            DocPrintJob job = service.createPrintJob();
            Doc doc = new SimpleDoc(bytes, FLAVOR, null);

            logger.info("프린터 '{}'로 데이터 전송 시작 ({} bytes)", service.getName(), bytes.length);
            job.print(doc, pras);
            
            // 전송 후 장치 안정화 시간
            Thread.sleep(100); 

        } catch (PrintException e) {
            logger.error("인쇄 중 하드웨어 에러 발생: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 사용 가능한 모든 프린터 목록 조회 (디버깅 및 설정용)
     */
    public void listAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(FLAVOR, null);
        for (PrintService s : services) {
            logger.info("사용 가능한 프린터: {}", s.getName());
        }
    }

    /**
     * 특정 이름의 프린터 찾기 (실패 시 기본 프린터 반환)
     */
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