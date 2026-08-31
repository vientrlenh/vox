package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Đọc phiên thi AI chấm lỗi, XUYÊN kỳ thi và XUYÊN trường.
 *
 * <p>Không có đường nào sẵn để hỏi câu này: màn kết quả hiện tại đi từ MỘT kỳ thi
 * ({@code ExamResultsListPage} đọc {@code examId} trên URL rồi lọc phía client), nên nó không trả
 * lời được "toàn hệ thống đang có bao nhiêu phiên hỏng, vì những nguyên nhân nào".
 *
 * <p>Là một BÁO CÁO, cùng khuôn với {@link PlatformOperationalHealthQueryRepository} — gộp ở đây
 * thay vì thêm hàng loạt {@code findBy...} vào {@code ExamSessionRepository}, vốn là cổng nghiệp vụ
 * của phiên thi chứ không phải chỗ phục vụ một cái bảng.
 *
 * <p>MỌI hàm ở đây dùng CHUNG một vị từ: {@code status = 'GRADING_FAILED'} và {@code submitted_at}
 * trong khoảng nửa mở {@code [from, to)} — đúng vị từ mà
 * {@link PlatformOperationalHealthQueryRepository#findGradingOutcomeByDay} đếm cho thẻ trên trang
 * tổng quan. Lệch một chi tiết là hai màn hình cạnh nhau nói hai con số khác nhau.
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
