package com.swna.javafx.pos.print;

import lombok.Getter;

@Getter
public enum ReceiptStyle {
    SIZE_58MM(32, "-", "*"), // 2인치 프린터 (보통 32자)
    SIZE_80MM(42, "-", "*"); // 3인치 프린터 (보통 42~48자)

    private final int width;
    private final String dashChar;
    private final String starChar;

    ReceiptStyle(int width, String dashChar, String starChar) {
        this.width = width;
        this.dashChar = dashChar;
        this.starChar = starChar;
    }

    // 구분선 생성
    public String getLine(boolean isStar) {
        return (isStar ? starChar : dashChar).repeat(width);
    }

    // 중앙 정렬
    public String center(String text) {
        if (text == null || text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    // 양 끝 정렬 (항목명 ........ 금액)
    public String justify(String left, String right) {
        int spaceNeeded = width - left.length();
        if (spaceNeeded <= 0) return left + " " + right; 
        return left + " ".repeat(Math.max(1, spaceNeeded - right.length())) + right;
    }

    // 타이틀이 포함된 구분선 (--- Notice ---)
    public String getNoticeLine(String title) {
        if (title.length() + 4 >= width) return title;
        int side = (width - title.length() - 4) / 2;
        return dashChar.repeat(side) + "  " + title + "  " + dashChar.repeat(side);
    }
}