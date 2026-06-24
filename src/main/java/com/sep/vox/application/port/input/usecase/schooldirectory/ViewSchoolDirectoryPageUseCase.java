package com.sep.vox.application.port.input.usecase.schooldirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolDirectoryPageQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.mapper.SchoolDirectoryDtoMapper;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class ViewSchoolDirectoryPageUseCase implements IUseCase<ViewSchoolDirectoryPageQuery, PageResult<SchoolDirectoryDto>> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;

    public ViewSchoolDirectoryPageUseCase(SchoolDirectoryRepository schoolDirectoryRepository) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolDirectoryDto> execute(ViewSchoolDirectoryPageQuery input) {
        var result = schoolDirectoryRepository.findAll(input.page(), input.size());
        return SchoolDirectoryDtoMapper.toSchoolDirectoryDtoPage(result);
    }
    
}
