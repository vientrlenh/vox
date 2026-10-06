package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.RegisterFormJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class RegisterFormMapper {

    private RegisterFormMapper() {}
    
    public static RegisterForm toDomain(RegisterFormJpaEntity jpa) {
        try {
            return RegisterForm.builder()
                .id(jpa.getId())
                .schoolDirectoryId(jpa.getSchoolDirectoryId())
                .schoolName(Name.from(jpa.getSchoolName()))
                .schoolDomain(SchoolDomain.from(jpa.getSchoolDomain()))
                .schoolWardId(jpa.getSchoolWardId())
                .schoolCityId(jpa.getSchoolCityId())
                .schoolProvinceId(jpa.getSchoolProvinceId())
                .schoolStreetAddress(jpa.getSchoolStreetAddress())
                .contactFullName(Name.from(jpa.getContactFullName()))
                .identityNumber(IdentityNumber.from(jpa.getIdentityNumber()))
                .contactPhone(Phone.from(jpa.getContactPhone()))
                .contactEmail(Email.from(jpa.getContactEmail()))
                .birthDate(BirthDate.from(jpa.getBirthDate()))
                .contactAddress(jpa.getContactAddress())
                .postalCode(PostalCode.from(jpa.getPostalCode()))
                .studentCount(PositiveInteger.from(jpa.getStudentCount()))
                .verificationMethod(RegisterFormVerificationMethod.from(jpa.getVerificationMethod()))
                .verifiedAt(jpa.getVerifiedAt())
                .rejectReason(jpa.getRejectReason())
                .rejectedAt(jpa.getRejectedAt())
                .status(RegisterFormStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .reviewedBy(jpa.getReviewedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping form entity to form model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static RegisterFormJpaEntity toJpa(RegisterForm form) {
        try {
            return RegisterFormJpaEntity.builder()
                .id(form.getId())
                .schoolDirectoryId(form.getSchoolDirectoryId())
                .schoolName(Name.valueOf(form.getSchoolName()))
                .schoolDomain(SchoolDomain.valueOf(form.getSchoolDomain()))
                .schoolWardId(form.getSchoolWardId())
                .schoolCityId(form.getSchoolCityId())
                .schoolProvinceId(form.getSchoolProvinceId())
                .schoolStreetAddress(form.getSchoolStreetAddress())
                .contactFullName(Name.valueOf(form.getContactFullName()))
                .identityNumber(IdentityNumber.valueOf(form.getIdentityNumber()))
                .contactPhone(Phone.valueOf(form.getContactPhone()))
                .contactEmail(Email.valueOf(form.getContactEmail()))
                .birthDate(BirthDate.valueOf(form.getBirthDate()))
                .contactAddress(form.getContactAddress())
                .postalCode(PostalCode.valueOf(form.getPostalCode()))
                .studentCount(PositiveInteger.valueOf(form.getStudentCount()))
                .verificationMethod(RegisterFormVerificationMethod.value(form.getVerificationMethod()))
                .verifiedAt(form.getVerifiedAt())
                .rejectReason(form.getRejectReason())
                .rejectedAt(form.getRejectedAt())
                .status(RegisterFormStatus.value(form.getStatus()))
                .createdAt(form.getCreatedAt())
                .reviewedBy(form.getReviewedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping form model to form entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
        
    }
}
