package com.sep.vox.application.port.input.usecase.schooldirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolDirectoryCursorPageQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.CursorPage;
import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.mapper.SchoolDirectoryDtoMapper;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class ViewSchoolDirectoryCursorPageUseCase implements IUseCase<ViewSchoolDirectoryCursorPageQuery, CursorPage<SchoolDirectoryDto>> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;

    public ViewSchoolDirectoryCursorPageUseCase(SchoolDirectoryRepository schoolDirectoryRepository) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<SchoolDirectoryDto> execute(ViewSchoolDirectoryCursorPageQuery input) {
        var results = input.cursor() == null ? schoolDirectoryRepository.findAllByOrderByIdAsc(input.limit()) : schoolDirectoryRepository.findByIdGreaterThanOrderByIdAsc(input.cursor(), input.limit());

        var hasNext = results.size() > input.limit();
        var page = hasNext ? results.subList(0, input.limit()) : results;
        var nextCursor = hasNext ? page.getLast().getId() : null;

        return new CursorPage<>(
            SchoolDirectoryDtoMapper.toSchoolDirectoryDtoList(page), 
            nextCursor, 
            hasNext
        );
    }
    
}
