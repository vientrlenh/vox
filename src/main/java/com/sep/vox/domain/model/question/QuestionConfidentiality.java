package com.sep.vox.domain.model.question;

public enum QuestionConfidentiality {
    OPEN, // rào bởi sharing + collaborators
    ASSESSMENT_RESTRICTED, // chỉ có thể access bởi assessment member
    RELEASED // từ ASSESSMENT_RESTRICTED đổi sang
}
