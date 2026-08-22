package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CloneSystemAssessmentPolicyRequest(
        @NotNull(message = "Chính sách mẫu không được để trống")
        UUID sourcePolicyId,

        // Mã/tên do TRƯỜNG đặt, không sao từ bản mẫu: đây là thứ phân biệt các bản sao của cùng một
        // bản mẫu và cũng là thứ ràng buộc unique của bảng rubrics kiểm.
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        @Size(max = 50, message = "Mã bộ tiêu chí tối đa 50 ký tự")
        String rubricCode,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        @Size(max = 255, message = "Tên bộ tiêu chí tối đa 255 ký tự")
        String rubricName,

        @Size(max = 2048, message = "Mô tả tối đa 2048 ký tự")
        String rubricDescription,

        @Pattern(regexp = "SUM|WEIGHTED_AVERAGE", message = "Cách tính điểm chỉ nhận SUM hoặc WEIGHTED_AVERAGE")
        String totalScoreMethod,

        // Chỉ điền khi bản mẫu KHÔNG gắn Khối; khi đó phải chọn đúng 1 trong 3. Bản mẫu đã gắn Khối
        // thì bản sao giữ nguyên khối đó và cả ba phải để trống.
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,

        @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
        String effectiveFrom,

        String effectiveTo
) {}
