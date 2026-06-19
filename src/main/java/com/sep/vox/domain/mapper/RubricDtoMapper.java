package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.model.rubric.Rubric;

import java.util.List;

public class RubricDtoMapper {

    public static RubricDto toRubricDto(Rubric rubric) {
        if (rubric == null) {
            return null;
        }
        return new RubricDto(
                rubric.getId(),
                rubric.getCode(),
                rubric.getName(),
                rubric.getDescription(),
                rubric.getLanguageId(),
                rubric.getFrameworkId(),
                rubric.getOwnerType() != null ? rubric.getOwnerType().name() : null,
                rubric.getCurrentVersionId(),
                rubric.getSchoolId()
        );
    }

    public static List<RubricDto> toRubricDtoList(List<Rubric> rubrics) {
        return rubrics.stream()
                .map(RubricDtoMapper::toRubricDto)
                .toList();
    }

    public static PageResult<RubricDto> toRubricPage(PageResult<Rubric> rubricPage) {
        return new PageResult<>(
                toRubricDtoList(rubricPage.content()),
                rubricPage.page(),
                rubricPage.size(),
                rubricPage.totalElements(),
                rubricPage.totalPages()
        );
    }
}