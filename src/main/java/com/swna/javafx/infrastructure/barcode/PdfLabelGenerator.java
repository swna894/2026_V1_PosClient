package com.swna.javafx.infrastructure.barcode;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Component;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfLabelGenerator {

    private final BarcodeGenerator barcodeGenerator;

    private static final int COLS = 5;
    private static final int ROWS = 13;
    private static final int LABELS_PER_PAGE = COLS * ROWS; // 페이지당 최대 개수 (65개)
    private static final String FILE_NAME = "labels_multi_page.pdf";

    private String getDownloadPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Downloads", FILE_NAME).toString();
    }

    /**
     * @param products 출력할 상품 DTO 목록
     */
    public void generate(List<ProductLabelDto> products) throws Exception {
        if (products == null || products.isEmpty()) {
            log.warn("Product list is empty. PDF generation skipped.");
            return;
        }

        String finalOutputPath = getDownloadPath();
        
        try (PdfWriter writer = new PdfWriter(finalOutputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            float startX = 15;
            float startY = 800; // 첫 번째 행의 Y 좌표
            float labelWidth = 110;
            float labelHeight = 58;

            for (int i = 0; i < products.size(); i++) {
                // 핵심: 페이지당 개수를 초과하면 새로운 페이지 추가
                if (i > 0 && i % LABELS_PER_PAGE == 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }

                ProductLabelDto dto = products.get(i);
                
                // 현재 페이지 내에서의 위치 계산 (0 ~ 64)
                int indexInPage = i % LABELS_PER_PAGE;
                int col = indexInPage % COLS;
                int row = indexInPage / COLS;

                float x = startX + (col * labelWidth);
                float y = startY - (row * labelHeight);

                renderLabel(document, dto, x, y);
            }

            log.info("PDF Multi-page generation completed. Total labels: {}", products.size());
        } catch (Exception e) {
            log.error("Critical error during PDF generation: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void renderLabel(Document document, ProductLabelDto dto, float x, float y) throws Exception {
        float labelWidth = 110;

        // 1. 상품명 (중앙 정렬)[cite: 7, 9]
        document.add(new Paragraph(dto.description())
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(x, y, labelWidth));

        // 2. 가격 (중앙 정렬)[cite: 7, 9]
        document.add(new Paragraph("$" + dto.price())
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(x, y - 8, labelWidth));

        // 3. 바코드 이미지 (중앙 정렬)[cite: 6, 7]
        byte[] imageBytes = barcodeGenerator.generate(dto.barcode());
        Image image = new Image(ImageDataFactory.create(imageBytes));
        
        float imageWidth = 90;
        float imageX = x + (labelWidth - imageWidth) / 2; 
        
        image.setFixedPosition(imageX, y - 32);
        image.setWidth(imageWidth);
        image.setHeight(20);
        document.add(image);

        // 4. 바코드 번호 (중앙 정렬)[cite: 7, 9]
        document.add(new Paragraph(dto.barcode())
                .setFontSize(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(x, y - 40, labelWidth));
    }
}