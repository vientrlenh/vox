package com.sep.vox.application.response.input.exam;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.Exam;

/**
 * Vé vào thi. Ứng dụng thi bám hoàn toàn vào đây cho cả vòng đời phiên thi, nên vé phải nói đủ để
 * client biết bước tiếp theo là gì.
 *
 * <p>{@code requiredStreamType} bỏ trống nghĩa là bài KHÔNG giám sát bằng stream: client phải bỏ
 * qua hẳn bước xin token ở {@code /streams/student/token}. Endpoint đó trả 400 cho bài không cấu
 * hình stream, nên nếu vé không nói ra thì client cứ gọi và học sinh không vào thi được, dù mọi
 * điều kiện dự thi đều đã đạt.
 */
public record ExamEntryTicketResponse(
    UUID attemptId,
    String ticketId,
    String expiresAt,
    String scheduleEndAt,
    String requiredStreamType,
    String streamTypePermission
) {

    public static ExamEntryTicketResponse of(
            UUID attemptId,
            String ticketId,
            String expiresAt,
            Instant scheduleEndAt,
            Exam exam) {
        return new ExamEntryTicketResponse(
            attemptId,
            ticketId,
            expiresAt,
            scheduleEndAt == null ? null : scheduleEndAt.toString(),
            exam.getRequiredStreamType() == null ? null : exam.getRequiredStreamType().name(),
            exam.getStreamTypePermission() == null ? null : exam.getStreamTypePermission().name()
        );
    }
}
