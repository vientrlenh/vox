package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewGradingFailureOverviewQuery;
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
@RequiredArgsConstructor
public class ViewGradingFailureOverviewUseCase
        implements IUseCase<ViewGradingFailureOverviewQuery, GradingFailureOverviewResponse> {

    /**
     * Maximum number of groups returned. The premise of the whole page is "one incident = one group"; if the grading service produces
     * messages too varied to be normalized into groups, the group count grows close to the session count. The cap keeps
     * the page from pulling back thousands of rows, and the truncated part is reported so that failure stays visible.
     */
    static final int MAX_GROUPS = 50;

    private final GradingFailureQueryRepository gradingFailureQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public GradingFailureOverviewResponse execute(ViewGradingFailureOverviewQuery input) {
        GradingFailureWindow window = GradingFailureWindow.resolve(input == null ? null : input.dateFrom(),
            input == null ? null : input.dateTo());

        if (window.isEmpty()) {
            return new GradingFailureOverviewResponse(0L, 0L, 0L, 0L, List.of(), 0L);
        }

        GradingFailureTotalsDto totals = gradingFailureQueryRepository.countTotals(window.from(), window.to());
        List<GradingFailureGroupDto> groups = gradingFailureQueryRepository.findGroups(window.from(), window.to(), MAX_GROUPS);

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
            ZoneId zone = ZoneConstant.BUSINESS_ZONE;
            Instant to = dateTo == null ? Instant.now() : dateTo;
            Instant from = dateFrom == null
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
