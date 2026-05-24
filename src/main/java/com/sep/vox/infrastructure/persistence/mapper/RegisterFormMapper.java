package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
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
            new FullName(jpa.getContactFullName()), 
            new IdentityNumber(jpa.getIdentityNumber()), 
            new Phone(jpa.getContactPhone()), 
            new Email(jpa.getContactEmail()), 
            jpa.getDateOfBirth(),
            jpa.getContactAddress(),
            new SchoolDomain(jpa.getSchoolDomain()),
            jpa.getSchoolName(), 
            jpa.getSchoolAddress(), 
            new PostalCode(jpa.getPostalCode()), 
            jpa.getPosition(), 
            new StudentCount(jpa.getStudentCount()), 
            jpa.getReason(), 
            RegisterFormStatus.valueOf(jpa.getStatus()), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(),
            jpa.getUpdatedBy()
        );
    }

    public static RegisterFormJpaEntity toJpa(RegisterForm registerForm) {
        return new RegisterFormJpaEntity(
            registerForm.getId(), 
            valueOf(registerForm.getContactFullName()), 
            valueOf(registerForm.getIdentityNumber()), 
            valueOf(registerForm.getContactPhone()), 
            valueOf(registerForm.getContactEmail()), 
            registerForm.getDateOfBirth(),
            registerForm.getContactAddress(),
            valueOf(registerForm.getSchoolDomain()), 
            registerForm.getSchoolName(), 
            registerForm.getSchoolAddress(), 
            valueOf(registerForm.getPostalCode()), 
            registerForm.getPosition(), 
            valueOf(registerForm.getStudentCount()), 
            registerForm.getReason(), 
            registerForm.getStatus().name(), 
            registerForm.getCreatedAt(), 
            registerForm.getUpdatedAt(), 
            registerForm.getUpdatedBy()
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
}
