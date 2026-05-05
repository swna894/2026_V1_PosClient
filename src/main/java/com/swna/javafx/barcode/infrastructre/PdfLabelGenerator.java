package com.swna.javafx.barcode.infrastructre;

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
import com.swna.javafx.barcode.dto.BarcodeLabelDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfLabelGenerator {

    private final BarcodeGenerator barcodeGenerator;

    private static final int COLS = 5; 
    private static final int ROWS = 13;
    private static final String FILE_NAME = "barcode_labels.pdf";

    private record LabelContext(
        Document document,
        BarcodeLabelDto dto,
        float x,
        float y,
        float labelWidth,
        float labelHeight,
        float fontSize,
        float barcodeWidth,
        float barcodeHeight
    ) {}

    public void generate(List<BarcodeLabelDto> products) throws Exception {
        if (products == null || products.isEmpty()) {
            log.warn("상품 목록이 비어 있어 PDF 생성을 중단합니다.");
            return;
        }

        String finalOutputPath = Paths.get(System.getProperty("user.home"), "Downloads", FILE_NAME).toString();
        
        try (PdfWriter writer = new PdfWriter(finalOutputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            float margin = 25f; 
            float pageWidth = PageSize.A4.getWidth() - (margin * 2);
            float pageHeight = PageSize.A4.getHeight() - (margin * 2);

            float labelWidth = pageWidth / COLS;
            float labelHeight = pageHeight / ROWS;

            // 기본 폰트 크기 계산 (전체 레이아웃의 기준점)
            float fontSize = Math.min(labelHeight * 0.12f, 11f); 
            float barcodeWidth = labelWidth * 0.85f;            
            float barcodeHeight = labelHeight * 0.35f;           

            int labelsPerPage = COLS * ROWS;

            for (int i = 0; i < products.size(); i++) {
                if (i > 0 && i % labelsPerPage == 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }

                int indexInPage = i % labelsPerPage;
                int col = indexInPage % COLS;
                int row = indexInPage / COLS;

                float x = margin + (col * labelWidth);
                float y = (PageSize.A4.getHeight() - margin) - ((row + 1) * labelHeight);

                LabelContext ctx = new LabelContext(
                    document, products.get(i), x, y, 
                    labelWidth, labelHeight, fontSize, barcodeWidth, barcodeHeight
                );
                renderLabel(ctx);
            }
        } catch (Exception e) {
            log.error("PDF 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    private void renderLabel(LabelContext ctx) throws Exception {
        // [수정] 상품명 전용 폰트 크기 (기본값의 85%로 축소)
        float descFontSize = ctx.fontSize() * 0.85f;
        float currentY = ctx.y() + ctx.labelHeight() - descFontSize - 4;

        // [수정] 작아진 폰트에 맞춰 글자 수 제한 다시 계산 (더 많은 글자 수용 가능)
        float avgCharWidth = descFontSize * 0.65f; 
        int dynamicMaxLength = (int) (ctx.labelWidth() / avgCharWidth);
        
        String description = ctx.dto().description();
        if (description != null && description.length() > dynamicMaxLength) {
            description = description.substring(0, Math.max(0, dynamicMaxLength - 1)) + "..";
        }

        // 1. 상품명 (축소된 폰트 적용)[cite: 7, 11]
        ctx.document().add(new Paragraph(description)
                .setFontSize(descFontSize)
                .setMultipliedLeading(1.0f) 
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 2. 가격 (기본 폰트 크기 유지하여 강조)[cite: 7, 11]
        currentY -= (ctx.fontSize() + 2);
        ctx.document().add(new Paragraph("$ " + ctx.dto().price())
                .setFontSize(ctx.fontSize())
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 3. 바코드 이미지[cite: 6, 7, 11]
        byte[] imageBytes = barcodeGenerator.generate(ctx.dto().barcode());
        Image image = new Image(ImageDataFactory.create(imageBytes));
        
        float imageX = ctx.x() + (ctx.labelWidth() - ctx.barcodeWidth()) / 2;
        currentY -= (ctx.barcodeHeight() + 5f); 
        
        image.setFixedPosition(imageX, currentY);
        image.setWidth(ctx.barcodeWidth());
        image.setHeight(ctx.barcodeHeight());
        ctx.document().add(image);

        // 4. 바코드 번호 (이전 요청대로 큼직하게 유지)[cite: 7, 11]
        float barcodeFontSize = ctx.fontSize() * 0.9f; 
        currentY -= (barcodeFontSize + 2f); 

        ctx.document().add(new Paragraph(ctx.dto().barcode())
                .setFontSize(barcodeFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));
    }
}