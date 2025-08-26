// services/order-service/src/main/java/com/swiftlogistics/order/entity/Client.java
package com.swiftlogistics.order.entity;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")  // Fixed: was 'n' instead of 'name'
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String clientId;

    @NotBlank(message = "Company name is required")
    private String companyName;  // Fixed: was 'companyN'

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @Email(message = "Valid email is required")
    private String email;

    private String phoneNumber;
    private String address;
    private boolean active;
    private LocalDateTime createdAt;

    // Constructors
    public Client() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public Client(String companyName, String contactPerson, String email) {
        this();
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.clientId = "CLIENT-" + System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getCompanyName() { return companyName; }  // Fixed method name
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}