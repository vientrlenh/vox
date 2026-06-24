package com.sep.vox.domain.model.exam;

public enum ExamSessionStatus {
    IN_PROGRESS, 
    SUBMITTED, 
    GRADING, 
    GRADED, 
    EXPIRED, // hết thời gian nộp 
    GRADING_FAILED // AI chấm lỗi
}
