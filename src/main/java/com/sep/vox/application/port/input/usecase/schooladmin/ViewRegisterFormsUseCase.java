package com.sep.vox.application.port.input.usecase.schooladmin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewRegisterFormsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.registerform.RegisterFormDto;
import com.sep.vox.domain.mapper.RegisterFormDtoMapper;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.util.PageRequest;
import com.sep.vox.domain.util.PageResult;

@Service
public class ViewRegisterFormsUseCase implements IUseCase<ViewRegisterFormsQuery, PageResult<RegisterFormDto>>{

    private final RegisterFormRepository registerFormRepository;

    public ViewRegisterFormsUseCase(RegisterFormRepository registerFormRepository) {
        this.registerFormRepository = registerFormRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RegisterFormDto> execute(ViewRegisterFormsQuery input) {
        var forms = registerFormRepository.findAll(new PageRequest(input.page(), input.size()));
        return RegisterFormDtoMapper.toRegisterFormPage(forms);
    }
    
}
