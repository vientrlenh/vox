package com.sep.vox.domain.valueobject.scoringruleaction;

public sealed interface ScoringRuleActionParams 
    permits CapFinalScoreParams, 
            RequireHumanReviewParams, 
            CapCriterionScoreParams, 
            FinalScoreDeltaParams, 
            CriterionScoreDeltaParams, 
            CapFrameworkResultBandParams, 
            SetFrameworkResultBandParams, 
            FeedbackTagParams, 
            InvalidResponseParams, 
            RequireRetakeParams, 
            ReviewReasonParams {
    
}
