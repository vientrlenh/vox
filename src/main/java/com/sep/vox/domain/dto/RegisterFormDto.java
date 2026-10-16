package com.sep.vox.domain.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.PersonName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;

public record RegisterFormDto(
    UUID id,
    UUID schoolDirectoryId,
    String schoolName,
    String schoolDomain,
    UUID schoolWardId,
    UUID schoolCityId,
    UUID schoolProvinceId,
    String schoolStreetAddress,
    String contactFullName,
    String identityNumber,
    String contactPhone,
    String contactEmail,
    LocalDate birthDate,
    String contactAddress,
    String postalCode,
    Integer studentCount,
    RegisterFormVerificationMethod verificationMethod,
    String rejectReason,
    RegisterFormStatus status
) {

    public static RegisterFormDto toDto(RegisterForm registerForm) {
        return toDto(registerForm, null, null, null);
    }

    /**
     * A form that points to a school directory entry takes the school identity (name, domain, street address)
     * from that entry; otherwise it uses what the applicant typed into the form itself.
     */
    public static RegisterFormDto toDto(
            RegisterForm registerForm, String schoolDomain, String schoolName, String schoolStreetAddress) {
        UUID directoryId = registerForm.getSchoolDirectoryId();
        boolean fromDirectory = directoryId != null;
        return new RegisterFormDto(
            registerForm.getId(),
            directoryId,
            fromDirectory ? schoolName : registerForm.getSchoolName(),
            fromDirectory ? schoolDomain : SchoolDomain.valueOf(registerForm.getSchoolDomain()),
            registerForm.getSchoolWardId(),
            registerForm.getSchoolCityId(),
            registerForm.getSchoolProvinceId(),
            fromDirectory ? schoolStreetAddress : registerForm.getSchoolStreetAddress(),
            PersonName.valueOf(registerForm.getContactFullName()),
            IdentityNumber.valueOf(registerForm.getIdentityNumber()),
            Phone.valueOf(registerForm.getContactPhone()),
            Email.valueOf(registerForm.getContactEmail()),
            BirthDate.valueOf(registerForm.getBirthDate()),
            registerForm.getContactAddress(),
            PostalCode.valueOf(registerForm.getPostalCode()),
            PositiveInteger.valueOf(registerForm.getStudentCount()),
            registerForm.getVerificationMethod(),
            registerForm.getRejectReason(),
            registerForm.getStatus()
        );
    }

    public static List<RegisterFormDto> toDtoList(List<RegisterForm> registerForms) {
        return registerForms.stream()
            .map(RegisterFormDto::toDto)
            .toList();
    }

    public static PageResult<RegisterFormDto> toDtoPage(PageResult<RegisterForm> registerFormPage) {
        return new PageResult<>(
            toDtoList(registerFormPage.content()),
            registerFormPage.page(),
            registerFormPage.size(),
            registerFormPage.totalElements(),
            registerFormPage.totalPages()
        );
    }
}
