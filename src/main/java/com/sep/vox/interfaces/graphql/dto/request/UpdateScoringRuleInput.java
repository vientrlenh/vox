package com.sep.vox.interfaces.graphql.dto.request;

// Input GraphQL để cập nhật Scoring Rule. Không có field "code" vì code là bất biến.
// conditionParamsJson/actionParamsJson là chuỗi JSON thô (schema không có scalar Map/JSON),
// cấu trúc bắt buộc phải khớp với conditionType/actionType đã chọn - vd:
//   conditionType = "DURATION_LESS_THAN"  -> conditionParamsJson = "{\"seconds\": 3}"
//   actionType     = "CAP_FINAL_SCORE"    -> actionParamsJson    = "{\"maxScore\": 0}"
public record UpdateScoringRuleInput(
        String name,
        String description,
        String conditionType,
        String conditionParamsJson,
        String actionType,
        String actionParamsJson,
        int priority,
        String severity,
        boolean stopProcessing,
        boolean isActive
) {
}