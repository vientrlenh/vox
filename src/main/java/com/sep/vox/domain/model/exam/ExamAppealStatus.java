package com.sep.vox.domain.model.exam;

/**
 * PENDING -&gt; APPROVED -&gt; GRADING -&gt; PUBLISHED
 * PENDING -&gt; REJECTED
 * PENDING -&gt; WITHDRAWN (học sinh tự rút, hoàn lại lượt phúc khảo)
 *
 * <p>{@code COMPARING} đã bị bỏ cùng với bước admin công bố: chỉ còn MỘT người chấm
 * phúc khảo nên không có gì để "đối chiếu" giữa các báo cáo, và người đủ thông tin
 * để quyết chính là người vừa chấm. Giáo viên nộp điểm là điểm lên luôn.
 *
 * <p>{@code WITHDRAWN} là trạng thái riêng chứ không phải một cờ {@code withdrawn_at}
 * trên đơn còn PENDING: đơn đã rút phải ngừng chặn đơn mới, mà mọi chỗ kiểm "còn đơn
 * đang mở" đều đọc {@code status}.
 *
 * <p>⚠️ CHECK constraint trên cột {@code status} phải sửa bằng SQL tay —
 * {@code ddl-auto} không alter được. Xem
 * {@code AI/doc/migration/2026-07-26-grading-rework.sql}.
 */
public enum ExamAppealStatus {
    PENDING,    // học sinh đã gửi đơn, chờ duyệt
    APPROVED,   // school admin đã duyệt, chờ phân công người chấm
    GRADING,    // đã phân công, giáo viên đang chấm lại
    PUBLISHED,  // giáo viên đã nộp điểm -> công bố luôn
    REJECTED,   // admin từ chối ở bước duyệt
    WITHDRAWN   // học sinh tự rút khi đơn còn PENDING
}
