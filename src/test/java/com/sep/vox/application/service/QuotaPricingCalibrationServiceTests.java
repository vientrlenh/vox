package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.QuotaPricingCalibrationService;
import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.domain.repository.SessionCostAggregate;
import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.properties.QuotaPricingCalibrationProperties;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;

/**
 * recalibrateExam()/recalibratePractice() phải: bỏ qua khi mẫu quá nhỏ (không insert row), làm
 * mượt khi rawRate nhảy quá maxChangeRatio so với applied lần trước, và chặn biên an toàn
 * (min/max bound) -- 3 hành vi này là lý do tồn tại của service, khác với việc tự tay sửa .env
 * trực tiếp. 2 method dùng CHUNG thuật toán (recalibrate() private) nhưng đọc/ghi rate theo
 * QuotaPricingSource riêng và join nguồn duration khác nhau (exam_item_responses vs
 * practice_response_turn) -- xem thêm should_use_practice_duration_source_and_tag_pricing_source.
 */
class QuotaPricingCalibrationServiceTests {

    private AiUsageRecordRepository aiUsageRecordRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private PracticeResponseTurnRepository practiceResponseTurnRepository;
    private QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private QuotaPricingCalibrationService service;

    @BeforeEach
    void setUp() {
        aiUsageRecordRepository = mock(AiUsageRecordRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        practiceResponseTurnRepository = mock(PracticeResponseTurnRepository.class);
        quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var calibrationProperties = new QuotaPricingCalibrationProperties(90, 2, new BigDecimal("0.20"),
            new BigDecimal("0.001"), new BigDecimal("1.0"));
        var quotaPricingProperties = new QuotaPricingProperties(new BigDecimal("0.01"), new BigDecimal("0.01"));

        service = new QuotaPricingCalibrationService(
            aiUsageRecordRepository,
            examItemResponseRepository,
            practiceResponseTurnRepository,
            quotaPricingCalibrationRepository,
            calibrationProperties,
            quotaPricingProperties);
    }

    @Test
    void should_skip_when_not_enough_sessions_with_cost() {
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any()))
            .thenReturn(List.of(new SessionCostAggregate(UUID.randomUUID(), BigDecimal.ONE)));

        service.recalibrateExam();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }

    @Test
    void should_skip_when_sessions_have_cost_but_no_matching_duration() {
        var sessionA = UUID.randomUUID();
        var sessionB = UUID.randomUUID();
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(sessionA, BigDecimal.TEN),
            new SessionCostAggregate(sessionB, BigDecimal.TEN)
        ));
        // Chỉ 1 session có duration khớp -- dưới minSampleSessions=2 nên vẫn phải bỏ qua.
        when(examItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection()))
            .thenReturn(List.of(new SessionDurationAggregate(sessionA, 100L)));

        service.recalibrateExam();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }

    @Test
    void should_save_raw_rate_unchanged_when_within_smoothing_and_bounds() {
        var sessionA = UUID.randomUUID();
        var sessionB = UUID.randomUUID();
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(sessionA, new BigDecimal("1.00")),
            new SessionCostAggregate(sessionB, new BigDecimal("1.00"))
        ));
        when(examItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        // previousApplied = 0.01 (default .env), rawRate = 2.00/200 = 0.01 -- không đổi, không cần làm mượt.
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.EXAM)).thenReturn(Optional.empty());

        service.recalibrateExam();

        var captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getSessionCount()).isEqualTo(2);
        assertThat(saved.getRawRateUsdPerSecond()).isEqualByComparingTo("0.01");
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.01");
        assertThat(saved.getNote()).isNull();
        assertThat(saved.getPricingSource()).isEqualTo(QuotaPricingSource.EXAM);
    }

    @Test
    void should_smooth_when_raw_rate_jumps_more_than_max_change_ratio() {
        var sessionA = UUID.randomUUID();
        var sessionB = UUID.randomUUID();
        // rawRate = 20.00/200 = 0.10 -- gấp 10 lần previousApplied (0.01), vượt xa maxChangeRatio=20%.
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(sessionA, new BigDecimal("10.00")),
            new SessionCostAggregate(sessionB, new BigDecimal("10.00"))
        ));
        when(examItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        var previous = new QuotaPricingCalibration(Instant.now(), 90, 10, BigDecimal.ONE, 100L,
            new BigDecimal("0.01"), new BigDecimal("0.01"), null, QuotaPricingSource.EXAM);
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.EXAM)).thenReturn(Optional.of(previous));

        service.recalibrateExam();

        var captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getRawRateUsdPerSecond()).isEqualByComparingTo("0.10");
        // Bị kẹp còn tối đa +20% so với previousApplied (0.01) = 0.012, KHÔNG được nhảy thẳng lên 0.10.
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.012");
        assertThat(saved.getNote()).isNotNull();
    }

    @Test
    void should_clamp_to_max_rate_bound_even_after_smoothing() {
        var sessionA = UUID.randomUUID();
        var sessionB = UUID.randomUUID();
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(sessionA, new BigDecimal("500.00")),
            new SessionCostAggregate(sessionB, new BigDecimal("500.00"))
        ));
        when(examItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        // previousApplied đã sát trần maxRateBound=1.0 -- dù làm mượt xong vẫn phải kẹp lại đúng 1.0.
        var previous = new QuotaPricingCalibration(Instant.now(), 90, 10, BigDecimal.ONE, 100L,
            new BigDecimal("0.99"), new BigDecimal("0.99"), null, QuotaPricingSource.EXAM);
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.EXAM)).thenReturn(Optional.of(previous));

        service.recalibrateExam();

        var captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        assertThat(captor.getValue().getAppliedRateUsdPerSecond()).isEqualByComparingTo("1.0");
    }

    @Test
    void should_use_practice_duration_source_and_tag_pricing_source() {
        var sessionA = UUID.randomUUID();
        var sessionB = UUID.randomUUID();
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(sessionA, new BigDecimal("1.00")),
            new SessionCostAggregate(sessionB, new BigDecimal("1.00"))
        ));
        when(practiceResponseTurnRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.PRACTICE)).thenReturn(Optional.empty());

        service.recalibratePractice();

        // Không được đụng gì tới nguồn duration của EXAM khi calibrate PRACTICE.
        verifyNoInteractions(examItemResponseRepository);
        var captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getPricingSource()).isEqualTo(QuotaPricingSource.PRACTICE);
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.01");
    }

    @Test
    void should_skip_practice_when_only_exam_side_has_matching_duration() {
        var examOnlySession = UUID.randomUUID();
        when(aiUsageRecordRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostAggregate(examOnlySession, BigDecimal.TEN)
        ));
        // practiceResponseTurnRepository không trả về gì cho session này (nó là session EXAM,
        // không có row practice_response_turn) -- đúng hành vi thật của join, không phải mock hoá đơn giản.
        when(practiceResponseTurnRepository.sumDurationSecondsGroupedBySessionIds(anyCollection()))
            .thenReturn(List.of());

        service.recalibratePractice();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }
}
