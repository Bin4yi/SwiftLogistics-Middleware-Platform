// services/driver-service/src/main/java/com/swiftlogistics/driver/dto/DeliveryUpdateRequest.java
package com.swiftlogistics.driver.dto;

import com.swiftlogistics.driver.enums.DeliveryStatus;
import javax.validation.constraints.NotNull;

public class DeliveryUpdateRequest {
    @NotNull(message = "Status is required")
    private DeliveryStatus status;

    private String notes;
    private String failureReason;
    private String proofOfDeliveryPhoto;
    private String digitalSignature;
    private Double latitude;
    private Double longitude;

    // Constructors
    public DeliveryUpdateRequest() {}

    // Getters and Setters
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getProofOfDeliveryPhoto() { return proofOfDeliveryPhoto; }
    public void setProofOfDeliveryPhoto(String proofOfDeliveryPhoto) { this.proofOfDeliveryPhoto = proofOfDeliveryPhoto; }

    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}