package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.ViewPlatformOperationalHealthQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;
import com.sep.vox.application.response.input.dashboard.PlatformOperationalHealthResponse;
import com.sep.vox.domain.common.ZoneConstant;

/**
 * Trọng tâm: chuỗi ngày phải ĐẦY. Query chỉ trả về ngày có phiên, nên nếu use case không chèn ngày
 * trống thì một ngày chết hoàn toàn sẽ biến mất khỏi biểu đồ thay vì hiện thành khoảng trũng — đúng
 * ngày đáng chú ý nhất lại vô hình.
 */
class ViewPlatformOperationalHealthUseCaseTests {

    private PlatformOperationalHealthQueryRepository queryRepository;
    private ViewPlatformOperationalHealthUseCase useCase;

    /** 01/08/2026 00:00 giờ VN — mốc cố định để test không phụ thuộc đồng hồ máy chạy. */
    private static final Instant FROM = LocalDate.of(2026, 8, 1)
        .atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();

    /** Mốc MỞ: 05/08 00:00 nên ngày cuối được vẽ là 04/08, không phải 05/08. */
    private static final Instant TO = LocalDate.of(2026, 8, 5)
        .atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();

    @BeforeEach
    void setUp() {
        queryRepository = mock(PlatformOperationalHealthQueryRepository.class);
        when(queryRepository.countLiveSessions()).thenReturn(new LiveSessionCountsDto(6L, 2L, 41L));
        useCase = new ViewPlatformOperationalHealthUseCase(queryRepository);
    }

    @Test
    void fillsMissingDaysWithZeroAndKeepsThemInOrder() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of(
            new GradingOutcomeBucketDto(LocalDate.of(2026, 8, 1), 120L, 1L),
            // 02/08 and 03/08 have no rows -- this is exactly the case where 0 must be inserted.
            new GradingOutcomeBucketDto(LocalDate.of(2026, 8, 4), 80L, 19L)));

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(FROM, TO));

        assertThat(result.daily()).extracting(bucket -> bucket.day())
            .containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4));
        assertThat(result.daily()).extracting(bucket -> bucket.graded())
            .containsExactly(120L, 0L, 0L, 80L);
        assertThat(result.daily()).extracting(bucket -> bucket.failed())
            .containsExactly(1L, 0L, 0L, 19L);
    }

    @Test
    void totalsAndSuccessRateCoverTheWholeWindow() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of(
            new GradingOutcomeBucketDto(LocalDate.of(2026, 8, 1), 120L, 1L),
            new GradingOutcomeBucketDto(LocalDate.of(2026, 8, 4), 80L, 19L)));

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(FROM, TO));

        assertThat(result.graded()).isEqualTo(200L);
        assertThat(result.gradingFailed()).isEqualTo(20L);
        // 200 / 220 = 90.909... -> rounded to 1 decimal place.
        assertThat(result.successRatePercent()).isEqualTo(90.9);
    }

    /**
     * When no session has finished grading or failed grading, the rate must be null, NOT 0: a
     * system that has run no session is not a system with a 0% success rate, and the red band on
     * the dashboard distinguishes exactly those two situations.
     */
    @Test
    void successRateIsNullWhenNothingWasGradedInTheWindow() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(FROM, TO));

        assertThat(result.successRatePercent()).isNull();
        assertThat(result.daily()).hasSize(4);
        assertThat(result.daily()).allSatisfy(bucket -> {
            assertThat(bucket.graded()).isZero();
            assertThat(bucket.failed()).isZero();
        });
    }

    /**
     * The current snapshot does not depend on the window, so it must go straight from the
     * repository to the response.
     */
    @Test
    void liveCountsPassThroughUnchanged() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(FROM, TO));

        assertThat(result.sessionsInProgress()).isEqualTo(6L);
        assertThat(result.assessmentsInProgress()).isEqualTo(2L);
        assertThat(result.gradingQueueDepth()).isEqualTo(41L);
    }

    /**
     * A reversed range (from > to) is a client-side error. The current snapshot is still returned
     * because it is correct whatever the window is, while the day series is empty -- a truly empty
     * range has no days.
     */
    @Test
    void invertedRangeKeepsLiveCountsAndReturnsNoDays() {
        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(TO, FROM));

        assertThat(result.daily()).isEmpty();
        assertThat(result.successRatePercent()).isNull();
        assertThat(result.sessionsInProgress()).isEqualTo(6L);
    }

    /** Leaving both points empty = the last 14 days up to now. */
    @Test
    void defaultsToFourteenDaysEndingNow() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(null, null));

        assertThat(result.daily()).hasSize(14);
        assertThat(result.daily().get(13).day())
            .isEqualTo(LocalDate.now(ZoneConstant.BUSINESS_ZONE));
    }

    /**
     * Days must be cut by Vietnam time, not UTC: a range that starts at 00:00 Vietnam time is 17:00
     * UTC the day before, so cutting by UTC would produce an extra empty day at the start of the
     * series.
     */
    @Test
    void bucketsByBusinessZoneNotUtc() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        PlatformOperationalHealthResponse result = useCase.execute(new ViewPlatformOperationalHealthQuery(FROM, TO));

        assertThat(result.daily().get(0).day()).isEqualTo(LocalDate.of(2026, 8, 1));
        ArgumentCaptor<ZoneId> zoneCaptor = ArgumentCaptor.forClass(ZoneId.class);
        org.mockito.Mockito.verify(queryRepository)
            .findGradingOutcomeByDay(eq(FROM), eq(TO), zoneCaptor.capture());
        assertThat(zoneCaptor.getValue()).isEqualTo(ZoneConstant.BUSINESS_ZONE);
    }
}
