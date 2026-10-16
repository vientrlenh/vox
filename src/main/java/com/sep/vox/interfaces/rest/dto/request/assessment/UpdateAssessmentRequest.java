package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentCommand;
import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record UpdateAssessmentRequest(
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer assessmentTimeDurationSecond,

    AssessmentResultDecisionMethod resultDecisionMethod,

    Boolean requiresOtp,

    /**
     * Cấu hình giám sát, cùng dạng thô như lúc tạo ({@code ["CAMERA","SCREEN"]}).
     *
     * <p>Bỏ trống (null) = giữ nguyên cấu hình hiện tại, giống mọi trường khác của API này. Danh
     * sách RỖNG mới là "tắt giám sát" -- lúc tạo thì null mang nghĩa đó, nhưng ở đây null đã có
     * nghĩa "không đụng tới" nên phải tách ra.
     */
    List<String> requiredStreamTypes,

    String streamTypePermission,

    /**
     * Ngưỡng tin cậy AI theo PHẦN TRĂM (0-100). Bỏ trống = GIỮ NGUYÊN giá trị đang có, giống mọi
     * trường khác của lệnh cập nhật -- nên hiện chưa có cách xoá ngưỡng đã đặt về lại "không đặt".
     */
    @DecimalMin(value = "0.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "Ngưỡng tin cậy phải từ 0 đến 100")
    @Digits(integer = 3, fraction = 2, message = "Ngưỡng tin cậy tối đa 2 chữ số thập phân")
    BigDecimal aiConfidenceThresholdPercent
) {

    public static UpdateAssessmentCommand toCommand(UUID assessmentId, UpdateAssessmentRequest request) {
                return new UpdateAssessmentCommand(
                    assessmentId,
                    request.name(),
                    request.description(),
                    request.openAt(),
                    request.closeAt(),
                    request.assessmentPolicyId(),
                    request.maxAttempt(),
                    request.assessmentTimeDurationSecond(),
                    request.resultDecisionMethod(),
                    request.requiresOtp(),
                    request.requiredStreamTypes(),
                    request.streamTypePermission()
        ,
                    request.aiConfidenceThresholdPercent()
                );
    }
}
