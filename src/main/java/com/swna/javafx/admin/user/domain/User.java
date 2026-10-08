package com.swna.javafx.admin.user.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX TableView 및 폼 바인딩을 위한 User Domain 클래스
 */
public class User {

    private Long id;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    
    // 별도로 분리된 Role Enum을 ObjectProperty로 관리
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>(Role.USER); 
    
    // Address Embeddable 필드 평탄화 (Flat)[cite: 1]
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty street = new SimpleStringProperty();
    private final StringProperty zipcode = new SimpleStringProperty();
    
    // ContactInfo Embeddable 필드 평탄화 (Flat)[cite: 2]
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty mobile = new SimpleStringProperty();

    // ===== DTO 변환 메서드 =====
    public static User from(UserRecordDto dto) {
        User domain = new User();
        domain.setId(dto.id());
        domain.setEmail(dto.email() != null ? dto.email() : "");
        domain.setName(dto.name() != null ? dto.name() : "");
        
        // DTO의 role이 Enum인 경우 바로 할당 (String인 경우 Role.valueOf() 처리 필요)
        if (dto.role() != null) {
            domain.setRole(dto.role());
        }
        
        domain.setCity(dto.city() != null ? dto.city() : "");
        domain.setStreet(dto.street() != null ? dto.street() : "");
        domain.setZipcode(dto.zipcode() != null ? dto.zipcode() : "");
        domain.setPhone(dto.phone() != null ? dto.phone() : "");
        domain.setMobile(dto.mobile() != null ? dto.mobile() : "");
        
        return domain;
    }

    // ===== Getter / Setter / Property 메서드 =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean value) { selected.set(value); }
    public BooleanProperty selectedProperty() { return selected; }

    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // Role Enum에 대한 Getter, Setter, Property
    public Role getRole() { return role.get(); }
    public void setRole(Role value) { role.set(value); }
    public ObjectProperty<Role> roleProperty() { return role; }

    public String getCity() { return city.get(); }
    public void setCity(String value) { city.set(value); }
    public StringProperty cityProperty() { return city; }

    public String getStreet() { return street.get(); }
    public void setStreet(String value) { street.set(value); }
    public StringProperty streetProperty() { return street; }

    public String getZipcode() { return zipcode.get(); }
    public void setZipcode(String value) { zipcode.set(value); }
    public StringProperty zipcodeProperty() { return zipcode; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String value) { phone.set(value); }
    public StringProperty phoneProperty() { return phone; }

    public String getMobile() { return mobile.get(); }
    public void setMobile(String value) { mobile.set(value); }
    public StringProperty mobileProperty() { return mobile; }

    // ===== 편의 메서드 =====
    
    public String getFullAddress() {
        if (getCity().isEmpty() && getStreet().isEmpty()) {
            return "";
        }
        return String.format("[%s] %s, %s", getZipcode(), getCity(), getStreet());
    }

    public String getContactSummary() {
        String primaryContact = !getMobile().isEmpty() ? getMobile() : getPhone();
        return primaryContact != null ? primaryContact : "";
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", getName(), getEmail(), getRole());
    }
}