// services/driver-service/src/main/java/com/swiftlogistics/driver/dto/DriverLoginRequest.java
package com.swiftlogistics.driver.dto;

import javax.validation.constraints.NotBlank;

public class DriverLoginRequest {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Constructors
    public DriverLoginRequest() {}

    public DriverLoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
