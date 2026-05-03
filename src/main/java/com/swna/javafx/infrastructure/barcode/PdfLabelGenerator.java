package com.swna.javafx.infrastructure.barcode;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PdfLabelGenerator {

    private final BarcodeGenerator barcodeGenerator;

    private static final int COLS = 5;
    private static final int ROWS = 13;

    public void generate(
            List<ProductLabelDto> products
    ) throws Exception {

        PdfWriter writer =
                new PdfWriter("labels65.pdf");

        PdfDocument pdf =
                new PdfDocument(writer);

        Document document =
                new Document(pdf, PageSize.A4);

        float startX = 15;
        float startY = 800;

        float labelWidth = 110;
        float labelHeight = 58;

        for (int i = 0; i < products.size(); i++) {

            ProductLabelDto dto =
                    products.get(i);

            int col = i % COLS;
            int row = i / COLS;

            float x =
                    startX + (col * labelWidth);

            float y =
                    startY - (row * labelHeight);
            Paragraph name =
                    new Paragraph(dto.name())
                            .setFontSize(7)
                            .setFixedPosition(
                                    x,
                                    y,
                                    100
                            );

            document.add(name);

            Paragraph price =
                    new Paragraph("₩" + dto.price())
                            .setFontSize(7)
                            .setFixedPosition(
                                    x,
                                    y - 8,
                                    100
                            );

            document.add(price);

            byte[] imageBytes =
                    barcodeGenerator.generate(
                            dto.code()
                    );

            Image image =
                    new Image(
                            ImageDataFactory.create(imageBytes)
                    );

            image.setFixedPosition(
                    x,
                    y - 38
            );

            image.setWidth(90);
            image.setHeight(20);

            document.add(image);

            Paragraph code =
                    new Paragraph(dto.code())
                            .setFontSize(6)
                            .setFixedPosition(
                                    x,
                                    y - 46,
                                    100
                            );

            document.add(code);
        }

        document.close();
    }
}