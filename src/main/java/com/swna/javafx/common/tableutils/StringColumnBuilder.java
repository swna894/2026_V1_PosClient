package com.swna.javafx.common.tableutils;


import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringColumnBuilder<S> {

    // ========== CSS Style Constants ==========
    private static final String STYLE_CENTER = "-fx-alignment: CENTER; -fx-padding: 0 4 0 4;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT; -fx-padding: 0 4 0 4;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT; -fx-padding: 0 4 0 4;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";

    private static final String STYLE_EDIT_TEXTFIELD =
        "-fx-background-color: white; -fx-border-color: #6F4CBB; " +
        "-fx-border-width: 0.5px; -fx-background-radius: 4px; -fx-border-radius: 4px; -fx-padding: 0 8 0 8; " +
        "-fx-background-insets: 2 0 2 0; -fx-border-insets: 2 0 2 0; " +
        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; " +
        "-fx-effect: dropshadow(gaussian, rgba(111,76,187,0.25), 4, 0, 0, 2);";

    private static final String STYLE_EDIT_TEXTFIELD_FOCUSED =
        "-fx-background-color: white; -fx-border-color: #4A90D9; " +
        "-fx-border-width: 0.5px; -fx-background-radius: 4px; -fx-border-radius: 4px; -fx-padding: 0 8 0 8; " +
        "-fx-background-insets: 2 0 2 0; -fx-border-insets: 2 0 2 0; " +
        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; " +
        "-fx-effect: dropshadow(gaussian, rgba(74,144,217,0.35), 6, 0, 0, 2);";

    public static <S> StringColumnBuilder<S> stringColumn(
            TableView<S> tableView,
            String title,
            Function<S, StringProperty> propertyGetter) {
        return new StringColumnBuilder<>(tableView, title, propertyGetter);
    }

    private final TableView<S> tableView;
    private final String title;
    private final Function<S, StringProperty> propertyGetter;

    private BiConsumer<S, String> setter = null;
    private boolean editable = false;
    private boolean isVisible = true;
    private String alignment = null;
    
    // 너비 및 리사이즈 관련 필드
    private int width = -1;
    private int minWidth = -1;
    private int maxWidth = -1;
    private boolean resizable = true;

    private TextField searchField = null;
    private Color textColor = Color.BLACK;
    private boolean wrapText = false;
    private double lineSpacing = 0.0;

    // 타 클래스 의존성 없이 자바 표준 Consumer<S> 사용[cite: 5]
    private Consumer<S> dirtyConsumer = null;

    public StringColumnBuilder(TableView<S> tableView, String title, Function<S, StringProperty> propertyGetter) {
        this.tableView = tableView;
        this.title = title;
        this.propertyGetter = propertyGetter;
    }

    public StringColumnBuilder<S> setter(BiConsumer<S, String> setter) {
        this.setter = setter;
        return this;
    }

    public StringColumnBuilder<S> editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    // 독자적인 dirtyConsumer 메서드 추가
    public StringColumnBuilder<S> dirtyConsumer(Consumer<S> dirtyConsumer) {
        this.dirtyConsumer = dirtyConsumer;
        return this;
    }

    public StringColumnBuilder<S> visible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    public StringColumnBuilder<S> alignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    // 기본 선호 너비 (가변 컬럼 기본값으로 활용)
    public StringColumnBuilder<S> width(int width) {
        this.width = width;
        return this;
    }

    // 최소 너비 설정
    public StringColumnBuilder<S> minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    // 최대 너비 설정
    public StringColumnBuilder<S> maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    // 컬럼 리사이즈 가능 여부 설정
    public StringColumnBuilder<S> resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    // [고정 컬럼 전용] 너비를 완전히 고정하고 리사이즈를 방지
    public StringColumnBuilder<S> fixedWidth(int width) {
        this.width = width;
        this.minWidth = width;
        this.maxWidth = width;
        this.resizable = true;
        return this;
    }

    public StringColumnBuilder<S> textColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }

    public StringColumnBuilder<S> wrapText(boolean wrapText) {
        this.wrapText = wrapText;
        return this;
    }

    public StringColumnBuilder<S> lineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    public StringColumnBuilder<S> highlight(TextField searchField) {
        this.searchField = searchField;
        return this;
    }

    public TableColumn<S, String> build() {
        TableColumn<S, String> column = new TableColumn<>(title);

        if (width > 0) column.setPrefWidth(width);
        if (minWidth > 0) column.setMinWidth(minWidth);
        if (maxWidth > 0) column.setMaxWidth(maxWidth);
        column.setResizable(resizable);

        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);

        if (alignment != null) {
            column.setStyle(TableColumnUtils.STYLE_TRANSPARENT +
                            TableColumnUtils.getAlignmentStyle(alignment));
        }

        // 컬럼 alignment(-fx-alignment)를 셀 내부 텍스트 정렬(TextAlignment)로 변환
        // (wrapText 시 Text가 셀 전체 폭을 차지하므로, 박스 정렬만으로는 글자 정렬이 되지 않음)
        final TextAlignment textAlignment = toTextAlignment(alignment);

        // CellFactory 분기
        if (searchField != null) {
            column.setCellFactory(col -> new HighlightTableCell<>(searchField, textColor, wrapText, lineSpacing, textAlignment));

            if (tableView != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> tableView.refresh());
            }
        } else if (editable) {
            column.setCellFactory(col -> new StringEditingCell<>(alignment));
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) { setter.accept(row, event.getNewValue()); }
                // 표준 Consumer 실행
                if (dirtyConsumer != null) { dirtyConsumer.accept(row); }
            });
        } else if (wrapText || !Color.BLACK.equals(textColor) || lineSpacing > 0) {
            column.setEditable(false);
            column.setCellFactory(col -> new CustomTextTableCell<>(textColor, wrapText, lineSpacing, textAlignment));
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }

        return column;
    }

    // ---------------- Helper Methods ----------------

    private static String getAlignmentStyle(String alignment) {
        if (alignment == null) return STYLE_CENTER;
        return switch (alignment.toUpperCase()) {
            case "RIGHT" -> STYLE_RIGHT;
            case "LEFT" -> STYLE_LEFT;
            default -> STYLE_CENTER;
        };
    }

    private static void setupTextFieldLayoutAndListeners(TextField textField, String alignment, TableCell<?, ?> cell) {
        double verticalMargin = 4.0;
        double targetHeight = Math.max(10, cell.getHeight() - verticalMargin);

        textField.setMinWidth(cell.getWidth() - cell.getInsets().getLeft() - cell.getInsets().getRight());
        textField.setMinHeight(targetHeight);
        textField.setPrefWidth(cell.getWidth());
        textField.setPrefHeight(targetHeight);

        String base = getAlignmentStyle(alignment);
        textField.setStyle(base + STYLE_EDIT_TEXTFIELD);

        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            boolean focused = Boolean.TRUE.equals(isFocused);
            textField.setStyle(base + (focused ? STYLE_EDIT_TEXTFIELD_FOCUSED : STYLE_EDIT_TEXTFIELD));
        });
    }

    private static class StringEditingCell<S> extends TableCell<S, String> {
        private final String alignment;
        private TextField textField;

        public StringEditingCell(String alignment) {
            this.alignment = alignment;
            setPadding(Insets.EMPTY);
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                Platform.runLater(textField::requestFocus);
        }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
            setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
                setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
                return;
        }

            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getItem() != null ? getItem() : "");
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item);
                setGraphic(null);
                setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        }
        }

        private void createTextField() {
            textField = new TextField(getItem() != null ? getItem() : "");
            setupTextFieldLayoutAndListeners(textField, alignment, this);

            textField.setOnAction(e -> commitEdit(textField.getText()));

            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (Boolean.FALSE.equals(isFocused) && isEditing()) {
                    Platform.runLater(() -> commitEdit(textField.getText()));
                }
            });
        }
    }

    /**
     * -fx-alignment 문자열(center, center-left, top-right 등)을 TextAlignment로 변환합니다.
     * 마지막 단어(left/right/center)를 기준으로 판단하며, 지정하지 않았거나 알 수 없으면 LEFT(기본값)입니다.
     */
    private static TextAlignment toTextAlignment(String alignment) {
        if (alignment == null) {
            return TextAlignment.LEFT;
        }
        String a = alignment.trim().toLowerCase();
        if (a.endsWith("right")) {
            return TextAlignment.RIGHT;
        }
        if (a.endsWith("left")) {
            return TextAlignment.LEFT;
        }
        if (a.contains("center")) {
            return TextAlignment.CENTER;
        }
        return TextAlignment.LEFT;
    }

    // [내부 클래스 1] 일반 커스텀 텍스트 셀
    private static class CustomTextTableCell<S> extends TableCell<S, String> {
        private final Text textNode = new Text();

        public CustomTextTableCell(Color textColor, boolean wrapText, double lineSpacing, TextAlignment textAlignment) {
            // textColor를 명시적으로 지정한 경우에만 그 색을 고정 적용
            // 지정하지 않은 경우(기본값 BLACK)에는 CSS 테마의 글자색을 따라가도록 함
            if (textColor != null && !Color.BLACK.equals(textColor)) {
                textNode.setFill(textColor);
            } else {
                textNode.getStyleClass().add("table-cell-text");
            }

            textNode.setLineSpacing(lineSpacing);
            // wrapText 시 여러 줄/짧은 글 모두 컬럼 정렬(가운데 등)대로 배치
            textNode.setTextAlignment(textAlignment);

            if (wrapText) {
                textNode.wrappingWidthProperty().bind(widthProperty().subtract(10));
            }
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                textNode.setText(item);
                setGraphic(textNode);
            }
        }
    }

    // [내부 클래스 2] 검색 강조 전용 TableCell
    private static class HighlightTableCell<S> extends TableCell<S, String> {
        private final TextField searchField;
        private final Color defaultTextColor;
        private final boolean wrapText;
        private final double lineSpacing;
        private final TextAlignment textAlignment;

        public HighlightTableCell(TextField searchField, Color defaultTextColor, boolean wrapText,
                                  double lineSpacing, TextAlignment textAlignment) {
            this.searchField = searchField;
            this.defaultTextColor = defaultTextColor != null ? defaultTextColor : Color.BLACK;
            this.wrapText = wrapText;
            this.lineSpacing = lineSpacing;
            this.textAlignment = textAlignment != null ? textAlignment : TextAlignment.LEFT;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String filterText = searchField.getText();

            if (filterText == null || filterText.trim().isEmpty()) {
                Text textNode = new Text(item);
                textNode.setFill(defaultTextColor);
                textNode.setLineSpacing(lineSpacing);
                textNode.setTextAlignment(textAlignment);
                if (wrapText) {
                    textNode.wrappingWidthProperty().bind(widthProperty().subtract(10));
                }
                setText(null);
                setGraphic(textNode);
                return;
            }

            TextFlow textFlow = createHighlightedTextFlow(item, filterText.trim());
            textFlow.setLineSpacing(lineSpacing);
            textFlow.setTextAlignment(textAlignment);
            if (wrapText) {
                textFlow.prefWidthProperty().bind(widthProperty().subtract(10));
            }
            setText(null);
            setGraphic(textFlow);
        }

        private TextFlow createHighlightedTextFlow(String fullText, String filter) {
            TextFlow textFlow = new TextFlow();
            Pattern pattern = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(fullText);

            int lastIndex = 0;
            while (matcher.find()) {
                if (matcher.start() > lastIndex) {
                    Text normalText = new Text(fullText.substring(lastIndex, matcher.start()));
                    normalText.setFill(defaultTextColor); 
                    textFlow.getChildren().add(normalText);
                }

                Text highlightedText = new Text(fullText.substring(matcher.start(), matcher.end()));
                highlightedText.setFill(Color.RED);
                highlightedText.setStyle("-fx-font-weight: bold;");
                textFlow.getChildren().add(highlightedText);

                lastIndex = matcher.end();
            }

            if (lastIndex < fullText.length()) {
                Text remainingText = new Text(fullText.substring(lastIndex));
                remainingText.setFill(defaultTextColor);
                textFlow.getChildren().add(remainingText);
            }

            return textFlow;
        }
    }
}