package com.swna.javafx.infrastructure.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class BarcodeGenerator {

    public byte[] generate(String code) throws Exception {

        Code128Writer writer =
                new Code128Writer();

        BitMatrix matrix =
                writer.encode(
                        code,
                        BarcodeFormat.CODE_128,
                        160,
                        35
                );

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                matrix,
                "PNG",
                baos
        );

        return baos.toByteArray();
    }
}
