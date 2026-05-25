package com.sep.vox.application.port.input.usecase.systemadmin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.mapper.RegisterFormDtoMapper;
import com.sep.vox.domain.repository.RegisterFormRepository;

@Service
public class ViewRegisterFormDetailsUseCase implements IUseCase<ViewRegisterFormDetailsQuery, RegisterFormDto> {

    private final RegisterFormRepository registerFormRepository;

    public ViewRegisterFormDetailsUseCase(RegisterFormRepository registerFormRepository) {
        this.registerFormRepository = registerFormRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RegisterFormDto execute(ViewRegisterFormDetailsQuery input) {
        var registerForm = registerFormRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đăng ký"));
        return RegisterFormDtoMapper.toRegisterFormDto(registerForm);
    }
    
}
