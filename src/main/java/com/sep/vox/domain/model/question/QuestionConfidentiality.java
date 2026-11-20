package com.sep.vox.domain.model.question;

public enum QuestionConfidentiality {
    OPEN, // rào bởi sharing + collaborators
    EXAM_RESTRICTED, // chỉ có thể access bởi exam member
    RELEASED // từ EXAM_RESTRICTED đổi sang
}
