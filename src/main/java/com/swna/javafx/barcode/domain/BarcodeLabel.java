package com.swna.javafx.barcode.domain;

import java.math.BigDecimal;

import com.swna.javafx.barcode.dto.BarcodeLabelDto;

import javafx.beans.property.*;

public class BarcodeLabel {
    private final LongProperty id = new SimpleLongProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty company = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
    
    public BarcodeLabel() {}
    
    public BarcodeLabel(Long id, String barcode, String company, String code, String description, BigDecimal price) {
        setId(id);
        setBarcode(barcode);
        setCompany(company);
        setCode(code);
        setDescription(description);
        setPrice(price);
    }
    
    // Property Getters (primary for TableView)
    public LongProperty idProperty() { return id; }
    public BooleanProperty selectedProperty() { return selected; }
    public StringProperty barcodeProperty() { return barcode; }
    public StringProperty companyProperty() { return company; }
    public StringProperty codeProperty() { return code; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }
    
    // Getters
    public Long getId() { return id.get(); }
    public boolean isSelected() { return selected.get(); }
    public String getBarcode() { return barcode.get(); }
    public String getCompany() { return company.get(); }
    public String getCode() { return code.get(); }
    public String getDescription() { return description.get(); }
    public BigDecimal getPrice() { return price.get(); }
    
    // Setters
    public void setId(Long id) { this.id.set(id); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    public void setCompany(String company) { this.company.set(company); }
    public void setCode(String code) { this.code.set(code); }
    public void setDescription(String description) { this.description.set(description); }
    public void setPrice(BigDecimal price) { this.price.set(price); }
    
    /**
     * DTO로부터 도메인 객체로 변환합니다.
     */
    public static BarcodeLabel fromDto(BarcodeLabelDto dto) {
        if (dto == null) return null;
        return new BarcodeLabel(dto.id(), dto.barcode(), dto.company(), 
                                dto.code(), dto.description(), dto.price());
    }
    
    /**
     * 도메인 객체를 DTO로 변환합니다.
     */
    public BarcodeLabelDto toDto() {
        return new BarcodeLabelDto(getId(), getBarcode(), getCompany(), 
                                   getCode(), getDescription(), getPrice());
    }
}
