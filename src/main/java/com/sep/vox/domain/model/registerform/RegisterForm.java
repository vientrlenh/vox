package com.sep.vox.domain.model.registerform;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RegisterForm {
    private UUID id;
    private String contactFullName;
    private String identityNumber;
    private String contactPhone;
    private String contactEmail;
    private LocalDate dateOfBirth;
    private String contactAddress;
    private String schoolDomain;
    private String schoolName;
    private String schoolAddress;
    private String postalCode;
    private String position;
    private int studentCount;
    private String reason;
    private RegisterFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;

    public RegisterForm() {
    }

    public RegisterForm(UUID id, String contactFullName, String identityNumber, String contactPhone,
            String contactEmail, LocalDate dateOfBirth, String contactAddress, String schoolDomain, String schoolName,
            String schoolAddress, String postalCode,
            String position, int studentCount, String reason, RegisterFormStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID updatedBy) {
        this.id = id;
        this.contactFullName = contactFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.dateOfBirth = dateOfBirth;
        this.contactAddress = contactAddress;
        this.schoolDomain = schoolDomain;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.postalCode = postalCode;
        this.position = position;
        this.studentCount = studentCount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public RegisterForm(String contactFullName, String identityNumber, String contactPhone, String contactEmail, LocalDate dateOfBirth,
            String contactAddress,
            String schoolDomain, String schoolName, String schoolAddress, String postalCode, String position,
            int studentCount, String reason, RegisterFormStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID updatedBy) {
        this.contactFullName = contactFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.dateOfBirth = dateOfBirth;
        this.contactAddress = contactAddress;
        this.schoolDomain = schoolDomain;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.postalCode = postalCode;
        this.position = position;
        this.studentCount = studentCount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(String contactFullName) {
        this.contactFullName = contactFullName;
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

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String taxCode) {
        this.postalCode = taxCode;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

}
