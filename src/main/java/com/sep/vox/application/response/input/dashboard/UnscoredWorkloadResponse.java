package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.domain.common.BusinessDays;

/**
 * Bài đã thi xong mà học sinh chưa có điểm, chia theo thứ đang chặn.
 *
 * <p>Năm nhóm loại trừ nhau và cộng lại đúng bằng {@code total} — xem
 * {@link SchoolUnscoredWorkloadDto} cho định nghĩa từng nhóm.
 *
 * <p>{@code aiFailed} là tổng của hai nhóm AI, trả sẵn vì giao diện gộp chúng thành MỘT dòng còn hai
 * số con chỉ là nhãn phụ trên dòng đó. Để client tự cộng thì con số trên thẻ và con số sau khi bấm
 * vào có thể lệch nhau nếu một bên quên một nhóm.
 */
public record UnscoredWorkloadResponse(
    int total,
    int aiFailed,
    int aiFailedRetryLeft,
    int aiFailedNoRetryLeft,
    int awaitingAssignment,
    int assignedOverdue,
    int assignedInProgress,
    /** Bài chờ lâu nhất đã chờ bao nhiêu ngày lịch; null khi không còn bài nào. */
    Integer oldestWaitingDays,
    int examCount
) {

    public static UnscoredWorkloadResponse of(SchoolUnscoredWorkloadDto dto, Instant now) {
        return new UnscoredWorkloadResponse(
            dto.total(),
            dto.aiFailed(),
            dto.aiFailedRetryLeft(),
            dto.aiFailedNoRetryLeft(),
            dto.awaitingAssignment(),
            dto.assignedOverdue(),
            dto.assignedInProgress(),
            BusinessDays.waitedDaysSince(dto.oldestSubmittedAt(), now),
            dto.examCount()
        );
    }
}
