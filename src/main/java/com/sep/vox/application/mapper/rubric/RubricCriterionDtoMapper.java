package com.sep.vox.application.mapper.rubric;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

@Service
@RequiredArgsConstructor
public class RubricCriterionDtoMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public RubricCriterionDto toDto(RubricCriterion criterion) {
        if (criterion == null) {
            return null;
        }
        return new RubricCriterionDto(
                criterion.getId(),
                criterion.getRubricVersionId(),
                criterion.getFrameworkCriterionId(),
                criterion.getCode(),
                criterion.getName(),
                criterion.getDescription(),
                toExamplesJson(criterion.getExamples()),
                criterion.getWeight(),
                criterion.getMinScore(),
                criterion.getMaxScore(),
                criterion.getOrder(),
                criterion.isRequired(),
                criterion.getCreatedAt(),
                criterion.getUpdatedAt(),
                criterion.createdByUserId().orElse(null),
                criterion.updatedByUserId().orElse(null)
        );
    }

    public List<RubricCriterionDto> toDtoList(List<RubricCriterion> criteria) {
        return criteria.stream()
                .map(this::toDto)
                .toList();
    }

    public PageResult<RubricCriterionDto> toPage(PageResult<RubricCriterion> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    private String toExamplesJson(RubricCriterionExamples examples) {
        if (examples == null) {
            return null;
        }
        return jsonSerializationPort.toJson(examples);
    }
}
