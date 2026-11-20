package com.sep.vox.domain.mapper;


import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RegisterFormDto;
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

public class RegisterFormDtoMapper {
    
    public static RegisterFormDto toRegisterFormDto(RegisterForm registerForm, String schoolDomain, String schoolName, String schoolAddress) {
        var directoryId = registerForm.getSchoolDirectoryId();
        return new RegisterFormDto(
            registerForm.getId(), 
            directoryId,
            directoryId == null ? registerForm.getSchoolName() : schoolName,
            directoryId == null ? valueOf(registerForm.getSchoolDomain()) : schoolDomain,
            registerForm.getSchoolDistrict(), 
            registerForm.getSchoolProvince(), 
            directoryId == null ? registerForm.getSchoolAddress() : schoolAddress,
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
            registerForm.getRejectedReason(), 
            valueOf(registerForm.getStatus())
        );
    }

    public static List<RegisterFormDto> toRegisterFormDtoList(List<RegisterForm> registerForms) {
        return registerForms.stream()
            .map(r -> RegisterFormDtoMapper.toRegisterFormDto(r, null, null, null))
            .toList();
    }

    public static PageResult<RegisterFormDto> toRegisterFormPage(PageResult<RegisterForm> registerFormPage) {
        return new PageResult<>(
            toRegisterFormDtoList(registerFormPage.content()),
            registerFormPage.page(),
            registerFormPage.size(),
            registerFormPage.totalElements(),
            registerFormPage.totalPages()
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

    private static String valueOf(DateOfBirth dateOfBirth) {
        return dateOfBirth == null ? null : dateOfBirth.value().toString();
    }

    private static String valueOf(RegisterFormVerificationMethod method) {
        return method == null ? null : method.name();
    }

    private static String valueOf(RegisterFormStatus status) {
        return status == null ? null : status.name();
    }
}
