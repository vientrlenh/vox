package com.sep.vox.application.port.input.usecase.registration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.mapper.RegisterFormDtoMapper;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class ViewRegisterFormDetailsUseCase implements IUseCase<ViewRegisterFormDetailsQuery, RegisterFormDto> {

    private final RegisterFormRepository registerFormRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;

    public ViewRegisterFormDetailsUseCase(RegisterFormRepository registerFormRepository, SchoolDirectoryRepository schoolDirectoryRepository) {
        this.registerFormRepository = registerFormRepository;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RegisterFormDto execute(ViewRegisterFormDetailsQuery input) {
        var registerForm = registerFormRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đăng ký"));
        String schoolDomain = null;
        String schoolName = null;
        String schoolAddress = null;
        if (registerForm.getSchoolDirectoryId() != null) {
            var schoolDirectory = schoolDirectoryRepository.findById(registerForm.getSchoolDirectoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường"));
            schoolDomain = schoolDirectory.getDomain();
            schoolName = schoolDirectory.getName();
            schoolAddress = schoolDirectory.getAddress();
        }
        return RegisterFormDtoMapper.toRegisterFormDto(registerForm, schoolDomain, schoolName, schoolAddress);
    }
    
}
