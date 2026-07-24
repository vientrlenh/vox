package com.sep.vox.domain.model.exam;

/**
 * Vòng đời một phân công chấm bài. Cố ý chỉ có hai trạng thái: MVP nộp cả bài
 * trong một lần nên không tồn tại dữ liệu "đang chấm x/n" để IN_PROGRESS mô tả.
 * Gỡ phân công là xoá dòng, không phải một trạng thái.
 */
public enum GradingAssignmentStatus {
    ASSIGNED,
    COMPLETED
}
