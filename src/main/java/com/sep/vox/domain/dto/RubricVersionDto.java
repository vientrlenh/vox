package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RubricVersionDto(
        UUID id,
        UUID rubricId,
        int version,
        String code,
        String name,
        String description,
        String status,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod,
        Instant createdAt,
        /**
         * Phiên bản mẫu mà bản này được sao ra, hoặc null nếu do chính chủ sở hữu soạn.
         *
         * <p>Có nó thì trang phiên bản nói được "Bản sao từ bộ tiêu chí mẫu của hệ thống" thay vì để
         * bản sao trông y hệt một bản tự soạn, và về sau đối chiếu được khi hệ thống ban hành bản
         * mới hơn bản mà trường đang dùng.
         */
        UUID sourceRubricVersionId
) {}
