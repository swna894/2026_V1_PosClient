package com.swna.javafx.infrastructure.barcode;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BarcodeGenerator {

    /**
     * Generates a Code128 barcode as a PNG byte array.
     * @param code The text to encode
     * @return PNG byte array
     */
    public byte[] generate(String code) throws Exception {
        log.debug("Generating Barcode for data: [{}]", code); // 상세 데이터 추적

        try {
            Code128Writer writer = new Code128Writer();
            // Code128 인코딩 수행
            BitMatrix matrix = writer.encode(code, BarcodeFormat.CODE_128, 160, 35);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate barcode for code [{}]: {}", code, e.getMessage());
            throw e; // 예외를 상위 계층으로 전파
        }
    }
}
