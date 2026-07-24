package com.sep.vox.domain.model.exam;

/**
 * PENDING -> APPROVED -> GRADING -> COMPARING -> PUBLISHED
 * PENDING -> REJECTED
 */
public enum ExamAppealStatus {
    PENDING,    // học sinh đã gửi đơn, chờ duyệt
    APPROVED,   // school admin đã duyệt, chờ phân công giám khảo
    GRADING,    // đã phân công, giám khảo đang chấm lại
    COMPARING,  // tất cả giám khảo đã nộp báo cáo, chờ admin đối chiếu
    PUBLISHED,  // admin đã quyết điểm cuối và công bố
    REJECTED    // admin từ chối ở bước duyệt
}
