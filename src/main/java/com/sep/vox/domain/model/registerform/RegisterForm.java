package com.sep.vox.domain.model.registerform;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.BaseModel;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;

public class RegisterForm extends BaseModel {
    private UUID schoolDirectoryId;
    private Name schoolName;
    private SchoolDomain schoolDomain;
    private UUID schoolWardId;
    private UUID schoolCityId;
    private UUID schoolProvinceId;
    private String schoolStreetAddress;
    private Name contactFullName;
    private IdentityNumber identityNumber;
    private Phone contactPhone;
    private Email contactEmail;
    private BirthDate birthDate;
    private String contactAddress;
    private PostalCode postalCode;
    private PositiveInteger studentCount;
    private RegisterFormVerificationMethod verificationMethod;
    private Instant verifiedAt;
    private String rejectReason;
    private Instant rejectedAt;
    private RegisterFormStatus status;
    private UUID reviewedBy;

    public RegisterForm() {
    }

    public RegisterForm(UUID schoolDirectoryId, Name schoolName, SchoolDomain schoolDomain,
            UUID schoolWardId, UUID schoolCityId, UUID schoolProvinceId, String schoolStreetAddress, Name contactFullName,
            IdentityNumber identityNumber, Phone contactPhone, Email contactEmail, BirthDate birthDate,
            String contactAddress, PostalCode postalCode, PositiveInteger studentCount,
            RegisterFormVerificationMethod verificationMethod, Instant verifiedAt, String rejectReason, Instant rejectedAt, 
            RegisterFormStatus status, UUID reviewedBy) {
        this.schoolDirectoryId = schoolDirectoryId;
        this.schoolName = schoolName;
        this.schoolDomain = schoolDomain;
        this.schoolWardId = schoolWardId;
        this.schoolCityId = schoolCityId;
        this.schoolProvinceId = schoolProvinceId;
        this.schoolStreetAddress = schoolStreetAddress;
        this.contactFullName = contactFullName;
        this.identityNumber = identityNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.birthDate = birthDate;
        this.contactAddress = contactAddress;
        this.postalCode = postalCode;
        this.studentCount = studentCount;
        this.verificationMethod = verificationMethod;
        this.verifiedAt = verifiedAt;
        this.rejectReason = rejectReason;
        this.rejectedAt = rejectedAt;
        this.status = status;
        this.reviewedBy = reviewedBy;
    }

    public RegisterForm(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolDirectoryId = builder.schoolDirectoryId;
        this.schoolName = builder.schoolName;
        this.schoolDomain = builder.schoolDomain;
        this.schoolWardId = builder.schoolWardId;
        this.schoolCityId = builder.schoolCityId;
        this.schoolProvinceId = builder.schoolProvinceId;
        this.schoolStreetAddress = builder.schoolStreetAddress;
        this.contactFullName = builder.contactFullName;
        this.identityNumber = builder.identityNumber;
        this.contactPhone = builder.contactPhone;
        this.contactEmail = builder.contactEmail;
        this.birthDate = builder.birthDate;
        this.contactAddress = builder.contactAddress;
        this.postalCode = builder.postalCode;
        this.studentCount = builder.studentCount;
        this.verificationMethod = builder.verificationMethod;
        this.verifiedAt = builder.verifiedAt;
        this.rejectReason = builder.rejectReason;
        this.rejectedAt = builder.rejectedAt;
        this.status = builder.status;
        this.reviewedBy = builder.reviewedBy;
    }

    public UUID getSchoolDirectoryId() {
        return schoolDirectoryId;
    }

    public void setSchoolDirectoryId(UUID schoolDirectoryId) {
        this.schoolDirectoryId = schoolDirectoryId;
    }

    public Name getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(Name schoolName) {
        this.schoolName = schoolName;
    }

    public SchoolDomain getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(SchoolDomain schoolDomain) {
        this.schoolDomain = schoolDomain;
    }

    public UUID getSchoolWardId() {
        return schoolWardId;
    }

    public void setSchoolWardId(UUID schoolWardId) {
        this.schoolWardId = schoolWardId;
    }

    public UUID getSchoolCityId() {
        return schoolCityId;
    }

    public void setSchoolCityId(UUID schoolCityId) {
        this.schoolCityId = schoolCityId;
    }

    public UUID getSchoolProvinceId() {
        return schoolProvinceId;
    }

    public void setSchoolProvinceId(UUID schoolProvinceId) {
        this.schoolProvinceId = schoolProvinceId;
    }

    public String getSchoolStreetAddress() {
        return schoolStreetAddress;
    }

    public void setSchoolStreetAddress(String schoolStreetAddress) {
        this.schoolStreetAddress = schoolStreetAddress;
    }

    public Name getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(Name contactFullName) {
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

    public BirthDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(BirthDate birthDate) {
        this.birthDate = birthDate;
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

    public PositiveInteger getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(PositiveInteger studentCount) {
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

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public RegisterFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegisterFormStatus status) {
        this.status = status;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public static Builder builder() {
        return new RegisterForm.Builder();
    }

    public static RegisterForm fromDirectoryWithDocuments(
        UUID schoolDirectoryId, 
        RegisterFormVerificationMethod verificationMethod, 
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate birthDate, 
        String contactAddress, 
        String postalCode,  
        int studentCount
    ) {
        RegisterForm form = RegisterForm.builder()
            .schoolDirectoryId(schoolDirectoryId)
            .verificationMethod(verificationMethod)
            .contactFullName(Name.from(contactFullName))
            .identityNumber(IdentityNumber.from(identityNumber))
            .contactEmail(Email.from(contactEmail))
            .contactPhone(Phone.from(contactPhone))
            .birthDate(BirthDate.from(birthDate))
            .contactAddress(contactAddress)
            .postalCode(PostalCode.from(postalCode))
            .studentCount(PositiveInteger.from(studentCount))
            .status(RegisterFormStatus.PENDING)
            .build();
        return form;
    }

    public static RegisterForm fromDirectoryWithVerifiedOtp(
        UUID schoolDirectoryId, 
        RegisterFormVerificationMethod verificationMethod, 
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate birthDate, 
        String contactAddress, 
        String postalCode, 
        int studentCount
    ) {
        RegisterForm form = RegisterForm.builder()
            .schoolDirectoryId(schoolDirectoryId)
            .verificationMethod(verificationMethod)
            .contactFullName(Name.from(contactFullName))
            .identityNumber(IdentityNumber.from(identityNumber))
            .contactEmail(Email.from(contactEmail))
            .contactPhone(Phone.from(contactPhone))
            .birthDate(BirthDate.from(birthDate))
            .contactAddress(contactAddress)
            .postalCode(PostalCode.from(postalCode))
            .studentCount(PositiveInteger.from(studentCount))
            .status(RegisterFormStatus.AUTO_APPROVED)
            .build();
        return form;
    }

    public static RegisterForm selfDeclared(
        String schoolName, 
        String schoolDomain, 
        String schoolStreetAddress, 
        UUID schoolWardId, 
        UUID schoolCityId, 
        UUID schoolProvinceId,  
        String contactFullName, 
        String identityNumber, 
        String contactEmail, 
        String contactPhone, 
        LocalDate birthDate, 
        String contactAddress, 
        String postalCode, 
        int studentCount, 
        Instant now
    ) {
        RegisterForm form = RegisterForm.builder()
            .verificationMethod(RegisterFormVerificationMethod.DOCUMENT)
            .schoolName(Name.from(schoolName))
            .schoolDomain(SchoolDomain.from(schoolDomain))
            .schoolStreetAddress(schoolStreetAddress)
            .schoolWardId(schoolWardId)
            .schoolCityId(schoolCityId)
            .schoolProvinceId(schoolProvinceId)
            .contactFullName(Name.from(contactFullName))
            .identityNumber(IdentityNumber.from(identityNumber))
            .contactEmail(Email.from(contactEmail))
            .contactPhone(Phone.from(contactPhone))
            .birthDate(BirthDate.from(birthDate))
            .contactAddress(contactAddress)
            .postalCode(PostalCode.from(postalCode))
            .studentCount(PositiveInteger.from(studentCount))
            .status(RegisterFormStatus.PENDING)
            .build();
        return form;
    }

    public static class Builder {
        private UUID id;
        private UUID schoolDirectoryId;
        private Name schoolName;
        private SchoolDomain schoolDomain;
        private UUID schoolWardId;
        private UUID schoolCityId;
        private UUID schoolProvinceId;
        private String schoolStreetAddress;
        private Name contactFullName;
        private IdentityNumber identityNumber;
        private Phone contactPhone;
        private Email contactEmail;
        private BirthDate birthDate;
        private String contactAddress;
        private PostalCode postalCode;
        private PositiveInteger studentCount;
        private RegisterFormVerificationMethod verificationMethod;
        private Instant verifiedAt;
        private String rejectReason;
        private Instant rejectedAt;
        private Instant createdAt;
        private RegisterFormStatus status;
        private UUID reviewedBy;

        public Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolDirectoryId(UUID schoolDirectoryId) {
            this.schoolDirectoryId = schoolDirectoryId;
            return this;
        }

        public Builder schoolName(Name schoolName) {
            this.schoolName = schoolName;
            return this;
        }

        public Builder schoolDomain(SchoolDomain schoolDomain) {
            this.schoolDomain = schoolDomain;
            return this;
        }

        public Builder schoolWardId(UUID schoolWardId) {
            this.schoolWardId = schoolWardId;
            return this;
        }

        public Builder schoolCityId(UUID schoolCityId) {
            this.schoolCityId = schoolCityId;
            return this;
        }

        public Builder schoolProvinceId(UUID schoolProvinceId) {
            this.schoolProvinceId = schoolProvinceId;
            return this;
        }

        public Builder schoolStreetAddress(String schoolStreetAddress) {
            this.schoolStreetAddress = schoolStreetAddress;
            return this;
        }

        public Builder contactFullName(Name contactFullName) {
            this.contactFullName = contactFullName;
            return this;
        }

        public Builder identityNumber(IdentityNumber identityNumber) {
            this.identityNumber = identityNumber;
            return this;
        }

        public Builder contactPhone(Phone contactPhone) {
            this.contactPhone = contactPhone;
            return this;
        }

        public Builder contactEmail(Email contactEmail) {
            this.contactEmail = contactEmail;
            return this;
        }

        public Builder birthDate(BirthDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder contactAddress(String contactAddress) {
            this.contactAddress = contactAddress;
            return this;
        }

        public Builder postalCode(PostalCode postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder studentCount(PositiveInteger studentCount) {
            this.studentCount = studentCount;
            return this;
        }

        public Builder verificationMethod(RegisterFormVerificationMethod verificationMethod) {
            this.verificationMethod = verificationMethod;
            return this; 
        }

        public Builder verifiedAt(Instant verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }

        public Builder rejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
            return this;
        }

        public Builder rejectedAt(Instant rejectedAt) {
            this.rejectedAt = rejectedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder status(RegisterFormStatus status) {
            this.status = status;
            return this;
        }

        public Builder reviewedBy(UUID reviewedBy) {
            this.reviewedBy = reviewedBy;
            return this;
        }

        public RegisterForm build() {
            return new RegisterForm(this);
        }
    }
}
