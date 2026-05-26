package com.sep.vox.domain.model.registerform;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

public class RegisterForm {
    private UUID id;
    private FullName contactFullName;
    private IdentityNumber identityNumber;
    private Phone contactPhone;
    private Email contactEmail;
    private DateOfBirth dateOfBirth;
    private String contactAddress;
    private SchoolDomain schoolDomain;
    private String schoolName;
    private String schoolAddress;
    private PostalCode postalCode;
    private String position;
    private StudentCount studentCount;
    private String reason;
    private RegisterFormStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;

    public RegisterForm() {
    }

    public RegisterForm(UUID id, FullName contactFullName, IdentityNumber identityNumber, Phone contactPhone,
            Email contactEmail, DateOfBirth dateOfBirth, String contactAddress, SchoolDomain schoolDomain, String schoolName,
            String schoolAddress, PostalCode postalCode,
            String position, StudentCount studentCount, String reason, RegisterFormStatus status, OffsetDateTime createdAt,
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

    public RegisterForm(FullName contactFullName, IdentityNumber identityNumber, Phone contactPhone, Email contactEmail, DateOfBirth dateOfBirth,
            String contactAddress,
            SchoolDomain schoolDomain, String schoolName, String schoolAddress, PostalCode postalCode, String position,
            StudentCount studentCount, String reason, RegisterFormStatus status, OffsetDateTime createdAt,
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

    public SchoolDomain getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(SchoolDomain schoolDomain) {
        this.schoolDomain = schoolDomain;
    }

    public StudentCount getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(StudentCount studentCount) {
        this.studentCount = studentCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FullName getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(FullName contactFullName) {
        this.contactFullName = contactFullName;
    }

    public IdentityNumber getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(IdentityNumber identityNumber) {
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

    public PostalCode getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(PostalCode postalCode) {
        this.postalCode = postalCode;
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

    public Phone getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(Phone contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Email getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(Email contactEmail) {
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

    public DateOfBirth getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirth dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void approve(UUID updatedBy) {
        if (status != RegisterFormStatus.PENDING) {
            throw new IllegalArgumentException("Đơn đăng ký đang không trạng thái chờ, không thể phê duyệt");
        }
        this.status = RegisterFormStatus.APPROVED;
        this.updatedBy = updatedBy;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject(UUID updatedBy, String reason) {
        if (status != RegisterFormStatus.PENDING) {
            throw new IllegalArgumentException("Đơn đăng ký đang không trong trạng thái chờ, không thể từ chối");
        }
        this.status = RegisterFormStatus.REJECTED;
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = OffsetDateTime.now();
    }

}
