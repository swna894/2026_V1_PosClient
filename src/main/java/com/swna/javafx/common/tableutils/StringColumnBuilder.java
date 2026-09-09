package com.swna.javafx.common.tableutils;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringColumnBuilder<S> {

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
        this.resizable = false;
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

        // 너비 및 가변/고정 옵션 적용
        if (width > 0) {
            column.setPrefWidth(width);
        }
        if (minWidth > 0) {
            column.setMinWidth(minWidth);
        }
        if (maxWidth > 0) {
            column.setMaxWidth(maxWidth);
        }
        column.setResizable(resizable);

        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);

        if (alignment != null) {
            column.setStyle(TableColumnUtils.STYLE_TRANSPARENT +
                            TableColumnUtils.getAlignmentStyle(alignment));
        }

        // CellFactory 분기
        if (searchField != null) {
            column.setCellFactory(col -> new HighlightTableCell<>(searchField, textColor, wrapText, lineSpacing));

            if (tableView != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> tableView.refresh());
            }
        } else if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        } else if (wrapText || !Color.BLACK.equals(textColor) || lineSpacing > 0) {
            column.setEditable(false);
            column.setCellFactory(col -> new CustomTextTableCell<>(textColor, wrapText, lineSpacing));
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }

        return column;
    }

    // [내부 클래스 1] 일반 커스텀 텍스트 셀
    private static class CustomTextTableCell<S> extends TableCell<S, String> {
        private final Text textNode = new Text();

    public CustomTextTableCell(Color textColor, boolean wrapText, double lineSpacing) {
        // textColor를 명시적으로 지정한 경우에만 그 색을 고정 적용
        // 지정하지 않은 경우(기본값 BLACK)에는 CSS 테마의 글자색을 따라가도록 함
        if (textColor != null && !Color.BLACK.equals(textColor)) {
            textNode.setFill(textColor);
        } else {
            textNode.getStyleClass().add("table-cell-text");
        }

        textNode.setLineSpacing(lineSpacing);

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

        public HighlightTableCell(TextField searchField, Color defaultTextColor, boolean wrapText, double lineSpacing) {
            this.searchField = searchField;
            this.defaultTextColor = defaultTextColor != null ? defaultTextColor : Color.BLACK;
            this.wrapText = wrapText;
            this.lineSpacing = lineSpacing;
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
                if (wrapText) {
                    textNode.wrappingWidthProperty().bind(widthProperty().subtract(10));
                }
                setText(null);
                setGraphic(textNode);
                return;
            }

            TextFlow textFlow = createHighlightedTextFlow(item, filterText.trim());
            textFlow.setLineSpacing(lineSpacing);
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