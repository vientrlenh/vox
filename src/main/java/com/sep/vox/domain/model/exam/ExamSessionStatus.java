package com.sep.vox.domain.model.exam;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ExamSessionStatus {
    IN_PROGRESS,
    SUBMITTED,
    INTERRUPTED,
    GRADING,
    GRADED,
    EXPIRED, // hết thời gian nộp
    GRADING_FAILED, // AI chấm lỗi
    /**
     * Đã xoá mềm (xem {@code DeleteExamSessionUseCase}). Không phải một bước trong vòng đời bài thi
     * mà là điểm dừng: phiên ở trạng thái này bị ẩn khỏi mọi luồng đọc bằng
     * {@code @SQLRestriction("deleted_at IS NULL")} trên {@code ExamSessionJpaEntity}, nên trên thực
     * tế không luồng nghiệp vụ nào còn nhìn thấy nó để mà chuyển trạng thái tiếp.
     */
    DELETED;

    /**
     * Các trạng thái mà một phiên thi vẫn còn được coi là đang dùng được: học viên có thể vào lại,
     * và hệ thống vẫn phải nhận dữ liệu giám sát của phiên đó.
     *
     * <p>Định nghĩa nằm ở đây thay vì viết {@code EnumSet.of(...)} tại từng nơi dùng, vì trước đây
     * hai chỗ đã lệch nhau: luồng vào thi coi INTERRUPTED là vào lại được, còn luồng phát token
     * stream chỉ chấp nhận IN_PROGRESS - nên đúng tình huống cần nhất (máy học viên mất kết nối rồi
     * quay lại với các đoạn ghi đang nằm trong buffer) lại là tình huống không xin được token để
     * đẩy dữ liệu lên.
     */
    public static final Set<ExamSessionStatus> RESUMABLE =
        Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, INTERRUPTED));
}
