package com.sep.vox.application.port.input.usecase.school;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;

@Service
public class ViewSchoolsUseCase implements IUseCase<ViewSchoolsQuery, PageResult<SchoolDto>> {

    private final SchoolRepository schoolRepository;

    public ViewSchoolsUseCase(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Override
    public PageResult<SchoolDto> execute(ViewSchoolsQuery input) {
        var schools = schoolRepository.findAll(input.page(), input.size());
        return SchoolDtoMapper.toSchoolDtoPage(schools);
    }
    
}
