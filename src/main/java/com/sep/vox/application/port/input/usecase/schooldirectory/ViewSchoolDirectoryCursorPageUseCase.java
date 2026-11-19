package com.sep.vox.application.port.input.usecase.schooldirectory;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.util.List;
import java.util.UUID;
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
        List<SchoolDirectory> results = input.cursor() == null ? schoolDirectoryRepository.findAllByOrderByIdAsc(input.limit()) : schoolDirectoryRepository.findByIdGreaterThanOrderByIdAsc(input.cursor(), input.limit());

        boolean hasNext = results.size() > input.limit();
        List<SchoolDirectory> page = hasNext ? results.subList(0, input.limit()) : results;
        UUID nextCursor = hasNext ? page.getLast().getId() : null;

        return new CursorPage<>(
            SchoolDirectoryDtoMapper.toSchoolDirectoryDtoList(page), 
            nextCursor, 
            hasNext
        );
    }
    
}
