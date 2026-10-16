package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.shared.PageResult;

public record RubricDto(
    UUID id,
    String name,
    String description,
    UUID learningLanguageId,
    UUID frameworkId,
    RubricOwnerType ownerType,
    UUID schoolId
) {

    public static RubricDto toDto(Rubric rubric) {
        return new RubricDto(
            rubric.getId(),
            rubric.getName(),
            rubric.getDescription(),
            rubric.getLearningLanguageId(),
            rubric.getFrameworkId(),
            rubric.getOwnerType(),
            rubric.getSchoolId()
        );
    }

    public static List<RubricDto> toDtoList(List<Rubric> rubrics) {
        return rubrics.stream()
                .map(RubricDto::toDto)
                .toList();
    }

    public static PageResult<RubricDto> toDtoPage(PageResult<Rubric> rubricPage) {
        return new PageResult<>(
                toDtoList(rubricPage.content()),
                rubricPage.page(),
                rubricPage.size(),
                rubricPage.totalElements(),
                rubricPage.totalPages()
        );
    }
}
