package com.sep.vox.domain.model.registerform;

import java.time.OffsetDateTime;
import java.util.UUID;


public class RegisterForm {
    private UUID id;
    private String representativeFullName;
    private String identityNumber;
    private String contactPhone;
    private String contactEmail;
    private String schoolDomain;
    private String schoolName;
    private String schoolAddress;
    private String taxCode;
    private String position;
    private int contractDurationMonth;
    private int studentCount;
    private RegisterFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime approvedAt;
    private UUID approvedBy;

    public RegisterForm() {}

    public RegisterForm(UUID id, String representativeFullName, String identityNumber, String contactPhone,
            String contactEmail, String schoolDomain, String schoolName, String schoolAddress, String taxCode,
            String position, int contractDurationMonth, int studentCount, RegisterFormStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime approvedAt, UUID approvedBy) {
        this.id = id;
        this.representativeFullName = representativeFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.schoolDomain = schoolDomain;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.taxCode = taxCode;
        this.position = position;
        this.contractDurationMonth = contractDurationMonth;
        this.studentCount = studentCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
    }

    public RegisterForm(String representativeFullName, String identityNumber, String contactPhone, String contactEmail,
            String schoolDomain, String schoolName, String schoolAddress, String taxCode, String position,
            int contractDurationMonth, int studentCount, RegisterFormStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, OffsetDateTime approvedAt, UUID approvedBy) {
        this.representativeFullName = representativeFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.schoolDomain = schoolDomain;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.taxCode = taxCode;
        this.position = position;
        this.contractDurationMonth = contractDurationMonth;
        this.studentCount = studentCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
    }

    public String getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(String schoolDomain) {
        this.schoolDomain = schoolDomain;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

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
