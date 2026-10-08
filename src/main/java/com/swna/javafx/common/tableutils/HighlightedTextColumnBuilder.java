package com.swna.javafx.common.tableutils;

import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * JavaFX TableView의 컬럼을 생성하고, 검색 필드(TextField)의 키워드와
 * 일치하는 텍스트를 하이라이트 색상(기본: 붉은색)으로 강조 표시하는 Builder 클래스입니다.
 *
 * @param <S> TableView에 바인딩되는 데이터 모델 타입
 * @param <T> TableColumn 셀에 바인딩되는 데이터 타입
 */
public class HighlightedTextColumnBuilder<S, T> {

    /** 컬럼이 추가될 대상 TableView */
    private final TableView<S> tableView;

    /** 컬럼 헤더에 표시될 제목 */
    private final String title;

    /** 각 행(Row) 객체에서 바인딩할 ObservableValue 속성을 추출하는 함수 */
    private final Function<S, ObservableValue<T>> propertyGetter;

    /** 셀 데이터 ↔ 문자열 변환기 (null 허용) */
    private StringConverter<T> converter = null;

    /** 하이라이트 감지용 검색 키워드 입력 필드 (null 허용) */
    private TextField searchField = null;

    /** 셀 편집 가능 여부 (기본값: false) */
    private boolean editable = false;

    /** 컬럼 표시 여부 (기본값: true) */
    private boolean isVisible = true;

    /** 컬럼 정렬 방식 ("LEFT", "CENTER", "RIGHT") */
    private String alignment = null;

    /** 컬럼 너비 (픽셀 단위, 미지정 시 -1) */
    private int width = -1;

    /** 검색 키워드 강조 색상 (기본값: RED) */
    private Color highlightColor = Color.RED;

    /** 기본 텍스트 글자 색상 (기본값: WHITE - 다크 배경 대응) */
    private Color textColor = Color.WHITE;

    /** 줄 간격 (기본값: 0.0) */
    private double lineSpacing = 0.0;

    /**
     * HighlightedTextColumnBuilder 생성자
     *
     * @param tableView      컬럼을 추가할 TableView 인스턴스
     * @param title          컬럼 헤더 제목
     * @param propertyGetter 셀 값을 추출하는 Property Getter 함수
     */
    public HighlightedTextColumnBuilder(TableView<S> tableView, String title, Function<S, ObservableValue<T>> propertyGetter) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
    }

    /**
     * 셀 값과 문자열 간 변환을 수행할 StringConverter를 설정합니다.
     *
     * @param converter 문자열 변환기
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> converter(StringConverter<T> converter) {
        this.converter = converter;
        return this;
    }

    /**
     * 하이라이트 텍스트 매칭 기준이 될 검색어 TextField를 설정합니다.
     *
     * @param searchField 검색어를 입력받는 TextField 컨트롤
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> searchField(TextField searchField) {
        this.searchField = searchField;
        return this;
    }

    /**
     * 컬럼의 셀 편집 가능 여부를 설정합니다.
     *
     * @param editable true 설정 시 편집 가능
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    /**
     * 컬럼의 가시성을 설정합니다.
     *
     * @param visible true 설정 시 화면에 표시
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> visible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    /**
     * 셀 텍스트의 정렬 방식을 설정합니다.
     *
     * @param alignment 정렬 문자열 ("LEFT", "CENTER", "RIGHT")
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> alignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * 컬럼의 너비를 설정합니다.
     *
     * @param width 너비 값 (픽셀 단위)
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> width(int width) {
        this.width = width;
        return this;
    }

    /**
     * 매칭된 검색 키워드의 하이라이트 글자 색상을 설정합니다.
     *
     * @param highlightColor 하이라이트 적용 색상 (javafx.scene.paint.Color)
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> highlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
        return this;
    }

    /**
     * 기본 텍스트의 글자 색상을 설정합니다.
     *
     * @param textColor 일반 텍스트 색상 (javafx.scene.paint.Color)
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> textColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }

    /**
     * 셀 텍스트의 줄 간격을 설정합니다.
     *
     * @param lineSpacing 줄 간격 (픽셀 단위)
     * @return 현재 빌더 인스턴스 (Fluent API)
     */
    public HighlightedTextColumnBuilder<S, T> lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    /**
     * 설정된 옵션을 바탕으로 최종 TableColumn 객체를 생성하고 TableView에 등록합니다.
     *
     * @return 구성된 TableColumn 인스턴스
     */
    public TableColumn<S, T> build() {
        TableColumn<S, T> column = new TableColumn<>(title);

        configureColumnProperties(column);
        configureCellFactory(column);

        if (tableView != null) {
            // 가변 행 높이가 동작하려면 TableView가 고정 셀 크기 모드가 아니어야 합니다.
            // (fixedCellSize > 0 이면 row.setPrefHeight()/setMinHeight() 호출이 전부 무시됨)
            tableView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
            tableView.getColumns().add(column);
        }

        return column;
    }

    /**
     * 컬럼의 정렬, 너비, 가시성, 정렬 스타일 등 기본 속성을 구성합니다.
     *
     * @param column 대상 TableColumn
     */
    private void configureColumnProperties(TableColumn<S, T> column) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);

        if (width > 0) {
            column.setPrefWidth(width);
        }

        if (alignment != null) {
            column.setStyle(TableColumnUtils.STYLE_TRANSPARENT + TableColumnUtils.getAlignmentStyle(alignment));
        }
    }

    /**
     * 편집 가능 여부에 맞춰 적절한 CellFactory를 생성 및 바인딩합니다.
     *
     * @param column 대상 TableColumn
     */
    private void configureCellFactory(TableColumn<S, T> column) {
        if (editable) {
            column.setCellFactory(createEditableCellFactory());
        } else {
            column.setCellFactory(tc -> new HighlightedTableCell(column));
        }
    }

    /**
     * 편집 가능한 컬럼용 TextFieldTableCell 팩토리를 생성합니다.
     *
     * @return Callback 형태의 CellFactory
     */
    private Callback<TableColumn<S, T>, TableCell<S, T>> createEditableCellFactory() {
        if (converter != null) {
            return TextFieldTableCell.forTableColumn(converter);
        }
        return tc -> new TextFieldTableCell<>();
    }

    /**
     * 검색 키워드 하이라이팅 및 동적 행 높이 자동 조절을 전담하는 Custom TableCell 내부 클래스입니다.
     *
     * 기존 구현과의 차이점:
     *  - TextFlow를 셀마다 새로 생성하지 않고 재사용 (그래픽 노드 재사용으로 안정성 향상)
     *  - 높이는 실제 화면에 표시되는 TextFlow 자체를 기준으로 측정합니다.
     *    (별도의 오프스크린 Text로 측정하면 CSS로 지정된 실제 폰트가 적용되지
     *    않아 높이가 실제보다 작게 나오고, 마지막 줄이 잘려 보이는 문제가 생깁니다.)
     *  - 다만 라이브 노드의 bounds를 리스너로 "계속" 관찰하지는 않습니다.
     *    (그렇게 하면 "row 높이 변경 → 리레이아웃 → bounds 재변경 → 다시 계산"
     *    형태의 피드백 루프가 생겨 행이 끝없이 커지는 문제가 발생합니다.)
     *    대신 렌더링 시점에 1회, 다음 레이아웃 펄스에서 보정 1회, 총 두 번만
     *    계산하여 정확도와 안정성을 함께 확보합니다.
     *  - min/pref 뿐 아니라 max 높이도 함께 지정하여, 외부 CSS나 다른 로직이
     *    최대 높이를 제한해 내용이 잘리는 상황을 방지
     */
    private class HighlightedTableCell extends TableCell<S, T> {

        /** 현재 셀이 속한 TableColumn 참조 */
        private final TableColumn<S, T> column;

        /** 셀마다 재사용되는 TextFlow (매 updateItem마다 새로 만들지 않음) */
        private final TextFlow textFlow = new TextFlow();

        /**
         * HighlightedTableCell 생성자
         *
         * @param column 연결된 TableColumn 객체
         */
        HighlightedTableCell(TableColumn<S, T> column) {
            this.column = column;
            setGraphic(textFlow);

            // 컬럼 너비 변경 시 텍스트 Wrapping 너비 및 행 높이 재계산
            this.column.widthProperty().addListener((obs, oldW, newW) -> {
                if (getItem() != null && !isEmpty()) {
                    renderHighlightedText(getItem());
                }
            });
        }

        /**
         * 셀 항목 업데이트 처리 메서드
         *
         * @param item  셀에 표시될 데이터 객체
         * @param empty 빈 셀 여부
         */
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                resetCell();
                return;
            }

            renderHighlightedText(item);
        }

        /**
         * 빈 셀인 경우 기존 그래픽 및 텍스트, 높이를 초기화합니다.
         */
        private void resetCell() {
            setText(null);
            textFlow.getChildren().clear();

            resetHeights();
        }

        /**
         * 셀과 부모 TableRow의 높이를 기본(계산) 크기로 되돌립니다.
         */
        private void resetHeights() {
            setPrefHeight(Region.USE_COMPUTED_SIZE);
            setMinHeight(Region.USE_COMPUTED_SIZE);
            setMaxHeight(Region.USE_COMPUTED_SIZE);

            TableRow<S> row = getTableRow();
            if (row != null) {
                row.setPrefHeight(Region.USE_COMPUTED_SIZE);
                row.setMinHeight(Region.USE_COMPUTED_SIZE);
                row.setMaxHeight(Region.USE_COMPUTED_SIZE);
            }
        }

        /**
         * 셀 데이터를 텍스트로 변환하고, 하이라이트가 적용된 TextFlow를 렌더링한 뒤
         * 필요한 행 높이를 계산해 반영합니다.
         *
         * 높이는 실제 화면에 표시되는 {@code textFlow} 자체를 기준으로 측정합니다.
         * (별도의 오프스크린 Text로 측정하면 CSS로 지정된 실제 폰트 크기가
         * 적용되지 않아 높이가 실제보다 작게 계산되고, 그 결과 마지막 줄이
         * 잘려서 안 보이는 문제가 생깁니다.)
         *
         * 다만 라이브 노드의 bounds를 "리스너로 계속" 관찰하면 이전에 겪었던
         * 무한 증식 피드백 루프가 재발하므로, 여기서는 딱 두 번만 계산합니다:
         *  1) 즉시 1회 (현재 펄스 기준)
         *  2) 다음 레이아웃 펄스에서 1회 보정 (최초 렌더 시점에 CSS/폰트가
         *     아직 완전히 적용되지 않았을 수 있는 타이밍 이슈 대응)
         * 이후로는 추가로 재귀 관찰을 하지 않으므로 루프가 생기지 않습니다.
         *
         * @param item 셀 표출 데이터
         */
        private void renderHighlightedText(T item) {
            String textValue = (converter != null) ? converter.toString(item) : item.toString();
            String keyword = (searchField != null) ? searchField.getText() : null;

            // 셀 패딩을 고려한 실질 텍스트 기준 너비 설정
            double targetWidth = Math.max(column.getWidth() - 20, 50);

            textFlow.getChildren().clear();
            populateHighlightedTextFlow(textFlow, textValue, keyword);
            textFlow.setMaxWidth(targetWidth);
            textFlow.setPrefWidth(targetWidth);
            textFlow.setLineSpacing(lineSpacing);
            setText(null);

            recalculateAndApplyHeight(item);

            // CSS/폰트 적용 타이밍 보정을 위한 1회성 재확인 (반복 리스너 아님)
            Platform.runLater(() -> recalculateAndApplyHeight(item));
        }

        /**
         * 실제 렌더링 중인 {@code textFlow}를 기준으로 CSS/레이아웃을 강제 적용한 뒤
         * 필요한 높이를 측정하여 셀/행에 반영합니다.
         *
         * @param expectedItem 계산 시점에 이 셀이 여전히 표시하고 있어야 할 데이터
         *                     (재사용된 셀이 그 사이 다른 행으로 바뀐 경우 무시하기 위함)
         */
        private void recalculateAndApplyHeight(T expectedItem) {
            if (isEmpty() || getItem() != expectedItem) {
                return;
            }

            // CSS(폰트) 적용 후, "현재 크기"가 아닌 "해당 너비에서 필요한 높이"를 계산합니다.
            // (getBoundsInLocal()은 재사용 중인 노드의 이전 크기가 남아 있어,
            //  한 줄짜리 내용도 가장 높은 행의 높이로 측정되는 문제가 있었음)
            double targetWidth = Math.max(column.getWidth() - 20, 50);
            textFlow.applyCss();
            double measuredHeight = textFlow.prefHeight(targetWidth);

            if (measuredHeight <= 0) {
                // 내용이 없으면 이전(재사용) 높이가 남지 않도록 기본 높이로 복원
                resetHeights();
                return;
            }

            applyHeight(measuredHeight + 10.0);
        }

        /**
         * 계산된 높이를 셀과 부모 TableRow에 min/pref/max 모두 적용합니다.
         * (max까지 지정해야 외부 CSS 등에 의한 clipping을 방지할 수 있습니다.)
         *
         * @param calculatedHeight 계산된 높이 값
         */
        private void applyHeight(double calculatedHeight) {
            setPrefHeight(calculatedHeight);
            setMinHeight(calculatedHeight);
            setMaxHeight(calculatedHeight);

            TableRow<S> row = getTableRow();
            if (row != null) {
                applyRowHeight(row, calculatedHeight);
            } else {
                // TableRow가 아직 null인 시점(최초 바인딩 전) 대응
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null) {
                        applyRowHeight(newRow, calculatedHeight);
                    }
                });
            }
        }

        /**
         * TableRow에 계산된 높이를 min/pref/max 모두 적용합니다.
         *
         * @param row              대상 TableRow
         * @param calculatedHeight 계산된 높이 값
         */
        private void applyRowHeight(TableRow<S> row, double calculatedHeight) {
            row.setMinHeight(calculatedHeight);
            row.setPrefHeight(calculatedHeight);
            row.setMaxHeight(calculatedHeight);
        }

        /**
         * 전체 텍스트를 줄바꿈(`\n`) 단위로 분할하여, 키워드 하이라이트가 처리된
         * 내용을 지정된 TextFlow에 채워 넣습니다.
         *
         * @param textFlow 대상 TextFlow (재사용되는 인스턴스)
         * @param text     전체 셀 문자열
         * @param keyword  하이라이트 대상 검색 키워드
         */
        private void populateHighlightedTextFlow(TextFlow textFlow, String text, String keyword) {
            if (text == null || text.isEmpty()) {
                return;
            }

            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                appendLineToTextFlow(textFlow, lines[i], keyword);
                if (i < lines.length - 1) {
                    textFlow.getChildren().add(new Text("\n"));
                }
            }
        }

        /**
         * 개별 라인에 키워드가 존재하는지 검사하고 적절한 텍스트 노드를 TextFlow에 추가합니다.
         *
         * @param textFlow 대상 TextFlow 컨테이너
         * @param line     검사할 단일 행 문자열
         * @param keyword  검색 키워드
         */
        private void appendLineToTextFlow(TextFlow textFlow, String line, String keyword) {
            if (!isKeywordPresent(line, keyword)) {
                textFlow.getChildren().add(createText(line, textColor));
                return;
            }
            appendHighlightedSegments(textFlow, line, keyword);
        }

        /**
         * 라인 내에 유효한 검색 키워드가 포함되어 있는지 판단합니다 (대소문자 무시).
         *
         * @param line    검사할 행 문자열
         * @param keyword 검색 키워드
         * @return 키워드 포함 여부 (boolean)
         */
        private boolean isKeywordPresent(String line, String keyword) {
            return keyword != null
                    && !keyword.trim().isEmpty()
                    && line.toLowerCase().contains(keyword.toLowerCase());
        }

        /**
         * 키워드를 기준으로 라인을 매칭 구간과 일반 구간으로 분할하여 색상을 각각 적용합니다.
         *
         * @param textFlow 대상 TextFlow 컨테이너
         * @param line     매칭을 수행할 단일 행 문자열
         * @param keyword  검색 키워드
         */
        private void appendHighlightedSegments(TextFlow textFlow, String line, String keyword) {
            String lowerLine = line.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();
            int lastEnd = 0;
            int index;

            while ((index = lowerLine.indexOf(lowerKeyword, lastEnd)) >= 0) {
                if (index > lastEnd) {
                    textFlow.getChildren().add(createText(line.substring(lastEnd, index), textColor));
                }
                textFlow.getChildren().add(createText(line.substring(index, index + keyword.length()), highlightColor));
                lastEnd = index + keyword.length();
            }

            if (lastEnd < line.length()) {
                textFlow.getChildren().add(createText(line.substring(lastEnd), textColor));
            }
        }

        /**
         * 주어진 내용과 전경색(Color)을 가진 JavaFX Text 노드를 생성합니다.
         *
         * @param content 표시할 텍스트 문자열
         * @param color   텍스트 색상
         * @return 구성된 Text 객체
         */
        private Text createText(String content, Color color) {
            Text text = new Text(content);
            text.setFill(color);
            return text;
        }
    }
}