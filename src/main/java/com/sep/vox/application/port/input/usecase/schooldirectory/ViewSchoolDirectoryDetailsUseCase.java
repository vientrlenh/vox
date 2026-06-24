package com.sep.vox.application.port.input.usecase.schooldirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolDirectoryDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.mapper.SchoolDirectoryDtoMapper;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class ViewSchoolDirectoryDetailsUseCase implements IUseCase<ViewSchoolDirectoryDetailsQuery, SchoolDirectoryDto>{

    private final SchoolDirectoryRepository schoolDirectoryRepository;

    public ViewSchoolDirectoryDetailsUseCase(SchoolDirectoryRepository schoolDirectoryRepository) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolDirectoryDto execute(ViewSchoolDirectoryDetailsQuery input) {
        var directory = schoolDirectoryRepository.findById(input.id()).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
        return SchoolDirectoryDtoMapper.toSchoolDirectoryDto(directory);
    }
    
}
