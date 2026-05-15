package com.sep.vox.domain.model.registerform;

import java.time.OffsetDateTime;
import java.util.UUID;


public class RegisterForm {
    private UUID id;
    private String representativeFullName;
    private String identityNumber;
    private String contactPhone;
    private String contactEmail;
    private String schoolName;
    private String schoolAddress;
    private String taxCode;
    private String position;
    private int contractDurationMonth;
    private RegisterFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime approvedAt;
    private UUID approvedBy;

    // Getter and setter
    public UUID getId() {
        return id;
    }

    public int getContractDurationMonth() {
        return contractDurationMonth;
    }

    public void setContractDurationMonth(int contractDurationMonth) {
        this.contractDurationMonth = contractDurationMonth;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRepresentativeFullName() {
        return representativeFullName;
    }

    public void setRepresentativeFullName(String representativeFullName) {
        this.representativeFullName = representativeFullName;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getSchoolAddress() {
        return schoolAddress;
    }

    public void setSchoolAddress(String schoolAddress) {
        this.schoolAddress = schoolAddress;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public RegisterFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegisterFormStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    
}
