package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.port.input.query.SearchSystemRubricsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.mapper.RubricDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.repository.RubricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchSystemRubricsUseCase implements IUseCase<SearchSystemRubricsQuery, PageResult<RubricDto>> {

    private final RubricRepository rubricRepository;

    public SearchSystemRubricsUseCase(RubricRepository rubricRepository) {
        this.rubricRepository = rubricRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricDto> execute(SearchSystemRubricsQuery query) {

        // 1. Gọi thẳng xuống DB để search (Repository sẽ xử lý SQL lọc động)
        PageResult<Rubric> pageResult = rubricRepository.searchSystemRubrics(
                query.keyword(),
                query.frameworkId(),
                query.languageId(),
                query.pageRequest()
        );

        // 2. Chuyển đổi từ Domain Model (Rubric) sang DTO (RubricDto)
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