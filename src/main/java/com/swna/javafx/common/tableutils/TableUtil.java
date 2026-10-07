
package com.swna.javafx.common.tableutils;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;
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
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableUtil {

    // ========== Alignment Constants ==========
    public static final String CENTER = "CENTER";
    public static final String RIGHT = "RIGHT";
    public static final String LEFT = "LEFT";

    // ========== CSS Style Constants ==========
    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";

    // ========== Public API - Number Column ==========
    
    public static <S> TableColumn<S, String> createNumberColumn(TableView<S> tableView, String title, int width) {
        TableColumn<S, String> column = new TableColumn<>(title != null ? title : "NO");
        if (width > 0) {
            column.setPrefWidth(width);
            column.setMaxWidth(width);
            column.setMinWidth(width);
        }
        column.setSortable(true);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(" " + (tableView.getItems().indexOf(p.getValue()) + 1) + " "));
        
        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - String Column ==========
    
    public static <S> TableColumn<S, String> makeStringColumn(
            TableView<S> tableView,
            String title, 
            Function<S, StringProperty> propertyGetter,
            BiConsumer<S, String> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, String> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);

        if (alignment != null) {
            column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        }

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Currency Column ==========
    
    public static <S> TableColumn<S, Double> makeCurrencyColumn(
            TableView<S> tableView,
            String title,
            Function<S, DoubleProperty> propertyGetter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, Double> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                DoubleProperty prop = propertyGetter.apply(row);
                if (prop != null) {
                    prop.set(event.getNewValue());
                }
            });
        } else {
            column.setCellFactory(tc -> new CurrencyCell<>());
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Integer Column ==========
    
    public static <S> TableColumn<S, Integer> makeIntegerColumn(
            TableView<S> tableView,
            String title,
            Function<S, IntegerProperty> propertyGetter,
            ObjIntConsumer<S> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, Integer> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(col -> new IntegerEditingCell<>(alignment));
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (event.getNewValue() != null && setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Long Column ==========
    
    public static <S> TableColumn<S, Long> makeReadOnlyLongColumn(
            TableView<S> tableView, 
            String title,
            Function<S, LongProperty> propertyGetter, 
            String alignment,
            int width
    ) {
        TableColumn<S, Long> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setSortable(true);
        column.setEditable(false);

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Double Column ==========
    
    public static <S> TableColumn<S, Double> makeDoubleColumn(
            TableView<S> tableView,
            String title,
            Function<S, DoubleProperty> propertyGetter,
            ObjDoubleConsumer<S> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, Double> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - DateTime Column ==========
    
    public static <S> TableColumn<S, LocalDateTime> makeDateTimeColumn(
            TableView<S> tableView,
            String title,
            Function<S, LocalDateTime> getter,
            BiConsumer<S, LocalDateTime> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, LocalDateTime> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DateTimeCell<>());

        if (editable) {
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - DateTime Column with Format ==========

    public static <S> TableColumn<S, LocalDateTime> makeDateTimeColumn(
            TableView<S> tableView,
            String title,
            Function<S, LocalDateTime> getter,
            BiConsumer<S, LocalDateTime> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            DateTimeFormatter formatter,
            int width
    ) {
        TableColumn<S, LocalDateTime> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new FormattedDateTimeCell<>(formatter));

        if (editable) {
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                if (setter != null) {
                    setter.accept(row, event.getNewValue());
                }
            });
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - DatePicker Column ==========
    
    public static <S> TableColumn<S, LocalDateTime> makeDatePickerColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<S, LocalDateTime> setter,
            String alignment,
            int width
    ) {
        TableColumn<S, LocalDateTime> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cell -> getter.apply(cell.getValue()));
        column.setSortable(true);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DatePickerTableCell<>(setter));
        column.setEditable(true);

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - BigDecimal Currency Column ==========

    public static <S> TableColumn<S, BigDecimal> makeBigDecimalCurrencyColumn(
            TableView<S> tableView,
            String title,
            Function<S, ObjectProperty<BigDecimal>> propertyGetter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, BigDecimal> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
            column.setOnEditCommit(event -> {
                S row = event.getRowValue();
                ObjectProperty<BigDecimal> prop = propertyGetter.apply(row);
                if (prop != null) {
                    prop.set(event.getNewValue());
                }
            });
        } else {
            column.setCellFactory(tc -> new BigDecimalCurrencyCell<>());
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Boolean Column ==========
    
    public static <S> TableColumn<S, Boolean> makeBooleanColumn(
            TableView<S> tableView,
            String title,
            Function<S, BooleanProperty> propertyGetter,
            BiConsumer<S, Boolean> setter,
            boolean editable,
            int width
    ) {
        TableColumn<S, Boolean> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        column.setSortable(true);
        column.setCellValueFactory(cellData -> {
            BooleanProperty prop = propertyGetter.apply(cellData.getValue());
            if (editable && prop != null) {
                prop.addListener((obs, oldVal, newVal) -> {
                    if (setter != null) {
                        setter.accept(cellData.getValue(), newVal);
                    }
                });
            }
            return prop;
        });
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setEditable(editable);

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - Button Column (ButtonColumnBuilder로 완전 위임) ==========
    
    public static <S> TableColumn<S, Void> makeButtonColumn(
            TableView<S> tableView,
            String title,
            boolean isVisible,
            String iconPath,
            Integer width,
            BiConsumer<S, ActionEvent> action
    ) {
        ButtonColumnBuilder<S> builder = new ButtonColumnBuilder<>(tableView)
                .title(title)
                .visible(isVisible)
                .iconPath(iconPath)
                .action(action);

        if (width != null && width > 0) {
            builder.width(width);
        }

        return builder.build();
    }

    // ========== Public API - Label Column ==========
    
    public static <S> TableColumn<S, Void> makeLabelColumn(
            TableView<S> tableView,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {
        TableColumn<S, Void> column = new TableColumn<>(title);
        setupStaticColumnProps(column, title, iconPath, width);
        column.setCellFactory(param -> new LabelCell<>(iconPath, actionEvent));

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Public API - CheckBox Header Column ==========
    
    public static <S> TableColumn<S, Boolean> createCheckBoxHeaderColumn(
            TableView<S> tableView, 
            Function<S, BooleanProperty> property
    ) {
        CheckBox headerCheckBox = createHeaderCheckBox(tableView, property);
        TableColumn<S, Boolean> column = new TableColumn<>();
        column.setGraphic(headerCheckBox);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setSortable(true);
        column.setPrefWidth(50);
        column.setCellValueFactory(cellData -> property.apply(cellData.getValue()));
        column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
        
        tableView.getColumns().add(column);
        return column;
    }

    public static <S> TableColumn<S, Boolean> createCheckBoxHeaderColumn(
            TableView<S> tableView,
            Function<S, BooleanProperty> property,
            String title,
            int width
    ) {
        TableColumn<S, Boolean> column = new TableColumn<>();
        CheckBox headerCheckBox = createHeaderCheckBox(tableView, property);
        configureColumn(column, headerCheckBox, title, width);
        if (width > 0) {
            column.setPrefWidth(width);
            column.setMaxWidth(width);
            column.setMinWidth(width);
        }
        column.setCellValueFactory(cellData -> property.apply(cellData.getValue()));
        column.setCellFactory(tc -> createCheckBoxCell(tableView, headerCheckBox, property));
        column.setEditable(true);
        
        tableView.getColumns().add(column);
        return column;
    }

    // ========== Public API - ComboBox Column (String 전용) ==========

    public static <S> TableColumn<S, String> makeComboBoxColumn(
            TableView<S> tableView,
            String title,
            Function<S, StringProperty> propertyGetter,
            Function<S, javafx.collections.ObservableList<String>> itemsGetter,
            BiConsumer<S, String> setter,
            boolean editable,
            boolean isVisible,
            String alignment,
            int width
    ) {
        TableColumn<S, String> column = new TableColumn<>(title);
        if (width > 0) column.setPrefWidth(width);
        
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setSortable(true);
        column.setVisible(isVisible);
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.ComboBox<String> comboBox = new javafx.scene.control.ComboBox<>();
                private boolean isUpdating = false;

                {
                    comboBox.setMaxWidth(Double.MAX_VALUE);
                    comboBox.setStyle("-fx-background-color: transparent;");
                    
                    comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                        if (isUpdating) return;
                        S rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                        if (rowItem != null) {
                            StringProperty prop = propertyGetter.apply(rowItem);
                            if (prop != null) prop.set(newVal);
                            if (setter != null) setter.accept(rowItem, newVal);
                        }
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        S rowItem = getTableRow().getItem();
                        isUpdating = true;
                        
                        comboBox.setItems(itemsGetter.apply(rowItem));
                        
                        StringProperty prop = propertyGetter.apply(rowItem);
                        if (prop != null) {
                            comboBox.getSelectionModel().select(prop.get());
                        } else {
                            comboBox.getSelectionModel().select(item);
                        }
                        
                        isUpdating = false;
                        setGraphic(comboBox);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            });
        } else {
            column.setEditable(false);
        }

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    // ========== Private Helper Methods for CheckBox ==========

    private static <S> void configureColumn(
            TableColumn<S, Boolean> column,
            CheckBox headerCheckBox,
            String title,
            int width
    ) {
        column.setGraphic(headerCheckBox);
        if (title != null) column.setText(title);
        column.setStyle(STYLE_CENTER);
        column.setSortable(true);
        column.setPrefWidth(width > 0 ? width : 50);
    }

    private static <S> TableCell<S, Boolean> createCheckBoxCell(
            TableView<S> tableView,
            CheckBox headerCheckBox,
            Function<S, BooleanProperty> property
    ) {
        return new TableCell<S, Boolean>() {
            private final CheckBox checkBox = createCheckBox();
            private final HBox container = createContainer(checkBox);

            {
                setupCheckBoxListener(tableView, headerCheckBox, property);
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                updateCellContent(empty);
            }
            
            private void updateCellContent(boolean empty) {
                if (isInvalidCell(empty)) {
                    setGraphic(null);
                    return;
                }
                
                S rowItem = getTableRow().getItem();
                BooleanProperty prop = property.apply(rowItem);
                
                if (prop != null) {
                    checkBox.setSelected(prop.get());
                    setGraphic(container);
                    setAlignment(Pos.CENTER);
                } else {
                    setGraphic(null);
                }
            }
            
            private boolean isInvalidCell(boolean empty) {
                return empty || getTableRow() == null || getTableRow().getItem() == null;
            }
            
            private void setupCheckBoxListener(
                    TableView<S> tableView,
                    CheckBox headerCheckBox,
                    Function<S, BooleanProperty> property
            ) {
                checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    S item = getTableRow().getItem();
                    if (item == null) return;
                    
                    BooleanProperty prop = property.apply(item);
                    if (prop == null || prop.get() == isSelected) return;
                    
                    prop.set(isSelected);
                    updateHeaderCheckBoxState(tableView, headerCheckBox, property);
                });
            }
        };
    }

    private static CheckBox createCheckBox() {
        CheckBox checkBox = new CheckBox();
        checkBox.setStyle(STYLE_TRANSPARENT);
        return checkBox;
    }

    private static HBox createContainer(CheckBox checkBox) {
        HBox container = new HBox(checkBox);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 0, 0, 0));
        return container;
    }

    private static <S> CheckBox createHeaderCheckBox(TableView<S> tableView, Function<S, BooleanProperty> property) {
        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setStyle(STYLE_TRANSPARENT);
        
        headerCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (S item : tableView.getItems()) {
                BooleanProperty prop = property.apply(item);
                if (prop != null) {
                    prop.set(newVal);
                }
            }
        });
        
        tableView.itemsProperty().addListener((obs, oldList, newList) -> {
            updateHeaderCheckBoxState(tableView, headerCheckBox, property);
        });
        
        tableView.getItems().addListener((ListChangeListener<S>) c -> {
            updateHeaderCheckBoxState(tableView, headerCheckBox, property);
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    updateHeaderCheckBoxState(tableView, headerCheckBox, property);
                    break;
                }
            }
        });
        
        return headerCheckBox;
    }
    
    private static <S> void updateHeaderCheckBoxState(
            TableView<S> tableView, 
            CheckBox headerCheckBox, 
            Function<S, BooleanProperty> property
    ) {
        int totalCount = tableView.getItems().size();
        if (totalCount == 0) {
            headerCheckBox.setSelected(false);
            headerCheckBox.setIndeterminate(false);
            return;
        }
        
        int selectedCount = 0;
        for (S item : tableView.getItems()) {
            BooleanProperty prop = property.apply(item);
            if (prop != null && prop.get()) {
                selectedCount++;
            }
        }
        
        if (selectedCount == totalCount) {
            headerCheckBox.setSelected(true);
            headerCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            headerCheckBox.setSelected(false);
            headerCheckBox.setIndeterminate(false);
        } else {
            headerCheckBox.setIndeterminate(true);
        }
    }

    // ========== Inner Cells ==========
    
    private static class IntegerEditingCell<S> extends TextFieldTableCell<S, Integer> {
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
    
    private static class CurrencyCell<S> extends TableCell<S, Double> {
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
    
    private static class DateTimeCell<S> extends TableCell<S, LocalDateTime> {
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
    
    private static class FormattedDateTimeCell<S> extends TableCell<S, LocalDateTime> {
        private final DateTimeFormatter formatter;
        
        public FormattedDateTimeCell(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }
        
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.format(formatter));
            }
        }
    }

    private static class BigDecimalCurrencyCell<S> extends TableCell<S, BigDecimal> {
        private final NumberFormat currencyFormat;
        
        public BigDecimalCurrencyCell() {
            this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setMinimumFractionDigits(2);
            currencyFormat.setMaximumFractionDigits(2);
        }
        
        @Override
        protected void updateItem(BigDecimal price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
            } else if (price.compareTo(BigDecimal.ZERO) == 0) {
                setText("-"); 
            } else {
                setText(currencyFormat.format(price));
            }
        }
    }

    private static class LabelCell<S> extends TableCell<S, Void> {
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
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(label);
                setAlignment(Pos.CENTER);
            }
        }
    }

    private static class DatePickerTableCell<S, T> extends TableCell<S, T> {
        public DatePickerTableCell(BiConsumer<S, T> setter) {}
    }

    // ========== Private Constructors & Helpers ==========
    
    private TableUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

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
        URL url = TableUtil.class.getResource(iconPath);
        if (url == null) return null;
        
        ImageView imageView = new ImageView(new Image(url.toExternalForm()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(22);
        imageView.setFitHeight(22);
        return imageView;
    }
    
    private static <S> void setupStaticColumnProps(TableColumn<S, Void> column, String title, String iconPath, Integer width) {
        if (title != null) column.setText(title);
        if (width != null) column.setPrefWidth(width);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(true);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
    }
}