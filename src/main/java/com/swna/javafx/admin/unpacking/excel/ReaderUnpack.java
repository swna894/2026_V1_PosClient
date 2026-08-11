package com.swna.javafx.admin.unpacking.excel;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.swna.javafx.admin.unpacking.model.UnpackItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReaderUnpack {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ReaderUnpack() {
        /* Utility class should not be instantiated */
    }

    /**
     * File 객체로부터 Excel/CSV를 읽어 UnpackItem 목록으로 변환
     */
    public static List<UnpackItem> read(File file) {
        if (file == null || !file.exists()) {
            log.warn("File is null or does not exist.");
            return new ArrayList<>();
        }

        try (Workbook workbook = WorkbookFactory.create(file)) {
            return parseSheet(workbook.getSheetAt(0));
        } catch (Exception e) {
            log.error("Failed to read Excel file: {}", file.getName(), e);
            return new ArrayList<>();
        }
    }

    /**
     * InputStream으로부터 Excel/CSV를 읽어 UnpackItem 목록으로 변환
     */
    public static List<UnpackItem> readAvalonFile(InputStream inputStream) {
        if (inputStream == null) {
            log.warn("InputStream is null.");
            return new ArrayList<>();
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseSheet(workbook.getSheetAt(0));
        } catch (Exception e) {
            log.error("Failed to read Excel InputStream", e);
            return new ArrayList<>();
        }
    }

    private static List<UnpackItem> parseSheet(Sheet sheet) {
        List<UnpackItem> itemList = new ArrayList<>();
        if (sheet == null) {
            log.warn("The specified sheet is null.");
            return itemList;
        }

        // Row 0: Header Row
        int headerRowIndex = 0;
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            log.warn("Header row (index {}) is missing.", headerRowIndex);
            return itemList;
        }

        // 헤더 컬럼 이름 매핑 (대소문자 구분 없음)
        Map<String, Integer> headerMap = buildHeaderMap(headerRow);

        // Data Rows: Row 1부터 파싱
        int startRow = headerRowIndex + 1;
        int lastRow = sheet.getLastRowNum();

        for (int r = startRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            UnpackItem item = parseRowToModel(row, headerMap, r);
            
            // 바코드, 코드, 설명 등의 유효성 검사 (유효한 데이터만 추가)
            if (isValidItem(item)) {
                itemList.add(item);
            }
        }

        log.info("Successfully parsed {} items from sheet.", itemList.size());
        return itemList;
    }

    private static UnpackItem parseRowToModel(Row row, Map<String, Integer> headerMap, int rowNum) {
        UnpackItem item = new UnpackItem();
        item.setLineNo(rowNum + 1); // Excel 행 번호 기준 (1-based index)

        String code = getCellValueByHeaderAsString(row, headerMap, "code");
        String description = getCellValueByHeaderAsString(row, headerMap, "description");
        String barcode = getCellValueByHeaderAsString(row, headerMap, "barcode");
        int qty = getCellValueByHeaderAsInt(row, headerMap, "qty");
        BigDecimal cost = getCellValueByHeaderAsBigDecimal(row, headerMap, "cost");

        item.setCode(code);
        item.setDescription(description);
        item.setBarcode(barcode);
        item.setQty(qty);
        item.setPricein(cost);

        // BigDecimal 정밀 연산 및 소수점 2자리 반올림
        BigDecimal totalAmount = cost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        item.setAmount(totalAmount);
        
        // UI 기본 상태 설정
        item.setConfirm(false);
        item.setIsSaved(false);
        item.setIsNew(true);

        return item;
    }

    /**
     * 바코드, 코드, 설명 중 하나라도 데이터가 있는 정상 Row인지 확인
     */
    private static boolean isValidItem(UnpackItem item) {
        return (item.getBarcode() != null && !item.getBarcode().isBlank())
            || (item.getCode() != null && !item.getCode().isBlank())
            || (item.getDescription() != null && !item.getDescription().isBlank());
    }

    /**
     * 대소문자 구분 없는 Header Map 생성
     */
    private static Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                String headerName = getCellValueAsString(cell);
                if (!headerName.isBlank()) {
                    headerMap.put(headerName.toLowerCase(), cell.getColumnIndex());
                }
            }
        }
        return headerMap;
    }

    // ---------------- Cell Value Extractors ----------------

    private static String getCellValueByHeaderAsString(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return "";
        return getCellValueAsString(row.getCell(colIndex));
    }

    private static int getCellValueByHeaderAsInt(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return 0;
        return getCellValueAsInt(row.getCell(colIndex));
    }

    private static BigDecimal getCellValueByHeaderAsBigDecimal(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return BigDecimal.ZERO;
        return getCellValueAsBigDecimal(row.getCell(colIndex));
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static int getCellValueAsInt(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                String strVal = cell.getStringCellValue().trim().replaceAll("[^0-9-]", "");
                return strVal.isEmpty() ? 0 : Integer.parseInt(strVal);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            } else if (cell.getCellType() == CellType.STRING) {
                String strVal = cell.getStringCellValue().trim().replaceAll("[^0-9.-]", "");
                return strVal.isEmpty() ? BigDecimal.ZERO : new BigDecimal(strVal).setScale(2, RoundingMode.HALF_UP);
            } else if (cell.getCellType() == CellType.FORMULA) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("Failed to parse cell value to BigDecimal: cell={}", cell, e);
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String str = FORMATTER.formatCellValue(cell).trim();
                if (!str.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}