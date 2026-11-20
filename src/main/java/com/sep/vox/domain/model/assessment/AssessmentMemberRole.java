package com.sep.vox.domain.model.assessment;

public enum AssessmentMemberRole {
    CHAIR, // người quyết định (approve/lock đề, schedule, release, publish kết quả)
    AUTHOR, // đóng góp câu hỏi
    REVIEWER // duyệt câu hỏi
}
