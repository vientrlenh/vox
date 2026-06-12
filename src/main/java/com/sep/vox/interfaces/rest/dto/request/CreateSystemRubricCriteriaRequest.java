package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricCriteriaRequest(
        @NotEmpty(message = "Phải có ít nhất 1 tiêu chí được gửi lên")
        @Valid
        List<CriterionItemRequest> criteria
) {
        public record CriterionItemRequest(
                @NotNull(message = "Framework Criterion ID không được để trống")
                UUID frameworkCriterionId,

                @NotBlank(message = "Mã tiêu chí không được để trống")
                String code,

                @NotBlank(message = "Tên tiêu chí không được để trống")
                String name,

                String description,

                @Valid // Để Spring validate luôn cả các ví dụ bên trong
                List<CriterionExampleRequest> examples, // BỔ SUNG EXAMPLES

                @NotNull(message = "Trọng số không được để trống")
                @DecimalMin(value = "0.0", message = "Trọng số phải lớn hơn hoặc bằng 0")
                BigDecimal weight,

                @NotNull(message = "Điểm tối thiểu không được để trống")
                BigDecimal minScore,

                @NotNull(message = "Điểm tối đa không được để trống")
                BigDecimal maxScore,

                @NotNull(message = "Thứ tự (Order) không được để trống")
                @Min(value = 1, message = "Thứ tự (Order) phải lớn hơn 0")
                Integer order,

                @NotNull(message = "Cờ bắt buộc (Is Required) không được để trống")
                Boolean isRequired
        ) {}

        public record CriterionExampleRequest(
                @NotBlank(message = "Nội dung ví dụ (transcript) không được để trống")
                String transcript,

                @NotBlank(message = "Giải thích (explanation) không được để trống")
                String explanation,

                @NotNull(message = "Điểm kỳ vọng không được để trống")
                BigDecimal expectedScore
        ) {}
}