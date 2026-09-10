package com.swna.javafx.common.tableutils;

import java.util.function.Function;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public class TableColumnUtils {

   private TableColumnUtils() {}

       // 정렬 상수 추가
    public static final String CENTER = "CENTER";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    
    // 상수 정의
    public static final String STYLE_TRANSPARENT = "-fx-background-color: transparent; ";
    
    // 정렬 스타일 가져오기
    public static String getAlignmentStyle(String alignment) {
        if (alignment == null) return "";
        
        switch (alignment.toUpperCase()) {
            case CENTER:
                return "-fx-alignment: CENTER;";
            case RIGHT:
                return "-fx-alignment: CENTER-RIGHT;";
            case LEFT:
                return "-fx-alignment: CENTER-LEFT;";
            default:
                return "";
        }
    }
    
    // 정적 팩토리 메서드 (편의를 위해)
    public static <S> StringColumnBuilder<S> stringColumn(
            TableView<S> tableView,
            String title,
            Function<S, StringProperty> propertyGetter) {
        return new StringColumnBuilder<>(tableView, title, propertyGetter);
    }

    /**
     * 버튼 컬럼 생성을 위한 빌더를 반환합니다.
     */
    public static <S> ButtonColumnBuilder<S> buttonColumn(TableView<S> tableView) {
        return new ButtonColumnBuilder<>(tableView);
    }

    /**
     * 콤보박스 컬럼 생성을 위한 빌더를 반환합니다.
     */
    public static <S> ComboBoxColumnBuilder<S> comboBoxColumn(
            TableView<S> tableView,
            String title,
            Function<S, StringProperty> propertyGetter,
            Function<S, javafx.collections.ObservableList<String>> itemsGetter) {
        return new ComboBoxColumnBuilder<>(tableView, title, propertyGetter, itemsGetter);
    }

    /**
     * 자유 입력 + 옵션 자동추가가 가능한 편집형 ComboBox 컬럼 빌더를 반환합니다.
     * (TAG처럼 고정 목록이 아니라 사용자가 새 값을 입력할 수 있는 컬럼에 사용)
     */
    public static <S> EditableComboBoxColumnBuilder<S> editableComboBoxColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObservableValue<String>> propertyGetter,
            ObservableList<String> options) {
        return new EditableComboBoxColumnBuilder<>(tableView, title, propertyGetter, options);
    }

    /**
     * 검색 키워드 하이라이트(붉은색) 기능이 포함된 텍스트 컬럼 빌더를 반환합니다.
     * searchField의 검색 텍스트를 기준으로 매칭되는 문자를 붉은색으로 하이라이팅하는 컬럼 생성
        TableColumnUtils.highlightedTextColumn(tableView, "사유 및 제재 내용", Eval::reasonProperty)
                .searchField(searchField)
                .width(200)
                .alignment(TableColumnUtils.LEFT)
                .build();
     */
    public static <S, T> HighlightedTextColumnBuilder<S, T> highlightedTextColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObservableValue<T>> propertyGetter) {
        return new HighlightedTextColumnBuilder<>(tableView, title, propertyGetter);
    }

    /**
     * 숫자(Integer, Long, Double 등) 전용 컬럼 빌더를 반환합니다.
     */
    public static <S, N extends Number> NumberColumnBuilder<S, N> numberColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObservableValue<N>> propertyGetter) {
        return new NumberColumnBuilder<>(tableView, title, propertyGetter);
    }

}