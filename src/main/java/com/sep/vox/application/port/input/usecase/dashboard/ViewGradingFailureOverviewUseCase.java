package com.sep.vox.application.port.input.usecase.dashboard;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewGradingFailureOverviewQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.application.response.input.dashboard.GradingFailureOverviewResponse;
import com.sep.vox.domain.common.ZoneConstant;

/**
 * Trang phân loại phiên chấm lỗi — chỗ đáp của ô "Phiên AI chấm lỗi" trên trang tổng quan.
 *
 * <p>Cửa sổ mặc định LẤY ĐÚNG của {@link ViewPlatformOperationalHealthUseCase}: người dùng bấm vào
 * một con số rồi mong thấy đúng những phiên tạo ra con số đó. Hai mặc định lệch nhau thì ngay lần
 * mở đầu tiên hai màn hình đã nói hai số khác nhau, và không ai biết số nào đúng.
 */
@Service
public class ViewGradingFailureOverviewUseCase
        implements IUseCase<ViewGradingFailureOverviewQuery, GradingFailureOverviewResponse> {

    /**
     * Trần số nhóm trả về. Tiền đề của cả trang là "một sự cố = một nhóm"; nếu dịch vụ chấm sinh
     * thông điệp quá đa dạng để chuẩn hóa gom lại được, số nhóm sẽ nở gần bằng số phiên. Trần giữ
     * cho trang không kéo về hàng nghìn dòng, và phần bị cắt được báo ra để lỗi đó nhìn thấy được.
     */
    static final int MAX_GROUPS = 50;

    private final GradingFailureQueryRepository gradingFailureQueryRepository;

    public ViewGradingFailureOverviewUseCase(GradingFailureQueryRepository gradingFailureQueryRepository) {
        this.gradingFailureQueryRepository = gradingFailureQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingFailureOverviewResponse execute(ViewGradingFailureOverviewQuery input) {
        var window = GradingFailureWindow.resolve(input == null ? null : input.dateFrom(),
            input == null ? null : input.dateTo());

        if (window.isEmpty()) {
            return new GradingFailureOverviewResponse(0L, 0L, 0L, 0L, List.of(), 0L);
        }

        var totals = gradingFailureQueryRepository.countTotals(window.from(), window.to());
        var groups = gradingFailureQueryRepository.findGroups(window.from(), window.to(), MAX_GROUPS);

        return new GradingFailureOverviewResponse(
            totals.sessionCount(),
            totals.causeCount(),
            totals.schoolCount(),
            totals.retryableCount(),
            groups,
            Math.max(0, totals.causeCount() - groups.size())
        );
    }

    /**
     * Cửa sổ đọc, dùng chung cho cả trang tóm tắt lẫn danh sách phiên.
     *
     * <p>Tách ra vì hai use case phải giải khoảng GIỐNG HỆT nhau: người dùng bấm vào một thẻ nhóm
     * đang hiện 1.147 phiên và mong danh sách sau đó cũng có 1.147 phiên. Hai bản sao của cùng phép
     * mặc định là hai chỗ để lệch.
     */
    record GradingFailureWindow(Instant from, Instant to) {

        static GradingFailureWindow resolve(Instant dateFrom, Instant dateTo) {
            var zone = ZoneConstant.BUSINESS_ZONE;
            var to = dateTo == null ? Instant.now() : dateTo;
            var from = dateFrom == null
                ? to.atZone(zone).toLocalDate()
                    .minusDays(ViewPlatformOperationalHealthUseCase.DEFAULT_WINDOW_DAYS - 1L)
                    .atStartOfDay(zone).toInstant()
                : dateFrom;
            return new GradingFailureWindow(from, to);
        }

        boolean isEmpty() {
            return !from.isBefore(to);
        }
    }
}
