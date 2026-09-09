package com.swna.javafx.admin.inventory.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.ToString;

@ToString
public class Inventory {

    private Long id;

    private BooleanProperty selected = new SimpleBooleanProperty(false);
    private StringProperty code = new SimpleStringProperty();
    private StringProperty barcode = new SimpleStringProperty();
    private StringProperty description = new SimpleStringProperty();
    private StringProperty comment = new SimpleStringProperty();
    private ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
    private ObjectProperty<BigDecimal> cost = new SimpleObjectProperty<>();
    private ObjectProperty<BigDecimal> priceOld = new SimpleObjectProperty<>();
    private ObjectProperty<BigDecimal> costOld = new SimpleObjectProperty<>();
    private IntegerProperty quantity = new SimpleIntegerProperty();
    private IntegerProperty minStock = new SimpleIntegerProperty();
    private IntegerProperty minOrderQuantity = new SimpleIntegerProperty();
    private ObjectProperty<LocalDateTime> lastOrderedAt = new SimpleObjectProperty<>();

    public Inventory() { super(); }

    public Inventory(String code) {
        this.code.set(code);
    }

    // ---------------- Property Getters/Setters ----------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }    

    public Boolean isSelected() { return selected.get(); }
    public void setSelected(Boolean selected) { this.selected.set(selected); }
    public BooleanProperty selectedProperty() { return selected; }

    public String getCode() { return code.get(); }
    public void setCode(String code) { this.code.set(code); }
    public StringProperty codeProperty() { return code; }

    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    public StringProperty barcodeProperty() { return barcode; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public String getComment() { return comment.get(); }
    public void setComment(String comment) { this.comment.set(comment); }
    public StringProperty commentProperty() { return comment; }

    public BigDecimal getPrice() { return price.get(); }
    public void setPrice(BigDecimal price) { this.price.set(price); }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    public BigDecimal getCost() { return cost.get(); }
    public void setCost(BigDecimal cost) { this.cost.set(cost); }
    public ObjectProperty<BigDecimal> costProperty() { return cost; }

    public BigDecimal getPriceOld() { return priceOld.get(); }
    public void setPriceOld(BigDecimal priceOld) { this.priceOld.set(priceOld); }
    public ObjectProperty<BigDecimal> priceOldProperty() { return priceOld; }

    public BigDecimal getCostOld() { return costOld.get(); }
    public void setCostOld(BigDecimal costOld) { this.costOld.set(costOld); }
    public ObjectProperty<BigDecimal> costOldProperty() { return costOld; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public IntegerProperty quantityProperty() { return quantity; }

    public int getMinStock() { return minStock.get(); }
    public void setMinStock(int minStock) { this.minStock.set(minStock); }
    public IntegerProperty minStockProperty() { return minStock; }

    public int getMinOrderQuantity() { return minOrderQuantity.get(); }
    public void setMinOrderQuantity(int minOrderQuantity) { this.minOrderQuantity.set(minOrderQuantity); }
    public IntegerProperty minOrderQuantityProperty() { return minOrderQuantity; }

    public LocalDateTime getLastOrderedAt() { return lastOrderedAt.get(); }
    public void setLastOrderedAt(LocalDateTime lastOrderedAt) { this.lastOrderedAt.set(lastOrderedAt); }
    public ObjectProperty<LocalDateTime> lastOrderedAtProperty() { return lastOrderedAt; }

    // ---------------- Utility Methods ----------------

    // ProductResponse DTO -> Inventory 변환
    public static Inventory from(InvoentoryResponse dto) {
        if (dto == null) return null;
        Inventory inventory = new Inventory();
        inventory.setId(dto.id());
        inventory.setCode(dto.code());
        inventory.setBarcode(dto.barcode());
        inventory.setDescription(dto.description());
        inventory.setPrice(dto.price());
        inventory.setCost(dto.cost());
        inventory.setComment(dto.comment());
        inventory.setPriceOld(dto.priceOld());
        inventory.setCostOld(dto.costOld());
        inventory.setQuantity(dto.quantity());
        inventory.setMinStock(dto.minStock());
        inventory.setMinOrderQuantity(dto.minOrderQuantity());
        inventory.setLastOrderedAt(dto.lastOrderedAt());
        return inventory;
    }

    /**
     * 🔥 현재 Inventory 객체 상태를 바탕으로 Update DTO 생성
     */
    public InventoryUpdateRequest toUpdateRequest() {
        return new InventoryUpdateRequest(
                getId(),
                getDescription(),
                getPrice(),
                getCost(),
                null, // category (필요 시 필드 추가 후 매핑)
                getQuantity(),
                getMinStock(),
                null, // maxStock
                getMinOrderQuantity()
        );
    }

    public String getLastOrderedAtFormatted() {
        if (getLastOrderedAt() == null) return "";
        return getLastOrderedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}