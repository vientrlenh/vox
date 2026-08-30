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

import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformOperationalHealthUseCase;
import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;
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
            // 02/08 và 03/08 không có dòng nào -- đây chính là trường hợp phải chèn 0.
            new GradingOutcomeBucketDto(LocalDate.of(2026, 8, 4), 80L, 19L)));

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(FROM, TO));

        assertThat(result.daily()).extracting(bucket -> bucket.day())
            .containsExactly("2026-08-01", "2026-08-02", "2026-08-03", "2026-08-04");
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

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(FROM, TO));

        assertThat(result.graded()).isEqualTo(200L);
        assertThat(result.gradingFailed()).isEqualTo(20L);
        // 200 / 220 = 90,909... -> làm tròn 1 chữ số thập phân.
        assertThat(result.successRatePercent()).isEqualTo(90.9);
    }

    /**
     * Chưa có phiên nào chấm xong lẫn chấm lỗi thì tỷ lệ phải là null, KHÔNG phải 0: một hệ thống
     * chưa chạy phiên nào không phải là hệ thống có tỷ lệ thành công 0%, và dải đỏ trên dashboard
     * phân biệt đúng hai chuyện đó.
     */
    @Test
    void successRateIsNullWhenNothingWasGradedInTheWindow() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(FROM, TO));

        assertThat(result.successRatePercent()).isNull();
        assertThat(result.daily()).hasSize(4);
        assertThat(result.daily()).allSatisfy(bucket -> {
            assertThat(bucket.graded()).isZero();
            assertThat(bucket.failed()).isZero();
        });
    }

    /** Ảnh chụp hiện tại không phụ thuộc cửa sổ, nên phải đi thẳng từ repository ra response. */
    @Test
    void liveCountsPassThroughUnchanged() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(FROM, TO));

        assertThat(result.sessionsInProgress()).isEqualTo(6L);
        assertThat(result.examsInProgress()).isEqualTo(2L);
        assertThat(result.gradingQueueDepth()).isEqualTo(41L);
    }

    /**
     * Khoảng ngược (từ > đến) là lỗi phía client. Vẫn trả ảnh chụp hiện tại vì nó đúng bất kể cửa
     * sổ, còn chuỗi ngày thì rỗng -- một khoảng rỗng thật sự không có ngày nào.
     */
    @Test
    void invertedRangeKeepsLiveCountsAndReturnsNoDays() {
        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(TO, FROM));

        assertThat(result.daily()).isEmpty();
        assertThat(result.successRatePercent()).isNull();
        assertThat(result.sessionsInProgress()).isEqualTo(6L);
    }

    /** Bỏ trống cả hai mốc = 14 ngày gần nhất tính tới bây giờ. */
    @Test
    void defaultsToFourteenDaysEndingNow() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(null, null));

        assertThat(result.daily()).hasSize(14);
        assertThat(result.daily().get(13).day())
            .isEqualTo(LocalDate.now(ZoneConstant.BUSINESS_ZONE).toString());
    }

    /**
     * Cắt ngày phải theo giờ VN chứ không UTC: một khoảng bắt đầu 00:00 giờ VN là 17:00 UTC hôm
     * trước, nên cắt theo UTC sẽ đẻ thêm một ngày rỗng ở đầu chuỗi.
     */
    @Test
    void bucketsByBusinessZoneNotUtc() {
        when(queryRepository.findGradingOutcomeByDay(any(), any(), any())).thenReturn(List.of());

        var result = useCase.execute(new ViewPlatformOperationalHealthUseCase.Query(FROM, TO));

        assertThat(result.daily().get(0).day()).isEqualTo("2026-08-01");
        var zoneCaptor = ArgumentCaptor.forClass(ZoneId.class);
        org.mockito.Mockito.verify(queryRepository)
            .findGradingOutcomeByDay(eq(FROM), eq(TO), zoneCaptor.capture());
        assertThat(zoneCaptor.getValue()).isEqualTo(ZoneConstant.BUSINESS_ZONE);
    }
}
