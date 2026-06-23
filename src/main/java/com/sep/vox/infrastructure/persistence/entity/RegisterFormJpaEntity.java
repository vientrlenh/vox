package com.sep.vox.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
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
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID default uuidv7()"
    )
    private UUID id;

    @Column(name = "school_directory_id", updatable = false)
    private UUID schoolDirectoryId;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    @Column(name = "school_domain", updatable = false, length = 100)
    private String schoolDomain;

    @Column(name = "school_district", length = 255)
    private String schoolDistrict;

    @Column(name = "school_province", length = 255)
    private String schoolProvince; 

    @Column(name = "school_address",  length = 512)
    private String schoolAddress;

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

    @Column(name = "postal_code", nullable = false, updatable = false, length = 10)
    private String postalCode;

    @Column(name = "position", nullable = false, updatable = false, length = 50)
    private String position;

    @Column(name = "student_count", nullable = false, updatable = false)
    private int studentCount;

    @Column(name = "verification_method", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_register_form_verification_method_valid", 
            constraint = "verification_method IN ('DOMAIN_OTP', 'DOCUMENT')"
        )
    })
    private String verificationMethod;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "rejected_reason", length = 255)
    private String rejectedReason;

    @Column(name = "status", length = 20, nullable = false, check = {
        @CheckConstraint(
            name = "chk_register_forms_status_valid", 
            constraint = "status IN ('PENDING', 'AUTO_APPROVED', 'APPROVED', 'REJECTED')"
        )
    })
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    protected RegisterFormJpaEntity() {
    }

    public RegisterFormJpaEntity(UUID id, UUID schoolDirectoryId, String schoolName, String schoolDomain, 
            String schoolDistrict, String schoolProvince, String schoolAddress, String contactFullName,
            String identityNumber, String contactPhone, String contactEmail, LocalDate dateOfBirth,
            String contactAddress, String postalCode, String position, int studentCount, String verificationMethod,
            OffsetDateTime verifiedAt, String rejectedReason, String status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID reviewedBy) {
        this.id = id;
        this.schoolDirectoryId = schoolDirectoryId;
        this.schoolName = schoolName;
        this.schoolDomain = schoolDomain;
        this.schoolDistrict = schoolDistrict;
        this.schoolProvince = schoolProvince;
        this.schoolAddress = schoolAddress;
        this.contactFullName = contactFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.dateOfBirth = dateOfBirth;
        this.contactAddress = contactAddress;
        this.postalCode = postalCode;
        this.position = position;
        this.studentCount = studentCount;
        this.verificationMethod = verificationMethod;
        this.verifiedAt = verifiedAt;
        this.rejectedReason = rejectedReason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reviewedBy = reviewedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolDirectoryId() {
        return schoolDirectoryId;
    }

    public void setSchoolDirectoryId(UUID schoolDirectoryId) {
        this.schoolDirectoryId = schoolDirectoryId;
    }

    public String getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(String schoolDomain) {
        this.schoolDomain = schoolDomain;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getSchoolDistrict() {
        return schoolDistrict;
    }

    public void setSchoolDistrict(String schoolDistrict) {
        this.schoolDistrict = schoolDistrict;
    }

    public String getSchoolProvince() {
        return schoolProvince;
    }

    public void setSchoolProvince(String schoolProvince) {
        this.schoolProvince = schoolProvince;
    }

    public String getSchoolAddress() {
        return schoolAddress;
    }

    public void setSchoolAddress(String schoolAddress) {
        this.schoolAddress = schoolAddress;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
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

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
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

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    

    
}
