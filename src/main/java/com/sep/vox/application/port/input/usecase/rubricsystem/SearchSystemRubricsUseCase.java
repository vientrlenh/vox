package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.mapper.RubricDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.repository.RubricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchSystemRubricsUseCase implements IUseCase<SearchSystemRubricsQuery, PageResult<RubricDto>> {

    private final RubricRepository rubricRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricDto> execute(SearchSystemRubricsQuery query) {

        PageResult<Rubric> pageResult = rubricRepository.searchSystemRubrics(
                query.keyword(),
                query.frameworkId(),
                query.languageId(),
                query.page(),
                query.size()
        );

        return new PageResult<>(
                pageResult.content().stream()
                        .map(RubricDtoMapper::toRubricDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}