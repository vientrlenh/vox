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

import com.sep.vox.application.port.input.query.ViewPlatformOperationalHealthQuery;
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

        // Khoảng rỗng hoặc ngược: ảnh chụp hiện tại vẫn đúng nên cứ trả, còn chuỗi ngày thì rỗng --
        // một khoảng rỗng thì thật sự không có ngày nào, và đó là câu trả lời trung thực hơn là 0%.
        if (!from.isBefore(to)) {
            return new PlatformOperationalHealthResponse(
                live.sessionsInProgress(), live.assessmentsInProgress(), live.gradingQueueDepth(),
                0L, 0L, null, List.of());
        }

        // Ngày cuối lấy theo khoảnh khắc CUỐI CÙNG còn nằm trong khoảng, vì `to` là mốc mở: khi
        // `to` rơi đúng 00:00 giờ VN thì ngày của chính nó không có phút nào thuộc cửa sổ.
        LocalDate lastDay = to.minusMillis(1).atZone(zone).toLocalDate();
        LocalDate firstDay = from.atZone(zone).toLocalDate();
        Instant queryFrom = from;
        if (ChronoUnit.DAYS.between(firstDay, lastDay) >= MAX_WINDOW_DAYS) {
            firstDay = lastDay.minusDays(MAX_WINDOW_DAYS - 1L);
            queryFrom = firstDay.atStartOfDay(zone).toInstant();
        }

        Map<LocalDate, GradingOutcomeBucketDto> byDay = platformOperationalHealthQueryRepository.findGradingOutcomeByDay(queryFrom, to, zone).stream()
            .collect(Collectors.toMap(dto -> dto.day(), Function.identity()));

        List<GradingOutcomeBucketResponse> daily = new ArrayList<GradingOutcomeBucketResponse>();
        long graded = 0L;
        long failed = 0L;
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            GradingOutcomeBucketDto bucket = byDay.get(day);
            long dayGraded = bucket == null ? 0L : bucket.graded();
            long dayFailed = bucket == null ? 0L : bucket.failed();
            graded += dayGraded;
            failed += dayFailed;
            daily.add(new GradingOutcomeBucketResponse(day, dayGraded, dayFailed));
        }

        return new PlatformOperationalHealthResponse(
            live.sessionsInProgress(), live.assessmentsInProgress(), live.gradingQueueDepth(),
            graded, failed, successRatePercent(graded, failed), List.copyOf(daily));
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
