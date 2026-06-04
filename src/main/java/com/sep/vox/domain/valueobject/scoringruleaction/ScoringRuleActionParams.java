package com.sep.vox.domain.valueobject.scoringruleaction;

public sealed interface ScoringRuleActionParams 
    permits CapFinalScoreParams, 
            RequireHumanReviewParams, 
            CapCriterionScoreParams, 
            ScoreDeltaParams, 
            CriterionScoreDeltaParams, 
            CapResultBandParams, 
            SetResultBandParams, 
            FeedbackTagParams, 
            NoActionParams, 
            InvalidResponseParams, 
            RequireRetakeParams, 
            ReviewReasonParams {
    
}
