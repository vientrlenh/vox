package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricRequest(
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        @Size(max = 50, message = "Mã bộ tiêu chí tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        @Size(max = 255, message = "Tên bộ tiêu chí tối đa 255 ký tự")
        String name,

        @Size(max = 2048, message = "Mô tả tối đa 2048 ký tự")
        String description,

        @NotNull(message = "Ngôn ngữ không được để trống")
        UUID languageId,

        @NotNull(message = "Khung tiêu chuẩn (Framework) không được để trống")
        UUID frameworkId,

        @NotEmpty(message = "Rubric hệ thống phải có ít nhất 1 phiên bản")
        @Valid // Quan trọng: Để Spring validate các trường bên trong List
        List<RubricVersionItemRequest> versions
) {
        public record RubricVersionItemRequest(
                @NotNull(message = "Phiên bản không được để trống")
                @Min(value = 1, message = "Phiên bản phải là một số nguyên dương")
                Integer version,

                @NotNull(message = "Điểm sàn (Min Score) không được để trống")
                @DecimalMin(value = "0.0", inclusive = true, message = "Điểm sàn phải lớn hơn hoặc bằng 0")
                BigDecimal scoringScaleMin,

                @NotNull(message = "Điểm trần (Max Score) không được để trống")
                @DecimalMin(value = "0.01", inclusive = true, message = "Điểm trần phải lớn hơn 0")
                BigDecimal scoringScaleMax,

                @NotNull(message = "Phương pháp tính tổng điểm không được để trống")
                RubricTotalScoreMethod totalScoreMethod,
                @NotBlank(message = "Ngày bắt đầu không được để trống")
                String effectiveFrom,

                String effectiveTo
        ) {}
}