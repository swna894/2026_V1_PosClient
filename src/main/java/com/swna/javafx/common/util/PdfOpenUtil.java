package com.swna.javafx.common.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF Open Utility
 */
@Slf4j
public class PdfOpenUtil {

    private PdfOpenUtil() {
    }

    /**
     * PDF 파일 열기
     */
    public static void open(String fileName) {

        try {
            Path path = Path.of(fileName);
            // 파일 존재 여부 확인
            if (!Files.exists(path)) {
                log.error( "PDF FILE NOT FOUND : {}", fileName );
                return;
            }

            // Desktop 지원 여부
            if (!Desktop.isDesktopSupported()) {
                log.error( "DESKTOP NOT SUPPORTED" );
                return;
            }

            Desktop desktop = Desktop.getDesktop();

            // OPEN 지원 여부
            if (!desktop.isSupported( Desktop.Action.OPEN )) {
                log.error(  "OPEN ACTION NOT SUPPORTED" );
                return;
            }
            log.info(  "OPEN PDF : {}", fileName );
            desktop.open(new File(fileName) );

            log.info( "PDF OPEN COMPLETE" );
        } catch (Exception e) {
            log.error( "PDF OPEN ERROR",  e );
        }
    }
}
