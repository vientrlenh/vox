package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricRequest(
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        String code,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        String name,

        String description,

        @NotNull(message = "Ngôn ngữ không được để trống")
        UUID languageId,

        @NotNull(message = "Khung tiêu chuẩn (Framework) không được để trống")
        UUID frameworkId,

        @NotEmpty(message = "Rubric phải có ít nhất 1 phiên bản")
        @Valid
        List<RubricSchoolVersionRequest> versions
) {
    public record RubricSchoolVersionRequest(
            @NotNull(message = "Version (Số phiên bản) không được để trống")
            Integer version,

            @NotNull(message = "Điểm sàn (Min Score) không được để trống")
            BigDecimal scoringScaleMin,

            @NotNull(message = "Điểm trần (Max Score) không được để trống")
            BigDecimal scoringScaleMax,

            @NotNull(message = "Phương pháp tính tổng điểm không được để trống")
            RubricTotalScoreMethod totalScoreMethod,

            @NotBlank(message = "Ngày bắt đầu áp dụng không được để trống")
            String effectiveFrom,

            // Đã gỡ @NotBlank để cho phép null (Áp dụng vô thời hạn)
            String effectiveTo
    ) {}
}