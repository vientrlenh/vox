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
 * The triage page for failed grading sessions -- the landing place of the "AI grading failed sessions" box on the
 * overview page.
 *
 * <p>The default window is taken EXACTLY from {@link ViewPlatformOperationalHealthUseCase}: a user clicks a
 * number and expects to see precisely the sessions that produced it. If the two defaults differ, the very first
 * time they are opened the two screens show different numbers and nobody knows which is right.
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
     * The reading window, shared by the summary page and the session list.
     *
     * <p>Split out because the two use cases must resolve the range IDENTICALLY: a user clicks a group card showing
     * 1,147 sessions and expects the list that follows to hold 1,147 sessions too. Two copies of the same default
     * are two places to drift apart.
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
