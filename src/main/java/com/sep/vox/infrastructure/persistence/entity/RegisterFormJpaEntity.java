package com.sep.vox.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "register_forms", indexes = {
        @Index(columnList = "identity_number", name = "idx_register_identity"),
        @Index(columnList = "contact_phone", name = "idx_register_phone"),
        @Index(columnList = "contact_email", name = "idx_register_email")
})
public class RegisterFormJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID default uuidv7()")
    private UUID id;

    @Column(name = "contact_full_name", nullable = false, updatable = false, length = 255)
    private String contactFullName;

    @Column(name = "identity_number", nullable = false, updatable = false, length = 20)
    private String identityNumber;

    @Column(name = "contact_phone", nullable = false, updatable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, updatable = false, length = 255)
    private String contactEmail;

    @Column(name = "date_of_birth", nullable = false, updatable = false)
    private LocalDate dateOfBirth;

    @Column(name = "contact_address", nullable = false, updatable = false, length = 512)
    private String contactAddress;

    @Column(name = "school_domain", nullable = false, updatable = false, length = 100)
    private String schoolDomain;

    @Column(name = "school_name", nullable = false, updatable = false, length = 255)
    private String schoolName;

    @Column(name = "school_address", nullable = false, updatable = false, length = 512)
    private String schoolAddress;

    @Column(name = "postal_code", nullable = false, updatable = false, length = 10)
    private String postalCode;

    @Column(name = "position", nullable = false, updatable = false, length = 50)
    private String position;

    @Column(name = "student_count", nullable = false, updatable = false)
    private int studentCount;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected RegisterFormJpaEntity() {
    }

    public RegisterFormJpaEntity(UUID id, String contactFullName, String identityNumber, String contactPhone,
            String contactEmail, LocalDate dateOfBirth, String contactAddress, String schoolDomain, String schoolName, String schoolAddress,
            String postalCode, String position,
            int studentCount, String reason, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.contactFullName = contactFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.dateOfBirth = dateOfBirth;
        this.contactAddress = contactAddress;
        this.schoolDomain = schoolDomain;
        this.schoolAddress = schoolAddress;
        this.schoolName = schoolName;
        this.postalCode = postalCode;
        this.position = position;
        this.studentCount = studentCount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public RegisterFormJpaEntity(String contactFullName, String identityNumber, String contactPhone,
            String contactEmail, LocalDate dateOfBirth, String contactAddress, String schoolDomain, String schoolName, String schoolAddress,
            String postalCode, String position,
            int studentCount, String reason, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID updatedBy) {
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

    public String getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(String schoolDomain) {
        this.schoolDomain = schoolDomain;
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

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
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
