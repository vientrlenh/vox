package com.sep.vox.domain.model.assessment;

public enum AssessmentAppealStatus {
    PENDING,    // học sinh đã gửi đơn, chờ duyệt
    APPROVED,   // school admin đã duyệt, chờ phân công người chấm
    GRADING,    // đã phân công, giáo viên đang chấm lại
    PUBLISHED,  // giáo viên đã nộp điểm -> công bố luôn
    REJECTED,   // admin từ chối ở bước duyệt
    WITHDRAWN   // học sinh tự rút khi đơn còn PENDING
}
