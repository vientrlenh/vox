package com.sep.vox.domain.model.registerform;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
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
    private UUID schoolDirectoryId;
    private String schoolName;
    private SchoolDomain schoolDomain;
    private String schoolDistrict;
    private String schoolProvince;
    private String schoolAddress;
    private FullName contactFullName;
    private IdentityNumber identityNumber;
    private Phone contactPhone;
    private Email contactEmail;
    private DateOfBirth dateOfBirth;
    private String contactAddress;
    private PostalCode postalCode;
    private String position;
    private StudentCount studentCount;
    private RegisterFormVerificationMethod verificationMethod;
    private Instant verifiedAt;
    private String rejectedReason;
    private RegisterFormStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID reviewedBy;

    public RegisterForm() {
    }

    public RegisterForm(UUID id, UUID schoolDirectoryId, String schoolName, SchoolDomain schoolDomain,
            String schoolDistrict, String schoolProvince, String schoolAddress, FullName contactFullName,
            IdentityNumber identityNumber, Phone contactPhone, Email contactEmail, DateOfBirth dateOfBirth,
            String contactAddress, PostalCode postalCode, String position, StudentCount studentCount,
            RegisterFormVerificationMethod verificationMethod, Instant verifiedAt, String rejectedReason,
            RegisterFormStatus status, Instant createdAt, Instant updatedAt, UUID reviewedBy) {
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

    public RegisterForm(UUID schoolDirectoryId, String schoolName, SchoolDomain schoolDomain,
            String schoolDistrict, String schoolProvince, String schoolAddress, FullName contactFullName,
            IdentityNumber identityNumber, Phone contactPhone, Email contactEmail, DateOfBirth dateOfBirth,
            String contactAddress, PostalCode postalCode, String position, StudentCount studentCount,
            RegisterFormVerificationMethod verificationMethod, Instant verifiedAt, String rejectedReason,
            RegisterFormStatus status, Instant createdAt, Instant updatedAt, UUID reviewedBy) {
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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public SchoolDomain getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(SchoolDomain schoolDomain) {
        this.schoolDomain = schoolDomain;
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

    public DateOfBirth getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirth dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
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

    public StudentCount getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(StudentCount studentCount) {
        this.studentCount = studentCount;
    }

    public RegisterFormVerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(RegisterFormVerificationMethod verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public RegisterFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegisterFormStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }


    public static RegisterForm fromDirectoryWithDocuments(
        UUID schoolDirectoryId, 
        RegisterFormVerificationMethod verificationMethod, 
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate dateOfBirth, 
        String contactAddress, 
        String postalCode, 
        String position, 
        int studentCount, 
        Instant now
    ) {
        var form = new RegisterForm();
        form.schoolDirectoryId = require(schoolDirectoryId);
        form.verificationMethod = verificationMethod;
        form.contactFullName = new FullName(contactFullName);
        form.identityNumber = new IdentityNumber(identityNumber);
        form.contactEmail = new Email(contactEmail);
        form.contactPhone = new Phone(contactPhone);
        form.dateOfBirth = new DateOfBirth(dateOfBirth);
        form.contactAddress = contactAddress;
        form.postalCode = new PostalCode(postalCode);
        form.position = position;
        form.studentCount = new StudentCount(studentCount);
        form.status = RegisterFormStatus.PENDING;
        form.createdAt = now;
        form.updatedAt = now;
        return form;
    }

    public static RegisterForm fromDirectoryWithVerifiedOtp(
        UUID schoolDirectoryId, 
        RegisterFormVerificationMethod verificationMethod, 
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate dateOfBirth, 
        String contactAddress, 
        String postalCode, 
        String position, 
        int studentCount, 
        Instant now
    ) {
        var form = new RegisterForm();
        form.schoolDirectoryId = require(schoolDirectoryId);
        form.verificationMethod = verificationMethod;
        form.contactFullName = new FullName(contactFullName);
        form.identityNumber = new IdentityNumber(identityNumber);
        form.contactEmail = new Email(contactEmail);
        form.contactPhone = new Phone(contactPhone);
        form.dateOfBirth = new DateOfBirth(dateOfBirth);
        form.contactAddress = contactAddress;
        form.postalCode = new PostalCode(postalCode);
        form.position = position;
        form.studentCount = new StudentCount(studentCount);
        form.status = RegisterFormStatus.AUTO_APPROVED;
        form.createdAt = now;
        form.updatedAt = now;
        return form;
    }

    public static RegisterForm selfDeclared(
        String schoolName, 
        String schoolDomain, 
        String schoolAddress, 
        String schoolProvince, 
        String schoolDistrict, 
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate dateOfBirth, 
        String contactAddress, 
        String postalCode, 
        String position, 
        int studentCount, 
        Instant now
    ) {
        var form = new RegisterForm();
        form.verificationMethod = RegisterFormVerificationMethod.DOCUMENT;
        form.schoolName = require(schoolName);
        form.schoolDomain = new SchoolDomain(schoolDomain);
        form.schoolAddress = require(schoolAddress);
        form.schoolProvince = require(schoolProvince);
        form.schoolDistrict = require(schoolDistrict);
        form.contactFullName = new FullName(contactFullName);
        form.identityNumber = new IdentityNumber(identityNumber);
        form.contactEmail = new Email(contactEmail);
        form.contactPhone = new Phone(contactPhone);
        form.dateOfBirth = new DateOfBirth(dateOfBirth);
        form.contactAddress = contactAddress;
        form.postalCode = new PostalCode(postalCode);
        form.position = position;
        form.studentCount = new StudentCount(studentCount);
        form.status = RegisterFormStatus.PENDING;
        form.createdAt = now;
        form.updatedAt = now;
        return form;
    }

    private static <T> T require(T data) {
        return Objects.requireNonNull(data);
    }
}
