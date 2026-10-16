package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;

public record RubricVersionDto(
    UUID id,
    UUID rubricId,
    Integer version,
    String name,
    String description,
    RubricStatus status,
    Instant effectiveFrom,
    Instant effectiveTo,
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    RubricTotalScoreMethod totalScoreMethod,
    Instant createdAt,
    /**
     * Phiên bản mẫu mà bản này được sao ra, hoặc null nếu do chính chủ sở hữu soạn.
     *
     * <p>Có nó thì trang phiên bản nói được "Bản sao từ bộ tiêu chí mẫu của hệ thống" thay vì để
     * bản sao trông y hệt một bản tự soạn, và về sau đối chiếu được khi hệ thống ban hành bản
     * mới hơn bản mà trường đang dùng.
     */
    UUID sourceRubricVersionId
) {

    public static RubricVersionDto toDto(RubricVersion rubricVersion) {
        return new RubricVersionDto(
            rubricVersion.getId(),
            rubricVersion.getRubricId(),
            PositiveInteger.valueOf(rubricVersion.getVersion()),
            rubricVersion.getName(),
            rubricVersion.getDescription(),
            rubricVersion.getStatus(),
            rubricVersion.getEffectiveFrom(),
            rubricVersion.getEffectiveTo(),
            PositiveDecimal.valueOf(rubricVersion.getScoringScaleMin()),
            PositiveDecimal.valueOf(rubricVersion.getScoringScaleMax()),
            rubricVersion.getTotalScoreMethod(),
            rubricVersion.getCreatedAt(),
            rubricVersion.getSourceRubricVersionId()
        );
    }

    public static List<RubricVersionDto> toDtoList(List<RubricVersion> versions) {
        return versions.stream()
                .map(RubricVersionDto::toDto)
                .toList();
    }

    public static PageResult<RubricVersionDto> toDtoPage(PageResult<RubricVersion> versionPage) {
        return new PageResult<>(
                toDtoList(versionPage.content()),
                versionPage.page(),
                versionPage.size(),
                versionPage.totalElements(),
                versionPage.totalPages()
        );
    }
}
