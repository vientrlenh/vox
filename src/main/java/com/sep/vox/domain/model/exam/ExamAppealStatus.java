package com.sep.vox.domain.model.exam;

public enum ExamAppealStatus {
    PENDING, 
    AUTO_REGRADING, 
    RESOLVED_NO_CHANGE, // dành cho 2 bậc chấm lại (máy)
    RESOLVED_CHANGED, 
    ESCALATED, // dành cho 2 bậc chấm lại (người)
    HUMAN_RESOLVED, 
    REJECTED
}
