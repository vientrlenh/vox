package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request tạo mới 1 Scoring Rule (luật chấm điểm tự động) cho Assessment Policy hệ thống.
// conditionParams/actionParams là JSON tự do, cấu trúc bắt buộc phải khớp với conditionType/actionType đã chọn
// (vd conditionType = DURATION_LESS_THAN thì conditionParams bắt buộc có field "seconds").
public record CreateScoringRuleRequest(

        @NotBlank(message = "Mã Rule (code) không được để trống")
        String code,

        @NotBlank(message = "Tên Rule không được để trống")
        String name,

        String description,

        @NotNull(message = "Loại điều kiện (conditionType) không được để trống")
        ScoringRuleConditionType conditionType,

        // Ví dụ: conditionType=DURATION_LESS_THAN -> {"seconds": 3}
        @NotNull(message = "Tham số điều kiện (conditionParams) không được để trống")
        Map<String, Object> conditionParams,

        @NotNull(message = "Loại hành động (actionType) không được để trống")
        ScoringRuleActionType actionType,

        // Ví dụ: actionType=CAP_FINAL_SCORE -> {"maxScore": 0}
        @NotNull(message = "Tham số hành động (actionParams) không được để trống")
        Map<String, Object> actionParams,

        @Min(value = 1, message = "Độ ưu tiên (priority) phải lớn hơn 0")
        int priority,

        @NotNull(message = "Mức độ nghiêm trọng (severity) không được để trống")
        ScoringRuleSeverity severity,

        // true = nếu rule này khớp thì dừng luôn, không xét tiếp các rule có priority thấp hơn
        boolean stopProcessing,

        // false = tạo ra nhưng chưa bật, không áp dụng khi chấm điểm
        boolean isActive
) {
}