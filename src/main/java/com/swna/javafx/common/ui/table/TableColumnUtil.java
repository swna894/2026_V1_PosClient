package com.swna.javafx.common.ui.table;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableColumnUtil {

    // ========== Alignment Constants ==========
    public static final String CENTER = "CENTER";
    public static final String RIGHT = "RIGHT";
    public static final String LEFT = "LEFT";

    // ========== CSS Style Constants ==========
    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";
    private static final String BUTTON_STYLE_DEFAULT = "-fx-background-color:transparent; -fx-alignment: center;";
    private static final String BUTTON_STYLE_HOVER = "-fx-background-color:#6F4CBB;";

    // ========== Inner Classes ==========
    
    /**
     * Integer 편집 셀 내부 클래스
     */
    private static class IntegerEditingCell<T> extends TextFieldTableCell<T, Integer> {
        private final String alignment;
        private final UnaryOperator<TextFormatter.Change> numberFilter;
        
        public IntegerEditingCell(String alignment) {
            super(new IntegerStringConverter());
            this.alignment = alignment;
            this.numberFilter = change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty()) return change;
                return newText.matches("-?\\d*") ? change : null;
            };
            applyStyle();
        }
        
        private void applyStyle() {
            setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        }
        
        @Override
        public void startEdit() {
            super.startEdit();
            applyStyle();
            configureTextField();
        }
        
        @Override
        public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            applyStyle();
        }
        
        private void configureTextField() {
            TextField textField = (TextField) getGraphic();
            if (textField == null) return;
            
            textField.setTextFormatter(new TextFormatter<>(
                new IntegerStringConverter(), null, numberFilter
            ));
            textField.setStyle(getAlignmentStyle(alignment));
        }
    }
    
    /**
     * 통화 표시 셀 내부 클래스
     */
    private static class CurrencyCell<T> extends TableCell<T, Double> {
        private final NumberFormat currencyFormat;
        
        public CurrencyCell() {
            this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setMinimumFractionDigits(2);
            currencyFormat.setMaximumFractionDigits(2);
        }
        
        @Override
        protected void updateItem(Double price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
            } else {
                setText(currencyFormat.format(price));
            }
        }
    }
    
    /**
     * 날짜/시간 표시 셀 내부 클래스
     */
    private static class DateTimeCell<T> extends TableCell<T, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.toString());
            }
        }
    }
    
    /**
     * 버튼 컨테이너 셀 내부 클래스
     */
    private static class ButtonCell<T> extends TableCell<T, Void> {
        private final Button button;
        
        public ButtonCell(String iconPath, Consumer<T> action) {
            this.button = createButton(iconPath);
            configureButton(action);
        }
        
        private Button createButton(String iconPath) {
            Button btn = new Button();
            if (iconPath != null) {
                btn.setGraphic(loadIconView(iconPath));
            }
            btn.setAlignment(Pos.CENTER);
            btn.setMinWidth(45);
            btn.setPrefWidth(45);
            btn.setMinHeight(34);
            btn.setPrefHeight(34);
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btn.setPadding(Insets.EMPTY);
            btn.setStyle(BUTTON_STYLE_DEFAULT);
            return btn;
        }
        
        private void configureButton(Consumer<T> action) {
            button.setOnMouseEntered(e -> {
                getTableView().getSelectionModel().select(getIndex());
                button.setStyle(BUTTON_STYLE_HOVER);
            });
            button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLE_DEFAULT));
            button.setOnAction(e -> {
                T item = getTableRow().getItem();
                if (item != null) action.accept(item);
            });
        }
        
        @Override
        public void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : button);
            setAlignment(Pos.CENTER);
        }
    }
    
    /**
     * 라벨 컨테이너 셀 내부 클래스
     */
    private static class LabelCell<T> extends TableCell<T, Void> {
        private final Label label;
        
        public LabelCell(String iconPath, EventHandler<MouseEvent> actionEvent) {
            this.label = createLabel(iconPath);
            configureLabel(actionEvent);
        }
        
        private Label createLabel(String iconPath) {
            Label lbl = new Label();
            if (iconPath != null) {
                ImageView imageView = loadIconView(iconPath);
                if (imageView != null) {
                    imageView.setFitWidth(16);
                    imageView.setFitHeight(16);
                    lbl.setGraphic(imageView);
                }
            }
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setStyle(STYLE_TRANSPARENT);
            lbl.setCursor(Cursor.HAND);
            return lbl;
        }
        
        private void configureLabel(EventHandler<MouseEvent> actionEvent) {
            label.setOnMouseEntered(e -> {
                getTableView().getSelectionModel().select(getIndex());
                label.setStyle("-fx-background-color: #6F4CBB; -fx-padding: 10px;");
            });
            label.setOnMouseExited(e -> label.setStyle("-fx-background-color: transparent; -fx-padding: 10px;"));
            label.setOnMousePressed(actionEvent);
        }
        
        @Override
        public void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(label);
                setAlignment(Pos.CENTER);
            }
        }
    }

    // ========== Private Constructors ==========
    
    private TableColumnUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== Private Helper Methods ==========
    
    private static String getAlignmentStyle(String alignment) {
        if (alignment == null) return STYLE_CENTER;
        return switch (alignment.toUpperCase()) {
            case RIGHT -> STYLE_RIGHT;
            case LEFT -> STYLE_LEFT;
            default -> STYLE_CENTER;
        };
    }
    
    private static ImageView loadIconView(String iconPath) {
        if (iconPath == null) return null;
        URL url = TableColumnUtil.class.getResource(iconPath);
        if (url == null) return null;
        
        ImageView imageView = new ImageView(new Image(url.toExternalForm()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(22);
        imageView.setFitHeight(22);
        return imageView;
    }
    
    private static <T> void setupStaticColumnProps(TableColumn<T, Void> column, String title, String iconPath, Integer width) {
        if (title != null) column.setText(title);
        if (width != null) column.setPrefWidth(width);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
    }

    // ========== Public API - Number Column ==========
    
    public static <T> TableColumn<T, String> createNumberColumn(TableView<T> tableView, TableColumn<T, String> column, int width) {
        if (width != 0) column.setPrefWidth(width);
        column.setText("NO");
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(" " + (tableView.getItems().indexOf(p.getValue()) + 1) + " "));
        return column;
    }

    // ========== Public API - String Column ==========
    
    public static <T> void makeStringColumn(
            TableColumn<T, String> column,
            Function<T, StringProperty> propertyGetter,
            BiConsumer<T, String> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setSortable(false);

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Currency Column ==========
    
    public static <T> void makeCurrencyColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(tc -> new CurrencyCell<>());

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                DoubleProperty prop = propertyGetter.apply(row);
                prop.set(event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Integer Column ==========
    
    public static <T> void makeIntegerColumn(
            TableColumn<T, Integer> column,
            Function<T, IntegerProperty> propertyGetter,
            ObjIntConsumer<T> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(col -> new IntegerEditingCell<>(alignment));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                if (event.getNewValue() != null) {
                    setter.accept(row, event.getNewValue());
                }
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Long Column ==========
    
    public static <T> void makeReadOnlyLongColumn(TableColumn<T, Long> column, Function<T, LongProperty> propertyGetter, String alignment) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setEditable(false);
    }

    // ========== Public API - Double Column ==========
    
    public static <T> void makeDoubleColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            ObjDoubleConsumer<T> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - DateTime Column ==========
    
    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DateTimeCell<>());

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        }
    }

    // ========== Public API - DatePicker Column ==========
    
    public static <T> void makeDatePickerColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<T, LocalDateTime> setter,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> getter.apply(cell.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DatePickerTableCell<>(setter, dirtyConsumer));
        column.setEditable(true);
    }

    // ========== Public API - Boolean Column ==========
    
    public static <T> void makeBooleanColumn(
            TableColumn<T, Boolean> column,
            Function<T, BooleanProperty> propertyGetter,
            BiConsumer<T, Boolean> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> {
            BooleanProperty prop = propertyGetter.apply(cellData.getValue());
            if (editable) {
                prop.addListener((obs, oldVal, newVal) -> {
                    setter.accept(cellData.getValue(), newVal);
                    if (dirtyConsumer != null) dirtyConsumer.accept(cellData.getValue());
                });
            }
            return prop;
        });
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setEditable(editable);
    }

    // ========== Public API - Button Column ==========
    
    public static <T> void makeButtonColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            Consumer<T> action
    ) {
        setupStaticColumnProps(column, title, iconPath, width);
        column.setCellFactory(param -> new ButtonCell<>(iconPath, action));
    }

    // ========== Public API - Label Column ==========
    
    public static <T> void makeLabelColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {
        setupStaticColumnProps(column, title, iconPath, width);
        column.setCellFactory(param -> new LabelCell<>(iconPath, actionEvent));
    }
}