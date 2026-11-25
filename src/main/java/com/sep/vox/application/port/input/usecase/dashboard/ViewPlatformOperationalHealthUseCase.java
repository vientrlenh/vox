package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewPlatformOperationalHealthQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;
import com.sep.vox.application.response.input.dashboard.GradingOutcomeBucketResponse;
import com.sep.vox.application.response.input.dashboard.PlatformOperationalHealthResponse;
import com.sep.vox.domain.common.ZoneConstant;

/**
 * Sức khỏe đường chấm AI + kỳ thi đang chạy, cho dashboard system admin.
 *
 * <p>Chuỗi ngày được làm ĐẦY ở đây chứ không ở DB: query chỉ trả về ngày có phiên, còn biểu đồ cần
 * mọi ngày trong cửa sổ — thiếu ngày thì các cột bị dồn lại và một ngày chết hoàn toàn (0 phiên) sẽ
 * biến mất thay vì hiện thành khoảng trống, tức là đúng ngày đáng chú ý nhất lại vô hình.
 */
@Service
@RequiredArgsConstructor
public class ViewPlatformOperationalHealthUseCase
        implements IUseCase<ViewPlatformOperationalHealthQuery, PlatformOperationalHealthResponse> {

    /** Default window when the client sends no bounds. */
    static final int DEFAULT_WINDOW_DAYS = 14;

    /**
     * Maximum number of days drawn. It is not there to protect the DB (the query already aggregates), but so that an accidentally
     * wide range — a client sending an EPOCH bound by mistake, for example — does not produce thousands of elements the chart cannot
     * draw. The start bound is moved up with it, so the total and the day series always describe the same window.
     */
    static final int MAX_WINDOW_DAYS = 366;

    private final PlatformOperationalHealthQueryRepository platformOperationalHealthQueryRepository;


    @Override
    @Transactional(readOnly = true)
    public PlatformOperationalHealthResponse execute(ViewPlatformOperationalHealthQuery input) {
        ZoneId zone = ZoneConstant.BUSINESS_ZONE;
        LiveSessionCountsDto live = platformOperationalHealthQueryRepository.countLiveSessions();

        Instant to = input == null || input.dateTo() == null ? Instant.now() : input.dateTo();
        Instant from = input == null || input.dateFrom() == null
            ? to.atZone(zone).toLocalDate().minusDays(DEFAULT_WINDOW_DAYS - 1L).atStartOfDay(zone).toInstant()
            : input.dateFrom();

        if (!from.isBefore(to)) {
            return snapshotWithoutDays(live);
        }

        GradingWindow window = clampWindow(from, to, zone);
        Map<LocalDate, GradingOutcomeBucketDto> byDay = platformOperationalHealthQueryRepository.findGradingOutcomeByDay(window.queryFrom(), to, zone).stream()
            .collect(Collectors.toMap(dto -> dto.day(), Function.identity()));

        DailyOutcomes outcomes = fillDailyOutcomes(byDay, window);

        return new PlatformOperationalHealthResponse(
            live.sessionsInProgress(), live.assessmentsInProgress(), live.gradingQueueDepth(),
            outcomes.graded(), outcomes.failed(), successRatePercent(outcomes.graded(), outcomes.failed()),
            List.copyOf(outcomes.daily()));
    }

    // Empty or reversed range: the current snapshot is still right so it is returned, while the day series is
    // empty -- an empty range really has no days, and that is a more honest answer than 0%.
    private PlatformOperationalHealthResponse snapshotWithoutDays(LiveSessionCountsDto live) {
        return new PlatformOperationalHealthResponse(
            live.sessionsInProgress(), live.assessmentsInProgress(), live.gradingQueueDepth(),
            0L, 0L, null, List.of());
    }

    private GradingWindow clampWindow(Instant from, Instant to, ZoneId zone) {
        // The last day is taken from the LAST instant still inside the range, because `to` is an open bound: when
        // `to` falls exactly on 00:00 Vietnam time, its own day has no minute inside the window.
        LocalDate lastDay = to.minusMillis(1).atZone(zone).toLocalDate();
        LocalDate firstDay = from.atZone(zone).toLocalDate();
        Instant queryFrom = from;
        if (ChronoUnit.DAYS.between(firstDay, lastDay) >= MAX_WINDOW_DAYS) {
            firstDay = lastDay.minusDays(MAX_WINDOW_DAYS - 1L);
            queryFrom = firstDay.atStartOfDay(zone).toInstant();
        }
        return new GradingWindow(firstDay, lastDay, queryFrom);
    }

    private DailyOutcomes fillDailyOutcomes(Map<LocalDate, GradingOutcomeBucketDto> byDay, GradingWindow window) {
        List<GradingOutcomeBucketResponse> daily = new ArrayList<GradingOutcomeBucketResponse>();
        long graded = 0L;
        long failed = 0L;
        for (LocalDate day = window.firstDay(); !day.isAfter(window.lastDay()); day = day.plusDays(1)) {
            GradingOutcomeBucketDto bucket = byDay.get(day);
            long dayGraded = bucket == null ? 0L : bucket.graded();
            long dayFailed = bucket == null ? 0L : bucket.failed();
            graded += dayGraded;
            failed += dayFailed;
            daily.add(new GradingOutcomeBucketResponse(day, dayGraded, dayFailed));
        }
        return new DailyOutcomes(daily, graded, failed);
    }

    private record GradingWindow(LocalDate firstDay, LocalDate lastDay, Instant queryFrom) {
    }

    private record DailyOutcomes(List<GradingOutcomeBucketResponse> daily, long graded, long failed) {
    }

    /** Làm tròn 1 chữ số thập phân: 98,7% đọc được, còn 98,66666% thì không thêm thông tin gì. */
    private static Double successRatePercent(long graded, long failed) {
        long attempted = graded + failed;
        if (attempted == 0L) {
            return null;
        }
        return Math.round(graded * 1000.0 / attempted) / 10.0;
    }
}
