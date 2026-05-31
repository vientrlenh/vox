package com.sep.vox.domain.valueobject.scoringrulecondition;

public sealed interface ScoringRuleConditionParams 
    permits DurationThresholdParams, 
            WordCountThresholdParams, 
            CriterionScoreThresholdParams, 
            CriterionBandThresholdParams, 
            FinalScoreThresholdParams, 
            RatioThresholdParams, 
            ConfidenceThresholdParams, 
            SpeechRateThresholdParams {
    
}
