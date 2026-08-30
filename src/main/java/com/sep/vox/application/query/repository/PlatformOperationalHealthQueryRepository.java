package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;

/**
 * Đọc tình trạng VẬN HÀNH của nền tảng từ bảng {@code exam_sessions}, cho dashboard system admin.
 *
 * <p>Là một BÁO CÁO, không phải cổng ghi của aggregate nào — cùng khuôn với
 * {@link TokenUsageTimeseriesQueryRepository} và {@link QuestionBankStatsQueryRepository}: gộp nhiều
 * phiên lại thành những con số không tương ứng với một thực thể nào và không có đường ghi ngược.
 *
 * <p>Nguồn sự thật là cột {@code exam_sessions.status}, nơi {@code GRADING_FAILED} nghĩa là AI chấm
 * lỗi — xem {@code ExamSessionStatus}. Không suy ra từ {@code exam_item_evaluations}: bảng đó chỉ có
 * bản ghi khi việc chấm đã THÀNH CÔNG, nên đếm ở đó thì mọi lượt chấm lỗi đều vô hình.
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
