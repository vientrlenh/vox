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
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.application.query.dto.SessionCostDto;
import com.sep.vox.application.query.repository.SessionCostQueryRepository;
import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.properties.QuotaPricingCalibrationProperties;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;

/**
 * recalibrateAssessment()/recalibratePractice() must: skip when the sample is too small (no row inserted), smooth
 * when rawRate jumps by more than maxChangeRatio compared with the last applied one, and enforce the safe bounds
 * (min/max bound) -- these 3 behaviors are the reason the service exists, as opposed to editing .env
 * by hand. The 2 methods SHARE the same algorithm (the private recalibrate()) but read/write the rate by their own
 * QuotaPricingSource and join a different duration source (assessment_item_responses vs
 * practice_response_turn) -- see also should_use_practice_duration_source_and_tag_pricing_source.
 */
class QuotaPricingCalibrationServiceTests {

    private SessionCostQueryRepository sessionCostQueryRepository;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private PracticeResponseTurnRepository practiceResponseTurnRepository;
    private QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private QuotaPricingCalibrationService service;

    @BeforeEach
    void setUp() {
        sessionCostQueryRepository = mock(SessionCostQueryRepository.class);
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        practiceResponseTurnRepository = mock(PracticeResponseTurnRepository.class);
        quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        QuotaPricingCalibrationProperties calibrationProperties = new QuotaPricingCalibrationProperties(90, 2, new BigDecimal("0.20"),
            new BigDecimal("0.001"), new BigDecimal("1.0"));
        QuotaPricingProperties quotaPricingProperties = new QuotaPricingProperties(new BigDecimal("0.01"), new BigDecimal("0.01"));

        service = new QuotaPricingCalibrationService(
            sessionCostQueryRepository,
            assessmentItemResponseRepository,
            practiceResponseTurnRepository,
            quotaPricingCalibrationRepository,
            calibrationProperties,
            quotaPricingProperties);
    }

    @Test
    void should_skip_when_not_enough_sessions_with_cost() {
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any()))
            .thenReturn(List.of(new SessionCostDto(UUID.randomUUID(), BigDecimal.ONE)));

        service.recalibrateAssessment();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }

    @Test
    void should_skip_when_sessions_have_cost_but_no_matching_duration() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(sessionA, BigDecimal.TEN),
            new SessionCostDto(sessionB, BigDecimal.TEN)
        ));
        // Only 1 session has a matching duration -- below minSampleSessions=2, so it must still be
        // skipped.
        when(assessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection()))
            .thenReturn(List.of(new SessionDurationAggregate(sessionA, 100L)));

        service.recalibrateAssessment();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }

    @Test
    void should_save_raw_rate_unchanged_when_within_smoothing_and_bounds() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(sessionA, new BigDecimal("1.00")),
            new SessionCostDto(sessionB, new BigDecimal("1.00"))
        ));
        when(assessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        // previousApplied = 0.01 (default .env), rawRate = 2.00/200 = 0.01 -- unchanged, no
        // smoothing needed.
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.ASSESSMENT)).thenReturn(Optional.empty());

        service.recalibrateAssessment();

        ArgumentCaptor<QuotaPricingCalibration> captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        QuotaPricingCalibration saved = captor.getValue();
        assertThat(saved.getSessionCount()).isEqualTo(2);
        assertThat(saved.getRawRateUsdPerSecond()).isEqualByComparingTo("0.01");
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.01");
        assertThat(saved.getNote()).isNull();
        assertThat(saved.getPricingSource()).isEqualTo(QuotaPricingSource.ASSESSMENT);
    }

    @Test
    void should_smooth_when_raw_rate_jumps_more_than_max_change_ratio() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        // rawRate = 20.00/200 = 0.10 -- 10 times previousApplied (0.01), far beyond
        // maxChangeRatio=20%.
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(sessionA, new BigDecimal("10.00")),
            new SessionCostDto(sessionB, new BigDecimal("10.00"))
        ));
        when(assessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        QuotaPricingCalibration previous = QuotaPricingCalibration.builder()
            .computedAt(Instant.now())
            .windowDays(90)
            .sessionCount(10)
            .totalCostUsd(BigDecimal.ONE)
            .totalAnsweredSeconds(100L)
            .rawRateUsdPerSecond(new BigDecimal("0.01"))
            .appliedRateUsdPerSecond(new BigDecimal("0.01"))
            .note(null)
            .pricingSource(QuotaPricingSource.ASSESSMENT)
            .build();
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.ASSESSMENT)).thenReturn(Optional.of(previous));

        service.recalibrateAssessment();

        ArgumentCaptor<QuotaPricingCalibration> captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        QuotaPricingCalibration saved = captor.getValue();
        assertThat(saved.getRawRateUsdPerSecond()).isEqualByComparingTo("0.10");
        // Clamped to at most +20% over previousApplied (0.01) = 0.012; it must NOT jump straight to
        // 0.10.
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.012");
        assertThat(saved.getNote()).isNotNull();
    }

    @Test
    void should_clamp_to_max_rate_bound_even_after_smoothing() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(sessionA, new BigDecimal("500.00")),
            new SessionCostDto(sessionB, new BigDecimal("500.00"))
        ));
        when(assessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        // previousApplied is already at the maxRateBound=1.0 ceiling -- even after smoothing it
        // must still be clamped to exactly 1.0.
        QuotaPricingCalibration previous = QuotaPricingCalibration.builder()
            .computedAt(Instant.now())
            .windowDays(90)
            .sessionCount(10)
            .totalCostUsd(BigDecimal.ONE)
            .totalAnsweredSeconds(100L)
            .rawRateUsdPerSecond(new BigDecimal("0.99"))
            .appliedRateUsdPerSecond(new BigDecimal("0.99"))
            .note(null)
            .pricingSource(QuotaPricingSource.ASSESSMENT)
            .build();
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.ASSESSMENT)).thenReturn(Optional.of(previous));

        service.recalibrateAssessment();

        ArgumentCaptor<QuotaPricingCalibration> captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        assertThat(captor.getValue().getAppliedRateUsdPerSecond()).isEqualByComparingTo("1.0");
    }

    @Test
    void should_use_practice_duration_source_and_tag_pricing_source() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(sessionA, new BigDecimal("1.00")),
            new SessionCostDto(sessionB, new BigDecimal("1.00"))
        ));
        when(practiceResponseTurnRepository.sumDurationSecondsGroupedBySessionIds(anyCollection())).thenReturn(List.of(
            new SessionDurationAggregate(sessionA, 100L),
            new SessionDurationAggregate(sessionB, 100L)
        ));
        when(quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.PRACTICE)).thenReturn(Optional.empty());

        service.recalibratePractice();

        // Calibrating PRACTICE must not touch the ASSESSMENT duration source at all.
        verifyNoInteractions(assessmentItemResponseRepository);
        ArgumentCaptor<QuotaPricingCalibration> captor = ArgumentCaptor.forClass(QuotaPricingCalibration.class);
        verify(quotaPricingCalibrationRepository).save(captor.capture());
        QuotaPricingCalibration saved = captor.getValue();
        assertThat(saved.getPricingSource()).isEqualTo(QuotaPricingSource.PRACTICE);
        assertThat(saved.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.01");
    }

    @Test
    void should_skip_practice_when_only_assessment_side_has_matching_duration() {
        UUID assessmentOnlySession = UUID.randomUUID();
        when(sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(any())).thenReturn(List.of(
            new SessionCostDto(assessmentOnlySession, BigDecimal.TEN)
        ));
        // practiceResponseTurnRepository returns nothing for this session (it is an ASSESSMENT session,
        // so it has no practice_response_turn rows) -- this is the real behavior of the join, not a
        // simplistic mock.
        when(practiceResponseTurnRepository.sumDurationSecondsGroupedBySessionIds(anyCollection()))
            .thenReturn(List.of());

        service.recalibratePractice();

        verify(quotaPricingCalibrationRepository, never()).save(any());
    }
}
