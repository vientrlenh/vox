package com.sep.vox.application.port.input.usecase.school;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewSchoolsUseCase implements IUseCase<ViewSchoolsQuery, PageResult<SchoolDto>> {

    private final SchoolRepository schoolRepository;

    public ViewSchoolsUseCase(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Override
    public PageResult<SchoolDto> execute(ViewSchoolsQuery input) {
        int page = input.pageRequest().page();
        int size = input.pageRequest().size();
        PageResult<School> schools = schoolRepository.findAll(page, size, input.search(), input.isActive());
        return SchoolDto.toDtoPage(schools);
    }
    
}
