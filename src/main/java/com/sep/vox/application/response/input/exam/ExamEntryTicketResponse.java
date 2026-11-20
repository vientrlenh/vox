package com.sep.vox.application.response.input.exam;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;

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
    String streamTypePermission,
    String chosenStreamType
) {

    /**
     * @param chosenStreamType loại stream phiên thi ĐÃ chốt, null khi chưa phát token lần nào.
     *
     * <p>Chỉ có ý nghĩa với kỳ thi cho học viên tự chọn ({@code streamTypePermission = ANY}), và ở
     * đó nó là bắt buộc chứ không phải tiện nghi: {@code IssueStudentStreamTokenUseCase} chốt lựa
     * chọn ở lần phát token ĐẦU TIÊN rồi từ chối mọi loại khác bằng 403. Một phiên thi bị gián
     * đoạn và vào lại sẽ đi qua màn chọn lần thứ hai -- không có trường này thì client không có
     * cách nào biết đã chốt gì, nên nó hiện lại đầy đủ lựa chọn và học viên ăn 403 sau khi đã ngồi
     * qua cả bước kiểm tra thiết bị.
     */
    public static ExamEntryTicketResponse of(
            UUID attemptId,
            String ticketId,
            String expiresAt,
            Instant scheduleEndAt,
            Exam exam,
            ExamRequiredStreamType chosenStreamType) {
        return new ExamEntryTicketResponse(
            attemptId,
            ticketId,
            expiresAt,
            scheduleEndAt == null ? null : scheduleEndAt.toString(),
            exam.getRequiredStreamType() == null ? null : exam.getRequiredStreamType().name(),
            exam.getStreamTypePermission() == null ? null : exam.getStreamTypePermission().name(),
            chosenStreamType == null ? null : chosenStreamType.name()
        );
    }
}
