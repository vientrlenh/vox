package com.sep.vox.domain.mapper;

import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

/**
 * Chuyển đổi qua lại giữa Map<String,Object> (dữ liệu JSON thô nhận từ REST request/trả về response)
 * và các Value Object cụ thể (record) của conditionParams/actionParams.
 *
 * Vì mỗi ScoringRuleConditionType/ScoringRuleActionType tương ứng với đúng 1 lớp Params riêng
 * (khai báo sẵn trong enum qua paramTypes()/paramType()), nên chỉ cần biết "loại" (Type) đã chọn
 * là suy ra được lớp cần convert sang, không cần Client phải biết trước cấu trúc Java.
 */
public final class ScoringRuleParamsMapper {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private ScoringRuleParamsMapper() {
    }

    // Map JSON thô -> Value Object điều kiện cụ thể (vd DURATION_LESS_THAN -> DurationThresholdParams)
    public static ScoringRuleConditionParams toConditionParams(ScoringRuleConditionType conditionType, Map<String, Object> raw) {
        return convert(raw, conditionType.paramTypes(), "điều kiện (conditionParams)");
    }

    // Map JSON thô -> Value Object hành động cụ thể (vd CAP_FINAL_SCORE -> CapFinalScoreParams)
    public static ScoringRuleActionParams toActionParams(ScoringRuleActionType actionType, Map<String, Object> raw) {
        return convert(raw, actionType.paramType(), "hành động (actionParams)");
    }

    // Value Object (bất kỳ) -> Map JSON thô, dùng khi trả dữ liệu ra ngoài response (DTO)
    public static Map<String, Object> toMap(Object params) {
        if (params == null) {
            return Map.of();
        }
        return JSON_MAPPER.convertValue(params, new TypeReference<Map<String, Object>>() {
        });
    }

    // Map JSON thô -> chuỗi JSON (dùng cho GraphQL, vì schema không có scalar Map/JSON nên trả về String
    // giống hệt cách "examplesJson" của Rubric đang làm - FE tự JSON.parse() chuỗi này)
    public static String toJsonString(Map<String, Object> raw) {
        if (raw == null) {
            return "{}";
        }
        return JSON_MAPPER.writeValueAsString(raw);
    }

    // Chiều ngược lại: chuỗi JSON (GraphQL input gửi lên) -> Map thô, để tái dùng chung với
    // toConditionParams()/toActionParams() phía trên
    public static Map<String, Object> fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Chuỗi JSON gửi lên không hợp lệ: " + e.getMessage());
        }
    }

    private static <T> T convert(Map<String, Object> raw, Class<T> targetType, String fieldLabel) {
        try {
            return JSON_MAPPER.convertValue(raw, targetType);
        } catch (Exception e) {
            // Bọc lại lỗi convert của Jackson thành lỗi nghiệp vụ dễ hiểu, tránh lộ stacktrace kỹ thuật ra Client
            throw new IllegalArgumentException(
                    "Tham số " + fieldLabel + " không hợp lệ với loại đã chọn: " + e.getMessage());
        }
    }
}