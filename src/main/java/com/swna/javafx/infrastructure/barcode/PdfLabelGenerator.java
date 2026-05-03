package com.swna.javafx.infrastructure.barcode;

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
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfLabelGenerator {

    private final BarcodeGenerator barcodeGenerator;

    // 설정값: 이 값을 변경하면 바코드 크기와 배치 개수가 자동으로 조절됩니다.
    private static final int COLS = 4; 
    private static final int ROWS = 10;
    private static final String FILE_NAME = "dynamic_labels_optimized.pdf";

    /**
     * 내부 메서드의 파라미터 폭주를 방지하기 위한 Parameter Object[cite: 10, 11]
     */
    private record LabelContext(
        Document document,
        ProductLabelDto dto,
        float x,
        float y,
        float labelWidth,
        float labelHeight,
        float fontSize,
        float barcodeWidth,
        float barcodeHeight
    ) {}

    public void generate(List<ProductLabelDto> products) throws Exception {
        if (products == null || products.isEmpty()) {
            log.warn("상품 목록이 비어 있어 PDF 생성을 중단합니다.");
            return;
        }

        String finalOutputPath = Paths.get(System.getProperty("user.home"), "Downloads", FILE_NAME).toString();
        
        try (PdfWriter writer = new PdfWriter(finalOutputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            // 1. 페이지 가용 영역 계산[cite: 11]
            float margin = 25f; 
            float pageWidth = PageSize.A4.getWidth() - (margin * 2);
            float pageHeight = PageSize.A4.getHeight() - (margin * 2);

            // 2. 단일 라벨의 크기 동적 계산[cite: 11]
            float labelWidth = pageWidth / COLS;
            float labelHeight = pageHeight / ROWS;

            // 3. 라벨 크기에 비례한 구성 요소 수치 결정[cite: 10, 11]
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

            log.info("PDF 생성 완료: {}", finalOutputPath);
        } catch (Exception e) {
            log.error("PDF 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    private void renderLabel(LabelContext ctx) throws Exception {
        // 1. Y축 시작점 계산[cite: 11]
        float currentY = ctx.y() + ctx.labelHeight() - ctx.fontSize() - 4;

        // 2. 동적 글자 수 제한 계산[cite: 11]
        float avgCharWidth = ctx.fontSize() * 0.65f; 
        int dynamicMaxLength = (int) (ctx.labelWidth() / avgCharWidth);
        
        String description = ctx.dto().description();
        if (description != null && description.length() > dynamicMaxLength) {
            description = description.substring(0, Math.max(0, dynamicMaxLength - 1)) + "..";
        }

        // 3. 상품명 출력 (중앙 정렬)
        ctx.document().add(new Paragraph(description)
                .setFontSize(ctx.fontSize())
                .setMultipliedLeading(1.0f) 
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 4. 가격 출력 (상품명 아래)
        currentY -= (ctx.fontSize() + 2);
        ctx.document().add(new Paragraph("₩ " + ctx.dto().price())
                .setFontSize(ctx.fontSize())
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));

        // 5. 바코드 이미지 출력 (중앙 정렬 및 간격 조정)[cite: 6, 7, 11]
        byte[] imageBytes = barcodeGenerator.generate(ctx.dto().barcode());
        Image image = new Image(ImageDataFactory.create(imageBytes));
        
        float imageX = ctx.x() + (ctx.labelWidth() - ctx.barcodeWidth()) / 2;
        
        // 가격과 바코드 사이 간격(5f) 추가
        currentY -= (ctx.barcodeHeight() + 5f); 
        
        image.setFixedPosition(imageX, currentY);
        image.setWidth(ctx.barcodeWidth());
        image.setHeight(ctx.barcodeHeight());
        ctx.document().add(image);

        // 6. 바코드 번호 출력 (바코드 아래 간격 추가)
        // 이미지 하단과 텍스트 사이 여백을 위해 fontSize의 1.2배만큼 간격을 줍니다.
        currentY -= (ctx.fontSize() * 1.2f); 
        ctx.document().add(new Paragraph(ctx.dto().barcode())
                .setFontSize(ctx.fontSize() * 0.75f) 
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedPosition(ctx.x(), currentY, ctx.labelWidth()));
    }
}