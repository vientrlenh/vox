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
import com.sep.vox.infrastructure.persistence.entity.RegisterFormJpaEntity;

public final class RegisterFormMapper {
    
    public static RegisterForm toDomain(RegisterFormJpaEntity jpa) {
        return new RegisterForm(
            jpa.getId(), 
            jpa.getSchoolDirectoryId(), 
            jpa.getSchoolName(), 
            new SchoolDomain(jpa.getSchoolDomain()), 
            jpa.getSchoolDistrict(), 
            jpa.getSchoolProvince(), 
            jpa.getSchoolAddress(),
            new FullName(jpa.getContactFullName()), 
            new IdentityNumber(jpa.getIdentityNumber()), 
            new Phone(jpa.getContactPhone()),
            new Email(jpa.getContactEmail()), 
            new DateOfBirth(jpa.getDateOfBirth()), 
            jpa.getContactAddress(), 
            new PostalCode(jpa.getPostalCode()), 
            jpa.getPosition(), 
            new StudentCount(jpa.getStudentCount()), 
            verificationMethodFromString(jpa.getVerificationMethod()), 
            jpa.getVerifiedAt(), 
            jpa.getRejectedReason(), 
            statusFromString(jpa.getStatus()), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getReviewedBy()
        );
    }

    public static RegisterFormJpaEntity toJpa(RegisterForm registerForm) {
        return new RegisterFormJpaEntity(
            registerForm.getId(), 
            registerForm.getSchoolDirectoryId(), 
            valueOf(registerForm.getSchoolDomain()), 
            registerForm.getSchoolName(), 
            registerForm.getSchoolDistrict(), 
            registerForm.getSchoolProvince(), 
            registerForm.getSchoolAddress(), 
            valueOf(registerForm.getContactFullName()), 
            valueOf(registerForm.getIdentityNumber()), 
            valueOf(registerForm.getContactPhone()), 
            valueOf(registerForm.getContactEmail()), 
            valueOf(registerForm.getDateOfBirth()), 
            registerForm.getContactAddress(), 
            valueOf(registerForm.getPostalCode()), 
            registerForm.getPosition(), 
            valueOf(registerForm.getStudentCount()), 
            valueOf(registerForm.getVerificationMethod()), 
            registerForm.getVerifiedAt(), 
            registerForm.getRejectedReason(), 
            valueOf(registerForm.getStatus()), 
            registerForm.getCreatedAt(), 
            registerForm.getUpdatedAt(), 
            registerForm.getReviewedBy()
        );
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
