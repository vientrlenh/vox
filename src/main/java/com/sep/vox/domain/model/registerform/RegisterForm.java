package com.sep.vox.domain.model.registerform;

import java.time.OffsetDateTime;

import com.sep.vox.domain.valueobject.id.RegisterFormId;

public class RegisterForm {
    private RegisterFormId id;
    private String representativeFullName;
    private String identityNumber;
    private String phone;
    private String schoolName;
    private String schoolAddress;
    private String taxCode;
    private String position;
    private int contractDurationMonth;
    private RegisterFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Getter and setter
    public RegisterFormId getId() {
        return id;
    }
    public int getContractDurationMonth() {
        return contractDurationMonth;
    }
    public void setContractDurationMonth(int contractDurationMonth) {
        this.contractDurationMonth = contractDurationMonth;
    }
    public void setId(RegisterFormId id) {
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
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
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

    
}
