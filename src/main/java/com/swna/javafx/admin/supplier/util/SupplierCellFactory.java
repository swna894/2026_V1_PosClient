package com.swna.javafx.admin.supplier.util;


import com.swna.javafx.admin.supplier.domain.Supplier;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * TableView Cell 렌더링 유틸리티
 */
public class SupplierCellFactory {
    
    /**
     * 상태 표시 Cell (Active/Inactive with circle indicator)
     */
    public static class StatusCell extends TableCell<Supplier, Boolean> {
        private final HBox content;
        private final Circle indicator;
        private final Label label;
        
        public StatusCell() {
            indicator = new Circle(6);
            label = new Label();
            content = new HBox(5, indicator, label);
            content.setAlignment(Pos.CENTER);
        }
        
        @Override
        protected void updateItem(Boolean active, boolean empty) {
            super.updateItem(active, empty);
            
            if (empty || active == null) {
                setGraphic(null);
                setText(null);
            } else {
                if (active) {
                    indicator.setFill(Color.GREEN);
                    label.setText("Active");
                    label.setStyle("-fx-text-fill: green;");
                } else {
                    indicator.setFill(Color.RED);
                    label.setText("Inactive");
                    label.setStyle("-fx-text-fill: red;");
                }
                setGraphic(content);
                setText(null);
            }
        }
    }
    
    /**
     * 전화번호 포맷 Cell
     */
    public static class PhoneCell extends TableCell<Supplier, String> {
        @Override
        protected void updateItem(String phone, boolean empty) {
            super.updateItem(phone, empty);
            
            if (empty || phone == null || phone.isBlank()) {
                setText("-");
            } else {
                // 전화번호 포맷팅 (예: 01012345678 -> 010-1234-5678)
                setText(formatPhoneNumber(phone));
            }
        }
        
        private String formatPhoneNumber(String phone) {
            if (phone == null) return "-";
            String cleaned = phone.replaceAll("[^0-9]", "");
            
            if (cleaned.length() == 10) {
                return cleaned.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
            } else if (cleaned.length() == 11) {
                return cleaned.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
            }
            return phone;
        }
    }
    
    /**
     * 날짜 포맷 Cell
     */
    public static class DateCell extends TableCell<Supplier, java.time.LocalDateTime> {
        private final java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        @Override
        protected void updateItem(java.time.LocalDateTime dateTime, boolean empty) {
            super.updateItem(dateTime, empty);
            
            if (empty || dateTime == null) {
                setText("-");
            } else {
                setText(dateTime.format(formatter));
            }
        }
    }
    
    /**
     * 툴팁이 있는 Cell
     */
    public static class TooltipCell extends TableCell<Supplier, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            
            if (empty || value == null || value.isBlank()) {
                setText("-");
                setTooltip(null);
            } else {
                setText(value.length() > 30 ? value.substring(0, 27) + "..." : value);
                Tooltip tooltip = new Tooltip(value);
                setTooltip(tooltip);
            }
        }
    }
}