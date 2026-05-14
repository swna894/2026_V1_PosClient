package com.swna.javafx.admin.supplier.domain;

import javafx.beans.property.*;
import java.time.LocalDateTime;

import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;

/**
 * JavaFX TableView용 Supplier Domain 클래스
 * Property 기반으로 UI 바인딩 지원
 */
public class Supplier {
    
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty abbr = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty company = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty cellphone = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty();
    
    // ===== 생성자 =====
    
    // ===== DTO 변환 메서드 =====
    public static Supplier from(SupplierResponseRecord dto) {
        Supplier domain = new Supplier();
        domain.setId(dto.id());
        domain.setAbbr(dto.abbr());
        domain.setName(dto.name());
        domain.setCompany(dto.company() != null ? dto.company() : "");
        domain.setEmail(dto.email() != null ? dto.email() : "");
        domain.setPhone(dto.phone() != null ? dto.phone() : "");
        domain.setCellphone(dto.cellphone() != null ? dto.cellphone() : "");
        domain.setAddress(dto.address() != null ? dto.address() : "");
        domain.setActive(dto.active());
        return domain;
    }
    
    // ===== Getter / Setter / Property =====
    
    // id
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }
    
    // abbr
    public String getAbbr() { return abbr.get(); }
    public void setAbbr(String value) { abbr.set(value); }
    public StringProperty abbrProperty() { return abbr; }
    
    // name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }
    
    // company
    public String getCompany() { return company.get(); }
    public void setCompany(String value) { company.set(value); }
    public StringProperty companyProperty() { return company; }
    
    // email
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }
    
    // phone
    public String getPhone() { return phone.get(); }
    public void setPhone(String value) { phone.set(value); }
    public StringProperty phoneProperty() { return phone; }
    
    // cellphone
    public String getCellphone() { return cellphone.get(); }
    public void setCellphone(String value) { cellphone.set(value); }
    public StringProperty cellphoneProperty() { return cellphone; }
    
    // address
    public String getAddress() { return address.get(); }
    public void setAddress(String value) { address.set(value); }
    public StringProperty addressProperty() { return address; }
    
    // active
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }
    public BooleanProperty activeProperty() { return active; }
    
    
    // ===== 편의 메서드 =====
    public String getStatusText() {
        return isActive() ? "Active" : "Inactive";
    }
    
    public String getStatusStyle() {
        return isActive() ? "status-active" : "status-inactive";
    }
    
    public String getFullName() {
        return String.format("%s (%s)", getName(), getAbbr());
    }
    
    public String getContactInfo() {
        return String.format("%s / %s", getPhone(), getEmail());
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s", getAbbr(), getName());
    }
}