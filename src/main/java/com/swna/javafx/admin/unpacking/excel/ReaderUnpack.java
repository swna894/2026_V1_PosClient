package com.swna.javafx.admin.unpacking.excel;

import java.io.File;
import java.io.InputStream;
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

    private ReaderUnpack() {
        /* This utility class should not be instantiated */
    }

    /**
     * Reads an Excel/CSV file from a File object and returns a list of UnpackItem.
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
     * Reads an Excel/CSV file from an InputStream and returns a list of UnpackItem.
     */
    public static List<UnpackItem> readAvalonFile(InputStream inputStream) {
        if (inputStream == null) {
            log.warn("InputStream is null.");
            return new ArrayList<>();
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseSheet(workbook.getSheetAt(0));
        } catch (Exception e) {
            log.error("Failed to read Excel InputStream.", e);
            return new ArrayList<>();
        }
    }

    private static List<UnpackItem> parseSheet(Sheet sheet) {
        List<UnpackItem> itemList = new ArrayList<>();
        if (sheet == null) {
            log.warn("The specified sheet is null.");
            return itemList;
        }

        DataFormatter formatter = new DataFormatter();

        // Row 0 is configured as the header row
        int headerRowIndex = 0;
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            log.warn("Header row (index {}) is missing.", headerRowIndex);
            return itemList;
        }

        // Build case-insensitive header map
        Map<String, Integer> headerMap = buildHeaderMap(headerRow, formatter);

        // Read data starting from Row 1
        int startRow = headerRowIndex + 1; 
        
        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            UnpackItem item = new UnpackItem();
            item.setLineNo(r);

            // Mapping data based on case-insensitive header text
            item.setCode(getCellValueByHeaderAsString(row, headerMap, "code", formatter));
            item.setDescription(getCellValueByHeaderAsString(row, headerMap, "description", formatter));
            item.setBarcode(getCellValueByHeaderAsString(row, headerMap, "barcode", formatter));
            item.setQty(getCellValueByHeaderAsInt(row, headerMap, "qty"));
            
            // Cost is mapped to PriceIn (or PriceOut depending on model definition)
            double cost = getCellValueByHeaderAsDouble(row, headerMap, "cost");
            item.setPricein(cost); 

            itemList.add(item);
        }

        return itemList;
    }

    /**
     * Constructs a header map where keys are stored in lowercase for case-insensitive matching.
     */
    private static Map<String, Integer> buildHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerMap = new HashMap<>();
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                if (cell != null) {
                    String headerName = getCellValueAsString(cell, formatter);
                    if (!headerName.isEmpty()) {
                        // Store header name in lowercase
                        headerMap.put(headerName.toLowerCase(), cell.getColumnIndex());
                    }
                }
            }
        }
        return headerMap;
    }

    // Extracts cell value as String using header name (case-insensitive)
    private static String getCellValueByHeaderAsString(Row row, Map<String, Integer> headerMap, String headerName, DataFormatter formatter) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return "";
        return getCellValueAsString(row.getCell(colIndex), formatter);
    }

    // Extracts cell value as int using header name (case-insensitive)
    private static int getCellValueByHeaderAsInt(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return 0;
        return getCellValueAsInt(row.getCell(colIndex));
    }

    // Extracts cell value as double using header name (case-insensitive)
    private static double getCellValueByHeaderAsDouble(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer colIndex = headerMap.get(headerName.toLowerCase());
        if (colIndex == null) return 0.0;
        return getCellValueAsDouble(row.getCell(colIndex));
    }

    // Cell value utility method (String)
    private static String getCellValueAsString(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    // Cell value utility method (Integer)
    private static int getCellValueAsInt(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // Cell value utility method (Double)
    private static double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // Checks whether the row is empty
    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}