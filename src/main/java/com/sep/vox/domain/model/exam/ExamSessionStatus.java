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
     * mà là điểm dừng.
     *
     * <p>KHÔNG bị Hibernate tự lọc -- {@code ExamSessionJpaEntity} cố tình không dùng
     * {@code @SQLRestriction} (xem chú thích ở đó), vì quản trị trường/chủ tịch hội đồng vẫn phải
     * đọc được phiên đã xoá để giải trình. Nghĩa là {@code findByCandidateId} và mọi truy vấn khác
     * trên bảng này VẪN TRẢ VỀ dòng DELETED -- người viết truy vấn/luồng nghiệp vụ mới phải tự loại
     * nó ra khi cần, không có tấm lưới nào ở tầng ORM đỡ hộ.
     *
     * <p>Từng bị hiểu nhầm là có {@code @SQLRestriction} (chú thích cũ ở đây nói vậy, sai với thực
     * tế) -- {@code UpdateExamStatusUseCase.requirePublishReadiness} là nạn nhân thật của hiểu nhầm
     * đó: thiếu bộ lọc DELETED khiến một học sinh thi hỏng, bị xoá phiên, thi lại là chặn công bố
     * kết quả của CẢ KỲ THI vĩnh viễn.
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
