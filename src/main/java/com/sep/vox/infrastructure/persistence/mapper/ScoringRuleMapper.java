package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleAppliesTo;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFinalScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapResultBandParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CriterionScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.FeedbackTagParams;
import com.sep.vox.domain.valueobject.scoringruleaction.InvalidResponseParams;
import com.sep.vox.domain.valueobject.scoringruleaction.NoActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.RequireHumanReviewParams;
import com.sep.vox.domain.valueobject.scoringruleaction.RequireRetakeParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ReviewReasonParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.SetResultBandParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ConfidenceThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionBandThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.DurationThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.FinalScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.RatioThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.SpeechRateThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.WordCountThresholdParams;
import com.sep.vox.infrastructure.persistence.entity.ScoringRuleJpaEntity;

public final class ScoringRuleMapper {

    private ScoringRuleMapper() {}

    public static ScoringRule toDomain(ScoringRuleJpaEntity jpa) {
        var conditionType = fromConditionType(jpa.getConditionType());
        var actionType = fromActionType(jpa.getActionType());
        return new ScoringRule(
            jpa.getId(),
            jpa.getPolicyId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            conditionType,
            JsonValueObjectMapper.fromJson(jpa.getConditionParamsJson(), conditionParamsType(conditionType)),
            actionType,
            JsonValueObjectMapper.fromJson(jpa.getActionParamsJson(), actionParamsType(actionType)),
            jpa.getPriority(),
            fromAppliesTo(jpa.getAppliesTo()),
            fromSeverity(jpa.getSeverity()),
            jpa.isStopProcessing(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ScoringRuleJpaEntity toJpa(ScoringRule rule) {
        return new ScoringRuleJpaEntity(
            rule.getId(),
            rule.getPolicyId(),
            rule.getCode(),
            rule.getName(),
            rule.getDescription(),
            valueOf(rule.getConditionType()),
            JsonValueObjectMapper.toJson(rule.getConditionParams()),
            valueOf(rule.getActionType()),
            JsonValueObjectMapper.toJson(rule.getActionParams()),
            rule.getPriority(),
            valueOf(rule.getAppliesTo()),
            valueOf(rule.getSeverity()),
            rule.isStopProcessing(),
            rule.isActive(),
            rule.getCreatedAt(),
            rule.getUpdatedAt(),
            rule.getCreatedBy(),
            rule.getUpdatedBy()
        );
    }

    private static String valueOf(ScoringRuleSeverity severity) {
        return severity == null ? null : severity.name();
    }

    private static String valueOf(ScoringRuleActionType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ScoringRuleAppliesTo to) {
        return to == null ? null : to.name();
    }

    private static String valueOf(ScoringRuleConditionType type) {
        return type == null ? null : type.name();
    }

    private static ScoringRuleConditionType fromConditionType(String value) {
        return value == null ? null : ScoringRuleConditionType.valueOf(value);
    }

    private static ScoringRuleActionType fromActionType(String value) {
        return value == null ? null : ScoringRuleActionType.valueOf(value);
    }

    private static ScoringRuleAppliesTo fromAppliesTo(String value) {
        return value == null ? null : ScoringRuleAppliesTo.valueOf(value);
    }

    private static ScoringRuleSeverity fromSeverity(String value) {
        return value == null ? null : ScoringRuleSeverity.valueOf(value);
    }

    private static Class<? extends ScoringRuleConditionParams> conditionParamsType(ScoringRuleConditionType type) {
        return switch (type) {
            case DURATION_LESS_THAN, DURATION_GREATER_THAN -> DurationThresholdParams.class;
            case WORD_COUNT_LESS_THAN, WORD_COUNT_GREATER_THAN -> WordCountThresholdParams.class;
            case CRITERION_SCORE_LESS_THAN, CRITERION_SCORE_GREATER_THAN -> CriterionScoreThresholdParams.class;
            case CRITERION_BAND_AT_OR_BELOW, CRITERION_BAND_AT_OR_ABOVE -> CriterionBandThresholdParams.class;
            case FINAL_SCORE_LESS_THAN, FINAL_SCORE_GREATER_THAN -> FinalScoreThresholdParams.class;
            case TASK_RELEVANCE_LESS_THAN, OFF_TOPIC_RATIO_GREATER_THAN,
                    CODE_SWITCHING_RATIO_GREATER_THAN, SILENCE_RATIO_GREATER_THAN -> RatioThresholdParams.class;
            case ASR_CONFIDENCE_LESS_THAN, AI_CONFIDENCE_LESS_THAN, AUDIO_QUALITY_LESS_THAN -> ConfidenceThresholdParams.class;
            case SPEECH_RATE_LESS_THAN, SPEECH_RATE_GREATER_THAN -> SpeechRateThresholdParams.class;
        };
    }

    private static Class<? extends ScoringRuleActionParams> actionParamsType(ScoringRuleActionType type) {
        return switch (type) {
            case CAP_FINAL_SCORE -> CapFinalScoreParams.class;
            case CAP_CRITERION_SCORE -> CapCriterionScoreParams.class;
            case ADD_FINAL_SCORE_DELTA -> ScoreDeltaParams.class;
            case ADD_CRITERION_SCORE_DELTA -> CriterionScoreDeltaParams.class;
            case CAP_RESULT_BAND -> CapResultBandParams.class;
            case SET_RESULT_BAND -> SetResultBandParams.class;
            case REQUIRE_HUMAN_REVIEW -> RequireHumanReviewParams.class;
            case MARK_RESPONSE_INVALID -> InvalidResponseParams.class;
            case REQUIRE_RETAKE -> RequireRetakeParams.class;
            case ADD_FEEDBACK_TAG -> FeedbackTagParams.class;
            case ADD_REVIEW_REASON -> ReviewReasonParams.class;
            case STOP_PROCESSING -> NoActionParams.class;
        };
    }
}
