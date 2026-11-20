package com.sep.vox.domain.model.assessment;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AssessmentSessionStatus {
    IN_PROGRESS,
    SUBMITTED,
    INTERRUPTED,
    GRADING,
    GRADED,
    EXPIRED, // hết thời gian nộp
    GRADING_FAILED, // AI chấm lỗi
    /**
     * Soft-deleted (see {@code DeleteAssessmentSessionUseCase}). Not a step in the lifecycle of an assessment attempt
     * but a stopping point.
     *
     * <p>NOT filtered automatically by Hibernate -- {@code AssessmentSessionJpaEntity} deliberately does not use
     * {@code @SQLRestriction} (see the note there), because the school administrator/council chair must still
     * read a deleted session to account for it. That means {@code findByCandidateId} and every other query
     * on this table STILL RETURN DELETED rows -- whoever writes a new query/business flow has to exclude
     * it themself when needed, and there is no safety net at the ORM layer to do it for them.
     *
     * <p>It was once wrongly believed to have {@code @SQLRestriction} (the old note here said so, which was wrong in
     * reality) -- {@code UpdateAssessmentStatusUseCase.requirePublishReadiness} was the real victim of that
     * misunderstanding: without a DELETED filter, a student who failed the attempt, had the session deleted and retook it would block the publishing of
     * the results of the WHOLE ASSESSMENT permanently.
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
    public static final Set<AssessmentSessionStatus> RESUMABLE =
        Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, INTERRUPTED));
}
