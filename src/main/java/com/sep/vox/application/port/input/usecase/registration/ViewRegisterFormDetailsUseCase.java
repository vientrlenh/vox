package com.sep.vox.application.port.input.usecase.registration;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.school.SchoolDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.registration.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.mapper.RegisterFormDtoMapper;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
@RequiredArgsConstructor
public class ViewRegisterFormDetailsUseCase implements IUseCase<ViewRegisterFormDetailsQuery, RegisterFormDto> {

    private final RegisterFormRepository registerFormRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;

    @Override
    @Transactional(readOnly = true)
    public RegisterFormDto execute(ViewRegisterFormDetailsQuery input) {
        RegisterForm registerForm = registerFormRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Register form not found"));
        String schoolDomain = null;
        String schoolName = null;
        String schoolAddress = null;
        if (registerForm.getSchoolDirectoryId() != null) {
            SchoolDirectory schoolDirectory = schoolDirectoryRepository.findById(registerForm.getSchoolDirectoryId())
                .orElseThrow(() -> new ResourceNotFoundException("School directory entry not found"));
            schoolDomain = schoolDirectory.getDomain();
            schoolName = schoolDirectory.getName();
            schoolAddress = schoolDirectory.getAddress();
        }
        return RegisterFormDtoMapper.toRegisterFormDto(registerForm, schoolDomain, schoolName, schoolAddress);
    }
    
}
