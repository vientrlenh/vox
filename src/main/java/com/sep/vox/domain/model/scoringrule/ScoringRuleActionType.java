package com.sep.vox.domain.model.scoringrule;

import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFinalScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CriterionScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.FeedbackTagParams;
import com.sep.vox.domain.valueobject.scoringruleaction.FinalScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.InvalidResponseParams;
import com.sep.vox.domain.valueobject.scoringruleaction.RequireHumanReviewParams;
import com.sep.vox.domain.valueobject.scoringruleaction.RequireRetakeParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ReviewReasonParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.SetFrameworkResultBandParams;

public enum ScoringRuleActionType {
    CAP_FINAL_SCORE(CapFinalScoreParams.class),
    CAP_CRITERION_SCORE(CapCriterionScoreParams.class),
    ADD_FINAL_SCORE_DELTA(FinalScoreDeltaParams.class),
    ADD_CRITERION_SCORE_DELTA(CriterionScoreDeltaParams.class),

    CAP_FRAMEWORK_RESULT_BAND(CapFrameworkResultBandParams.class),
    SET_FRAMEWORK_RESULT_BAND(SetFrameworkResultBandParams.class), 

    REQUIRE_HUMAN_REVIEW(RequireHumanReviewParams.class),
    MARK_RESPONSE_INVALID(InvalidResponseParams.class),
    REQUIRE_RETAKE(RequireRetakeParams.class),

    ADD_FEEDBACK_TAG(FeedbackTagParams.class),
    ADD_REVIEW_REASON(ReviewReasonParams.class), 
    ;

    private final Class<? extends ScoringRuleActionParams> paramType;
    ScoringRuleActionType(Class<? extends ScoringRuleActionParams> p) {
        this.paramType = p;
    }
    public Class<? extends ScoringRuleActionParams> paramType() {
        return paramType;
    }
}
