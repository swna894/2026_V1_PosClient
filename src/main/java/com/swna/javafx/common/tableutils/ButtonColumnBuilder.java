package com.swna.javafx.common.tableutils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class ButtonColumnBuilder<S> {

    // 아이콘 Image 캐시
    private static final Map<String, Image> ICON_CACHE = new ConcurrentHashMap<>();

    // ===== [버튼 스타일 기본/호버 값 정의] =====
    private static final String BUTTON_STYLE_HOVER = "-fx-background-color: #6F4CBB; -fx-text-fill: white; -fx-alignment: center;";

    // 필수 매개변수
    private final TableView<S> tableView;

    // 선택적 매개변수 (기본값 설정)
    private String headerIconPath = null; // 헤더 전용 아이콘 경로
    private String title = "";
    private boolean isVisible = true;
    private String iconPath = null;
    
    // 텍스트 색상 및 배경색 필드
    private String textColor = null;
    private String bgColor = null;

    private Function<S, String> iconPathFunction = null;

    // 너비 및 고정/가변 관련 필드
    private int width = -1;
    private int minWidth = -1;
    private int maxWidth = -1;
    private boolean resizable = true;

    // 클릭 콜백
    private BiConsumer<S, ActionEvent> action = null;

    public ButtonColumnBuilder(TableView<S> tableView) {
        this.tableView = tableView;
    }

    public ButtonColumnBuilder<S> title(String title) {
        this.title = title;
        return this;
    }

    public ButtonColumnBuilder<S> visible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    /** 고정 아이콘 경로 설정 */
    public ButtonColumnBuilder<S> iconPath(String iconPath) {
        this.iconPath = iconPath;
        this.iconPathFunction = null;
        return this;
    }

    /** 텍스트 색상 설정 빌더 메서드 */
    public ButtonColumnBuilder<S> textColor(String color) {
        this.textColor = color;
        return this;
    }

    /** 배경색 설정 빌더 메서드 */
    public ButtonColumnBuilder<S> bgColor(String color) {
        this.bgColor = color;
        return this;
    }

    /** 동적 아이콘 경로 설정 (row 객체 기반) */
    public ButtonColumnBuilder<S> iconPath(Function<S, String> iconPathFunction) {
        this.iconPathFunction = iconPathFunction;
        this.iconPath = null;
        return this;
    }

    /** 헤더 아이콘 경로 직접 설정 */
    public ButtonColumnBuilder<S> headerIconPath(String headerIconPath) {
        this.headerIconPath = headerIconPath;
        return this;
    }

    /** 헤더 아이콘과 동적 셀 아이콘을 한 번에 설정하는 오버로딩 메서드 */
    public ButtonColumnBuilder<S> iconPath(String headerIconPath, Function<S, String> iconPathFunction) {
        this.headerIconPath = headerIconPath;
        this.iconPathFunction = iconPathFunction;
        this.iconPath = null;
        return this;
    }

    public ButtonColumnBuilder<S> width(int width) {
        this.width = width;
        return this;
    }

    public ButtonColumnBuilder<S> minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public ButtonColumnBuilder<S> maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public ButtonColumnBuilder<S> resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public ButtonColumnBuilder<S> fixedWidth(int width) {
        this.width = width;
        this.minWidth = width;
        this.maxWidth = width;
        this.resizable = false;
        return this;
    }

    public ButtonColumnBuilder<S> action(Consumer<S> action) {
        this.action = (item, event) -> action.accept(item);
        return this;
    }

    public ButtonColumnBuilder<S> action(BiConsumer<S, ActionEvent> action) {
        this.action = action;
        return this;
    }

    public TableColumn<S, Void> build() {
        TableColumn<S, Void> column = new TableColumn<>(title);
        column.setVisible(isVisible);
        column.setSortable(false);
        column.setEditable(false);

        // 1. title이 있으면 텍스트 설정
        if (title != null && !title.isBlank()) {
            column.setText(title);
        } 
        // 2. headerIconPath 또는 정적 iconPath가 있으면 헤더 아이콘으로 적용
        else {
            String targetHeaderIcon = (headerIconPath != null && !headerIconPath.isBlank()) 
                    ? headerIconPath 
                    : iconPath;

            if (targetHeaderIcon != null && !targetHeaderIcon.isBlank()) {
                ImageView headerImageView = new ImageView(loadIcon(targetHeaderIcon));
                headerImageView.setFitWidth(16);
                headerImageView.setFitHeight(16);
                headerImageView.setPreserveRatio(true);
                column.setGraphic(headerImageView);
            }
        }

        // TableUtil을 거치지 않고 통합 CellFactory를 직접 생성
        column.setCellFactory(createUnifiedButtonCellFactory());

        applyColumnWidthProperties(column);

        if (tableView != null) {
            tableView.getColumns().add(column);
        }
        return column;
    }

    private void applyColumnWidthProperties(TableColumn<S, Void> column) {
        if (column == null) return;

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
    }

    private Callback<TableColumn<S, Void>, TableCell<S, Void>> createUnifiedButtonCellFactory() {
        return col -> new UnifiedButtonTableCell();
    }

    /**
     * 정적 아이콘/텍스트 버튼과 동적 아이콘 버튼을 모두 처리하는 통합 Cell
     */
    private class UnifiedButtonTableCell extends TableCell<S, Void> {
        private final ImageView imageView = new ImageView();
        private final Button button = new Button();

        public UnifiedButtonTableCell() {
            initImageView();
            initButton();
        }

        private void initImageView() {
            imageView.setFitWidth(16);
            imageView.setFitHeight(16);
            imageView.setPreserveRatio(true);
        }

        private void initButton() {
            button.setAlignment(Pos.CENTER);
            button.setMinWidth(0);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMaxHeight(Double.MAX_VALUE);
            button.setMinHeight(34);
            button.setPrefHeight(34);
            button.setPadding(Insets.EMPTY);

            // 지정된 배경색과 글자색이 반영된 기본 스타일 적용
            applyDefaultStyle();

            button.setOnMouseEntered(e -> {
                if (getTableView() != null) {
                    getTableView().getSelectionModel().select(getIndex());
                }
                button.setStyle(BUTTON_STYLE_HOVER);
            });

            // 마우스 이탈 시 기본 스타일로 복구
            button.setOnMouseExited(e -> applyDefaultStyle());
            button.setOnAction(this::handleAction);
        }

        /**
         * bgColor / textColor 설정 유무에 따른 기본 스타일 적용 메서드
         */
        private void applyDefaultStyle() {
            StringBuilder style = new StringBuilder();

            // 1. 배경색 설정 (지정된 게 없으면 transparent)
            if (bgColor != null && !bgColor.isBlank()) {
                style.append("-fx-background-color: ").append(bgColor).append(";");
            } else {
                style.append("-fx-background-color: transparent;");
            }

            // 2. 글자색 설정 (지정된 게 없으면 기본 검은색 톤)
            if (textColor != null && !textColor.isBlank()) {
                style.append("-fx-text-fill: ").append(textColor).append(";");
            } else {
                style.append("-fx-text-fill: #1a1a1a;");
            }

            style.append("-fx-alignment: center;");
            button.setStyle(style.toString());
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            if (getGraphic() == button) {
                var insets = getInsets();
                double w = Math.max(0, getWidth() - insets.getLeft() - insets.getRight());
                double h = Math.max(0, getHeight() - insets.getTop() - insets.getBottom());
                button.resizeRelocate(insets.getLeft(), insets.getTop(), w, h);
            }
        }

        private void handleAction(ActionEvent event) {
            S rowItem = getCurrentRowItem();
            if (action != null && rowItem != null) {
                action.accept(rowItem, event);
            }
        }

        private S getCurrentRowItem() {
            int idx = getIndex();
            var tv = getTableView();
            if (tv == null || idx < 0 || idx >= tv.getItems().size()) {
                return null;
            }
            return tv.getItems().get(idx);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);

            S rowItem = getCurrentRowItem();
            if (empty || rowItem == null) {
                setGraphic(null);
                return;
            }

            // 아이콘 경로 결정 (동적 우선, 없으면 정적)
            String currentIconPath = null;
            if (iconPathFunction != null) {
                currentIconPath = iconPathFunction.apply(rowItem);
            } else if (iconPath != null) {
                currentIconPath = iconPath;
            }

            // 이미지 세팅
            if (currentIconPath != null) {
                imageView.setImage(loadIcon(currentIconPath));
                button.setGraphic(imageView);
            } else {
                button.setGraphic(null);
            }

            // 텍스트 세팅
            if (title != null && !title.isBlank()) {
                button.setText(title);
            }

            // Display 형태 설정
            if (button.getGraphic() != null && button.getText() != null && !button.getText().isBlank()) {
                button.setContentDisplay(ContentDisplay.LEFT);
            } else if (button.getGraphic() != null) {
                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                button.setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            setGraphic(button);
            setAlignment(Pos.CENTER);
        }
    }

    private static Image loadIcon(String path) {
        return ICON_CACHE.computeIfAbsent(path, Image::new);
    }
}