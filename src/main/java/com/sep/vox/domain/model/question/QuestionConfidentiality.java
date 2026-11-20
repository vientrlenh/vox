package com.sep.vox.domain.model.question;

public enum QuestionConfidentiality {
    OPEN, // gated by sharing + collaborators
    ASSESSMENT_RESTRICTED, // only accessible by assessment members
    RELEASED // changed over from ASSESSMENT_RESTRICTED
}
