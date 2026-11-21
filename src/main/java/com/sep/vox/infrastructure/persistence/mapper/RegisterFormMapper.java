package com.sep.vox.infrastructure.persistence.mapper;

import java.time.LocalDate;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
import com.sep.vox.infrastructure.persistence.entity.registerform.RegisterFormJpaEntity;

public final class RegisterFormMapper {
    
    public static RegisterForm toDomain(RegisterFormJpaEntity jpa) {
        return RegisterForm.builder()
            .id(jpa.getId())
            .schoolDirectoryId(jpa.getSchoolDirectoryId())
            .schoolName(jpa.getSchoolName())
            .schoolDomain(new SchoolDomain(jpa.getSchoolDomain()))
            .schoolDistrict(jpa.getSchoolDistrict())
            .schoolProvince(jpa.getSchoolProvince())
            .schoolAddress(jpa.getSchoolAddress())
            .contactFullName(new FullName(jpa.getContactFullName()))
            .identityNumber(new IdentityNumber(jpa.getIdentityNumber()))
            .contactPhone(new Phone(jpa.getContactPhone()))
            .contactEmail(new Email(jpa.getContactEmail()))
            .dateOfBirth(new DateOfBirth(jpa.getDateOfBirth()))
            .contactAddress(jpa.getContactAddress())
            .postalCode(new PostalCode(jpa.getPostalCode()))
            .position(jpa.getPosition())
            .studentCount(new StudentCount(jpa.getStudentCount()))
            .verificationMethod(verificationMethodFromString(jpa.getVerificationMethod()))
            .verifiedAt(jpa.getVerifiedAt())
            .rejectedReason(jpa.getRejectedReason())
            .status(statusFromString(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .reviewedBy(jpa.getReviewedBy())
            .build();
    }

    public static RegisterFormJpaEntity toJpa(RegisterForm registerForm) {
        return RegisterFormJpaEntity.builder()
            .id(registerForm.getId())
            .schoolDirectoryId(registerForm.getSchoolDirectoryId())
            .schoolName(registerForm.getSchoolName())
            .schoolDomain(valueOf(registerForm.getSchoolDomain()))
            .schoolDistrict(registerForm.getSchoolDistrict())
            .schoolProvince(registerForm.getSchoolProvince())
            .schoolAddress(registerForm.getSchoolAddress())
            .contactFullName(valueOf(registerForm.getContactFullName()))
            .identityNumber(valueOf(registerForm.getIdentityNumber()))
            .contactPhone(valueOf(registerForm.getContactPhone()))
            .contactEmail(valueOf(registerForm.getContactEmail()))
            .dateOfBirth(valueOf(registerForm.getDateOfBirth()))
            .contactAddress(registerForm.getContactAddress())
            .postalCode(valueOf(registerForm.getPostalCode()))
            .position(registerForm.getPosition())
            .studentCount(valueOf(registerForm.getStudentCount()))
            .verificationMethod(valueOf(registerForm.getVerificationMethod()))
            .verifiedAt(registerForm.getVerifiedAt())
            .rejectedReason(registerForm.getRejectedReason())
            .status(valueOf(registerForm.getStatus()))
            .createdAt(registerForm.getCreatedAt())
            .updatedAt(registerForm.getUpdatedAt())
            .reviewedBy(registerForm.getReviewedBy())
            .build();
    }

    private static String valueOf(FullName fullName) {
        return fullName == null ? null : fullName.value();
    }

    private static String valueOf(IdentityNumber identityNumber) {
        return identityNumber == null ? null : identityNumber.value();
    }

    private static String valueOf(Phone phone) {
        return phone == null ? null : phone.value();
    }

    private static String valueOf(Email email) {
        return email == null ? null : email.value();
    }

    private static String valueOf(SchoolDomain schoolDomain) {
        return schoolDomain == null ? null : schoolDomain.value();
    }

    private static String valueOf(PostalCode postalCode) {
        return postalCode == null ? null : postalCode.value();
    }

    private static int valueOf(StudentCount studentCount) {
        return studentCount == null ? 0 : studentCount.value();
    }

    private static LocalDate valueOf(DateOfBirth dateOfBirth) {
        return dateOfBirth == null ? null : dateOfBirth.value();
    }

    private static RegisterFormVerificationMethod verificationMethodFromString(String method) {
        return method == null ? null : RegisterFormVerificationMethod.valueOf(method);
    }

    private static RegisterFormStatus statusFromString(String status) {
        return status == null ? null : RegisterFormStatus.valueOf(status);
    }

    private static String valueOf(RegisterFormVerificationMethod method) {
        return method == null ? null : method.name();
    }

    private static String valueOf(RegisterFormStatus status) {
        return status == null ? null : status.name();
    }
}
