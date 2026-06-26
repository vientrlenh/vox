package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

public class ExamItemRuleHit {
    private UUID id;
    private UUID evaluationId;
    private UUID scoringRuleId;
    private String ruleCode; // snapshot của rule
    private ScoringRuleConditionType conditionType;
    private BigDecimal observedValue; // giá trị đạt được (ví dụ 0.7)
    private BigDecimal threshold; // ngưỡng (ví dụ 0.5)
    private ScoringRuleActionType actionType;
    private String effectSummary; // ví dụ điểm từ 8.5 xuống 2.5
    private ScoringRuleSeverity severity;
    private String reasonCode;
    private int appliedOrder;
    private OffsetDateTime createdAt;
    private UUID createdBy;
}
