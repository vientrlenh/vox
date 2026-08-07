package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.model.rubric.RubricVersion;

import java.util.List;

public class RubricVersionDtoMapper {

    public static RubricVersionDto toRubricVersionDto(RubricVersion version) {
        if (version == null) {
            return null;
        }
        return new RubricVersionDto(
                version.getId(),
                version.getRubricId(),
                version.getVersion(),
                version.getCode(),
                version.getName(),
                version.getDescription(),
                version.getStatus() != null ? version.getStatus().name() : null,
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getScoringScaleMin(),
                version.getScoringScaleMax(),
                version.getTotalScoreMethod() != null ? version.getTotalScoreMethod().name() : null,
                version.getCreatedAt()
        );
    }

    public static List<RubricVersionDto> toRubricVersionDtoList(List<RubricVersion> versions) {
        return versions.stream()
                .map(RubricVersionDtoMapper::toRubricVersionDto)
                .toList();
    }

    public static PageResult<RubricVersionDto> toRubricVersionPage(PageResult<RubricVersion> versionPage) {
        return new PageResult<>(
                toRubricVersionDtoList(versionPage.content()),
                versionPage.page(),
                versionPage.size(),
                versionPage.totalElements(),
                versionPage.totalPages()
        );
    }
}
