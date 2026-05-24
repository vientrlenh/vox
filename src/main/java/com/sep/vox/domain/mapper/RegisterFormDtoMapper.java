package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.registerform.RegisterFormDto;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.util.PageResult;

public class RegisterFormDtoMapper {
    
    public static RegisterFormDto toRegisterFormDto(RegisterForm registerForm) {
        return new RegisterFormDto(
            registerForm.getId(), 
            registerForm.getContactFullName(), 
            registerForm.getIdentityNumber(), 
            registerForm.getContactPhone(), 
            registerForm.getContactEmail(), 
            registerForm.getDateOfBirth(), 
            registerForm.getContactAddress(), 
            registerForm.getSchoolDomain(), 
            registerForm.getSchoolName(), 
            registerForm.getSchoolAddress(), 
            registerForm.getPostalCode(), 
            registerForm.getPosition(), 
            registerForm.getStudentCount(), 
            registerForm.getReason(), 
            registerForm.getStatus().name()
        );
    }

    public static List<RegisterFormDto> toRegisterFormDtoList(List<RegisterForm> registerForms) {
        return registerForms.stream()
            .map(RegisterFormDtoMapper::toRegisterFormDto)
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
}
