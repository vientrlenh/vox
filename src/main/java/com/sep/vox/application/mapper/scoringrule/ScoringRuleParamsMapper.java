package com.sep.vox.application.mapper.scoringrule;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

/**
 * Converts back and forth between Map<String,Object> (raw JSON data received from a REST request or returned in a
 * response) and the concrete Value Objects (records) of conditionParams/actionParams.
 *
 * Because each ScoringRuleConditionType/ScoringRuleActionType maps to exactly one Params class of its own (declared
 * in the enum through paramTypes()/paramType()), knowing the chosen "type" is enough to infer the class to convert
 * to, so the client does not need to know the Java structure in advance.
 */
@Service
public class ScoringRuleParamsMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public ScoringRuleParamsMapper(JsonSerializationPort jsonSerializationPort) {
        this.jsonSerializationPort = jsonSerializationPort;
    }

    // Raw JSON map -> concrete condition Value Object (e.g. DURATION_LESS_THAN -> DurationThresholdParams)
    public ScoringRuleConditionParams toConditionParams(ScoringRuleConditionType conditionType, Map<String, Object> raw) {
        return convert(raw, conditionType.paramTypes(), "điều kiện (conditionParams)");
    }

    // Raw JSON map -> concrete action Value Object (e.g. CAP_FINAL_SCORE -> CapFinalScoreParams)
    public ScoringRuleActionParams toActionParams(ScoringRuleActionType actionType, Map<String, Object> raw) {
        return convert(raw, actionType.paramType(), "hành động (actionParams)");
    }

    // Any Value Object -> raw JSON map, used when returning data to the outside in a response (DTO)
    public Map<String, Object> toMap(Object params) {
        if (params == null) {
            return Map.of();
        }
        return jsonSerializationPort.convertToObjectMap(params);
    }

    // Raw JSON map -> JSON string (used by GraphQL: the schema has no Map/JSON scalar, so it returns a String the
    // same way Rubric's "examplesJson" does -- the frontend runs JSON.parse() on this string)
    public String toJsonString(Map<String, Object> raw) {
        if (raw == null) {
            return "{}";
        }
        return jsonSerializationPort.toJson(raw);
    }

    // The reverse direction: JSON string (sent as GraphQL input) -> raw map, so it can share
    // toConditionParams()/toActionParams() above
    public Map<String, Object> fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonSerializationPort.toObjectMap(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chuỗi JSON gửi lên không hợp lệ: " + e.getMessage());
        }
    }

    private <T> T convert(Map<String, Object> raw, Class<T> targetType, String fieldLabel) {
        try {
            return jsonSerializationPort.convertValue(raw, targetType);
        } catch (Exception e) {
            // Wrap the Jackson conversion error into an easy-to-read business error, so the technical stack trace
            // does not leak to the client
            throw new IllegalArgumentException(
                    "Tham số " + fieldLabel + " không hợp lệ với loại đã chọn: " + e.getMessage());
        }
    }
}
