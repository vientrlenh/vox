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
    GRADING_FAILED; // AI chấm lỗi

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
