package com.sep.vox.domain.service.recording;

import java.util.Comparator;
import java.util.Map;

import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.model.exam.ExamRecordingAssemblyStatus;

/**
 * Quyết định bản ghi nào được coi là bản chuẩn khi một phiên thi có nhiều bản cùng streamType.
 *
 * <p>Hai đường ingest mint streamID độc lập nên sinh ra hai recording.mp4 ở hai prefix S3 khác
 * nhau. Mỗi nguồn giữ một hàng riêng trong exam_recordings, nên không nguồn nào ghi đè nguồn nào:
 * việc chọn bản nào để mở trước được hoãn tới lúc đọc, và không bản nào bị vứt đi.
 */
public final class RecordingPrecedence {

    private RecordingPrecedence() {
    }

    /**
     * Nguồn nào đáng tin hơn khi một phiên thi có nhiều bản ghi. Rank cao hơn thì được chọn làm
     * bản chuẩn.
     *
     * <p>SERVER_WATCHDOG xếp cuối vì đó là ca duy nhất không ai xác nhận stream kết thúc sạch:
     * nó ghép những gì đã nhận được từ một client đã im lặng, nên độ đầy đủ là không xác định.
     */
    private static final Map<String, Integer> SOURCE_RANK = Map.of(
        "DESKTOP_SEGMENT_UPLOAD", 3,
        "SERVER_WEBRTC", 2,
        "SERVER_WATCHDOG", 1
    );

    /**
     * Nguồn không rõ hoặc null xếp dưới mọi nguồn đã biết. Các hàng ghi trước khi có cột source
     * rơi vào nhóm này: chúng vẫn hiện ra và vẫn mở được, chỉ không được ưu tiên làm bản chuẩn
     * khi đã có một bản biết rõ mình từ đâu.
     */
    public static int rankOf(String source) {
        return source == null ? 0 : SOURCE_RANK.getOrDefault(source, 0);
    }

    /** Trạng thái mà bản ghi thực sự trỏ tới một file mở được. */
    public static boolean hasRecording(ExamRecordingAssemblyStatus status) {
        return status == ExamRecordingAssemblyStatus.READY
            || status == ExamRecordingAssemblyStatus.PARTIAL;
    }

    public static boolean isTerminal(ExamRecordingAssemblyStatus status) {
        return status == ExamRecordingAssemblyStatus.READY
            || status == ExamRecordingAssemblyStatus.PARTIAL
            || status == ExamRecordingAssemblyStatus.FAILED
            || status == ExamRecordingAssemblyStatus.ABANDONED;
    }

    /**
     * Thứ tự chọn bản chuẩn: có file mở được trước, rồi mới tới độ tin cậy của nguồn. Bản lớn
     * nhất theo thứ tự này là bản chuẩn của nhóm.
     *
     * <p>Vế đầu phải đứng trước vế sau: một FAILED từ nguồn ưu tiên cao không được làm bản chuẩn
     * khi vẫn còn một READY từ nguồn thấp hơn. Một cái nhãn hỏng không hơn được một file xem được.
     */
    public static final Comparator<ExamRecording> CANONICAL_ORDER =
        Comparator.<ExamRecording, Boolean>comparing(recording -> hasRecording(recording.getStatus()))
            .thenComparingInt(recording -> rankOf(recording.getSource()));

    /**
     * Có nên giữ nguyên hàng hiện có và bỏ qua event vừa tới hay không.
     *
     * <p>Chỉ còn là luật trong phạm vi một hàng: từ khi source là một phần khoá tra cứu, hai vế
     * luôn thuộc cùng một nguồn, nên ở đây không còn chuyện nguồn này ghi đè nguồn kia. Việc so
     * sánh giữa các nguồn đã chuyển sang {@link #CANONICAL_ORDER} ở đường đọc.
     */
    public static boolean shouldKeepExisting(
        ExamRecordingAssemblyStatus existingStatus,
        ExamRecordingAssemblyStatus incomingStatus
    ) {
        if (!isTerminal(existingStatus)) {
            return false;
        }
        // Không hạ cấp một kết quả cuối về lại "đang xử lý".
        if (incomingStatus == ExamRecordingAssemblyStatus.PROCESSING) {
            return true;
        }
        // Một thất bại không được xoá một bản ghi đang dùng được bằng cái nhãn hỏng của chính nó.
        return hasRecording(existingStatus) && !hasRecording(incomingStatus);
    }
}
