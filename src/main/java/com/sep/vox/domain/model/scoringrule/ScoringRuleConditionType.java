package com.sep.vox.domain.model.scoringrule;

import com.sep.vox.domain.valueobject.scoringrulecondition.ConfidenceThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.DurationThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.FinalScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.RatioThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.SpeechRateThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.WordCountThresholdParams;

public enum ScoringRuleConditionType {
    DURATION_LESS_THAN(DurationThresholdParams.class),
    DURATION_GREATER_THAN(DurationThresholdParams.class),

    WORD_COUNT_LESS_THAN(WordCountThresholdParams.class),
    WORD_COUNT_GREATER_THAN(WordCountThresholdParams.class),

    CRITERION_SCORE_LESS_THAN(CriterionScoreThresholdParams.class),
    CRITERION_SCORE_GREATER_THAN(CriterionScoreThresholdParams.class),

    FINAL_SCORE_LESS_THAN(FinalScoreThresholdParams.class),
    FINAL_SCORE_GREATER_THAN(FinalScoreThresholdParams.class),

    TASK_RELEVANCE_LESS_THAN(RatioThresholdParams.class),
    OFF_TOPIC_RATIO_GREATER_THAN(RatioThresholdParams.class),

    CODE_SWITCHING_RATIO_GREATER_THAN(RatioThresholdParams.class),

    ASR_CONFIDENCE_LESS_THAN(ConfidenceThresholdParams.class),
    AI_CONFIDENCE_LESS_THAN(ConfidenceThresholdParams.class),
    AUDIO_QUALITY_LESS_THAN(ConfidenceThresholdParams.class), 

    SILENCE_RATIO_GREATER_THAN(RatioThresholdParams.class),
    SPEECH_RATE_LESS_THAN(SpeechRateThresholdParams.class), 
    SPEECH_RATE_GREATER_THAN(SpeechRateThresholdParams.class),
    ;

    
    private final Class<? extends ScoringRuleConditionParams> paramType;
    ScoringRuleConditionType(Class<? extends ScoringRuleConditionParams> p) {
        this.paramType = p;
    }
    public Class<? extends ScoringRuleConditionParams> paramTypes() {
        return paramType;
    }
}
