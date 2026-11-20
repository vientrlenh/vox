package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;

/**
 * Reads the OPERATIONAL health of the platform from the {@code assessment_sessions} table, for the system admin dashboard.
 *
 * <p>It is a REPORT, not the write gateway of any aggregate -- in the same mold as
 * {@link SchoolAiCostQueryRepository} and {@link QuestionBankStatsQueryRepository}: it gathers many
 * sessions into numbers that correspond to no entity and has no write path back.
 *
 * <p>The source of truth is the column {@code assessment_sessions.status}, where {@code GRADING_FAILED} means the AI's grading
 * failed -- see {@code AssessmentSessionStatus}. It is not derived from {@code assessment_item_evaluations}: that table only has
 * a record once grading has SUCCEEDED, so counting there makes every failed grading invisible.
 */
public interface PlatformOperationalHealthQueryRepository {

    /** Ảnh chụp tại thời điểm gọi: phiên đang thi, số kỳ thi có phiên đang chạy, và hàng chờ chấm. */
    LiveSessionCountsDto countLiveSessions();

    /**
     * Số phiên chấm xong / chấm lỗi theo từng ngày lịch, chỉ trả về ngày CÓ dữ liệu — nơi gọi tự
     * chèn ngày trống thành 0.
     *
     * <p>Gộp theo {@code submitted_at} chứ không {@code started_at}: một phiên thi đêm khuya nộp
     * sang ngày hôm sau thì kết quả chấm thuộc về ngày nộp, và đó cũng là mốc duy nhất mà mọi phiên
     * đã chấm chắc chắn có.
     *
     * @param from mốc đầu, BAO GỒM
     * @param to   mốc cuối, KHÔNG bao gồm — hai dải liền nhau sẽ đếm trùng phiên rơi đúng ranh giới
     *             nếu dùng {@code <=}; cùng quy ước với {@code schoolBalanceSummary}
     * @param zone múi giờ dùng để cắt ngày lịch
     */
    List<GradingOutcomeBucketDto> findGradingOutcomeByDay(Instant from, Instant to, ZoneId zone);
}
