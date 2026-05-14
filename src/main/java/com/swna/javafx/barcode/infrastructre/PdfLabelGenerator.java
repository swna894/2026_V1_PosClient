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

    private static final int DEFAULT_COLS = 3;
    private static final int DEFAULT_ROWS = 13;
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

    /**
     * 기본 레이아웃 (cols=3, rows=13)으로 PDF 생성
     */
    public void generate(List<BarcodeLabelDto> products) throws Exception {
        generate(products, DEFAULT_COLS, DEFAULT_ROWS);
    }

    /**
     * 사용자 지정 cols, rows로 PDF 생성
     */
    public void generate(List<BarcodeLabelDto> products, int cols, int rows) throws Exception {
        if (products == null || products.isEmpty()) {
            log.warn("상품 목록이 비어 있어 PDF 생성을 중단합니다.");
            return;
        }

        if (cols <= 0 || rows <= 0) {
            log.warn("잘못된 레이아웃 값 (cols={}, rows={}), 기본값 사용", cols, rows);
            cols = DEFAULT_COLS;
            rows = DEFAULT_ROWS;
        }

        String finalOutputPath = Paths.get(System.getProperty("user.home"), "Downloads", FILE_NAME).toString();
        
        try (PdfWriter writer = new PdfWriter(finalOutputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            float margin = 25f; 
            float pageWidth = PageSize.A4.getWidth() - (margin * 2);
            float pageHeight = PageSize.A4.getHeight() - (margin * 2);

            float labelWidth = pageWidth / cols;
            float labelHeight = pageHeight / rows;

            float fontSize = Math.min(labelHeight * 0.12f, 11f); 
            float barcodeWidth = labelWidth * 0.85f;            
            float barcodeHeight = labelHeight * 0.35f;           

            int labelsPerPage = cols * rows;

            log.info("PDF 생성 시작: {}개 상품, 레이아웃 {}x{} ({} 라벨/페이지)", 
                     products.size(), cols, rows, labelsPerPage);

            for (int i = 0; i < products.size(); i++) {
                if (i > 0 && i % labelsPerPage == 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }

                int indexInPage = i % labelsPerPage;
                int col = indexInPage % cols;
                int row = indexInPage / cols;

                float x = margin + (col * labelWidth);
                float y = (PageSize.A4.getHeight() - margin) - ((row + 1) * labelHeight);

                LabelContext ctx = new LabelContext(
                    document, products.get(i), x, y, 
                    labelWidth, labelHeight, fontSize, barcodeWidth, barcodeHeight
                );
                renderLabel(ctx);
            }
            
            log.info("PDF 생성 완료: {}", finalOutputPath);
        } catch (Exception e) {
            log.error("PDF 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    private void renderLabel(LabelContext ctx) throws Exception {
        float descFontSize = ctx.fontSize() * 0.85f;
        float currentY = ctx.y() + ctx.labelHeight() - descFontSize - 4;

        float avgCharWidth = descFontSize * 0.65f; 
        int dynamicMaxLength = (int) (ctx.labelWidth() / avgCharWidth);
        
        String description = ctx.dto().description();
        if (description != null && description.length() > dynamicMaxLength) {
            description = description.substring(0, Math.max(0, dynamicMaxLength - 1)) + "..";
        }

        // 1. 상품명
        ctx.document().add(new Paragraph(description)
                .setFontSize(descFontSize)
                .setMultipliedLeading(1.0f) 
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 2. 가격
        currentY -= (ctx.fontSize() + 2);
        ctx.document().add(new Paragraph("$ " + ctx.dto().price())
                .setFontSize(ctx.fontSize())
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 3. 바코드 이미지
        byte[] imageBytes = barcodeGenerator.generate(ctx.dto().barcode());
        Image image = new Image(ImageDataFactory.create(imageBytes));
        
        float imageX = ctx.x() + (ctx.labelWidth() - ctx.barcodeWidth()) / 2;
        currentY -= (ctx.barcodeHeight() + 5f); 
        
        image.setFixedPosition(imageX, currentY);
        image.setWidth(ctx.barcodeWidth());
        image.setHeight(ctx.barcodeHeight());
        ctx.document().add(image);

        // 4. 바코드 번호
        float barcodeFontSize = ctx.fontSize() * 0.9f; 
        currentY -= (barcodeFontSize + 2f); 

        ctx.document().add(new Paragraph(ctx.dto().barcode())
                .setFontSize(barcodeFontSize)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));
    }
}