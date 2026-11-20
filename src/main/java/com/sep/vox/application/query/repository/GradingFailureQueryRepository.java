package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Reads the AI-failed assessment sessions, ACROSS assessments and ACROSS schools.
 *
 * <p>There is no existing path to ask this question: the current results screen starts from ONE assessment
 * ({@code AssessmentResultsListPage} reads {@code assessmentId} from the URL and then filters on the client), so it cannot
 * answer "how many sessions are broken across the whole system, and for what causes".
 *
 * <p>It is a REPORT, in the same mold as {@link PlatformOperationalHealthQueryRepository} -- gathered here
 * instead of adding a pile of {@code findBy...} to {@code AssessmentSessionRepository}, which is the business gateway
 * of the assessment session and not a place to serve a table.
 *
 * <p>EVERY function here uses ONE SHARED predicate: {@code status = 'GRADING_FAILED'} and {@code submitted_at}
 * in the half-open range {@code [from, to)} -- exactly the predicate that
 * {@link PlatformOperationalHealthQueryRepository#findGradingOutcomeByDay} counts for the card on the
 * overview page. Differ in one detail and two screens side by side show two different numbers.
 */
public interface GradingFailureQueryRepository {

    GradingFailureTotalsDto countTotals(Instant from, Instant to);

    /**
     * Nhóm theo chữ ký lỗi, nhóm đông nhất trước.
     *
     * @param limit trần số nhóm trả về. Có trần vì tiền đề "một sự cố = một nhóm" phụ thuộc vào việc
     *              chuẩn hóa thông điệp có ăn hay không; nếu dịch vụ chấm sinh thông điệp quá đa
     *              dạng thì số nhóm có thể nở ra bằng số phiên, và trang không được phép kéo cả
     *              nghìn dòng về chỉ để hiện mười dòng đầu.
     */
    List<GradingFailureGroupDto> findGroups(Instant from, Instant to, int limit);

    /**
     * Phiên trong MỘT nhóm.
     *
     * @param signature chữ ký của nhóm; {@code null} chọn đúng nhóm "không rõ nguyên nhân" chứ không
     *                  phải "bỏ lọc" — so bằng {@code IS NOT DISTINCT FROM} để null khớp null
     */
    PageResult<GradingFailureSessionDto> findSessions(
        Instant from, Instant to, String signature, int page, int size);
}
