package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Một mục trong bộ lọc kỳ thi của hàng đợi giáo viên.
 *
 * <p>Cố ý mỏng: đây là dropdown, không phải màn kỳ thi. Mỗi field thêm vào đây là một
 * field giáo viên đọc được về kỳ thi tập trung mà họ không phải thành viên.
 */
public record GradingExamOptionInfo(
    UUID id,
    /** Mã kỳ thi — tên trùng nhau giữa các đợt, mã thì không. */
    String code,
    String name,
    /** Tổng số phân công của người gọi ở kỳ thi này, mọi vòng, mọi trạng thái. */
    long taskCount,
    /** Trong đó còn ASSIGNED — con số cho badge "còn phải chấm". */
    long openTaskCount
) {
}
