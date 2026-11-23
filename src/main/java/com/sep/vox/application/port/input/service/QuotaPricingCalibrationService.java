package com.sep.vox.application.port.input.service;

import com.sep.vox.application.query.dto.SessionCostDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;
import com.sep.vox.application.query.repository.SessionCostQueryRepository;
import com.sep.vox.application.port.output.QuotaPricingCalibrationConfigPort;
import com.sep.vox.application.port.output.QuotaPricingConfigPort;

/**
 * Recomputes estimatedCostPer{Assessment,Practice}SecondUsd (see QuotaPricingProperties) from the REAL AI cost
 * already recorded (ai_usage_record) and the REAL answer seconds in the most recent trailing window -- replacing
 * having to guess/edit .env by hand every time we want to recalibrate (see AI_COST_CALIBRATION_LOG.md --
 * this is exactly the SQL in that file, rewritten as code and run periodically through QuotaPricingCalibrationJob).
 *
 * <p>ASSESSMENT and PRACTICE keep 2 separate rates (see QuotaPricingSource) because their AI pipelines are very different
 * (evalGraph grading an assessment is much heavier than PRACTICE's realtimeCorrectionGraph) -- merging them would skew
 * the rate of both sides depending on which side has more data. The 2 sources SHARE 1 set of tuning thresholds
 * (windowDays/minSampleSessions/maxChangeRatio/min-maxRateBound) -- they differ only in the joined table used to get the real
 * answer seconds (assessment_item_responses vs practice_response_turns) and in the default fallback rate.
 *
 * <p>Uses the REAL ANSWER seconds as the denominator (not the configured assessmentTimeDurationSecond) -- a business
 * decision that is already settled: it gives a larger safety margin because students always use less than the time
 * they were given, matching the way it was calibrated by hand the 2 previous times.
 *
 * <p>Does not insert a row if the sample is too small (below minSampleSessions) -- the read side (QuotaPricingService)
 * only ever needs to take the latest row per source, without having to filter on "is it reliable enough".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaPricingCalibrationService {

    private final SessionCostQueryRepository sessionCostQueryRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final QuotaPricingCalibrationConfigPort calibrationConfig;
    private final QuotaPricingConfigPort quotaPricingConfig;

    /** Calibrates estimatedCostPerAssessmentSecondUsd -- the real answer seconds come from assessment_item_responses. */
    @Transactional
    public void recalibrateAssessment() {
        recalibrate(
            QuotaPricingSource.ASSESSMENT,
            assessmentItemResponseRepository::sumDurationSecondsGroupedBySessionIds,
            quotaPricingConfig::estimatedCostPerAssessmentSecondUsd
        );
    }

    /** Calibrate estimatedCostPerPracticeSecondUsd -- giây trả lời thật lấy từ practice_response_turns. */
    @Transactional
    public void recalibratePractice() {
        recalibrate(
            QuotaPricingSource.PRACTICE,
            practiceResponseTurnRepository::sumDurationSecondsGroupedBySessionIds,
            quotaPricingConfig::estimatedCostPerPracticeSecondUsd
        );
    }

    private void recalibrate(
            QuotaPricingSource source,
            Function<Collection<UUID>, List<SessionDurationAggregate>> durationLookup,
            Supplier<BigDecimal> defaultRate) {
        Integer windowDays = calibrationConfig.windowDays();
        Integer minSampleSessions = calibrationConfig.minSampleSessions();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        List<SessionCostDto> costs = sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(since);
        if (costs.size() < minSampleSessions) {
            log.info(
                "[quota-pricing-calibration] {} bỏ qua: chỉ có {} session phát sinh usage trong {} ngày gần nhất, "
                    + "cần tối thiểu {}",
                source, costs.size(), windowDays, minSampleSessions
            );
            return;
        }

        List<UUID> sessionIds = costs.stream().map(a -> a.sessionId()).toList();
        Map<UUID, Long> durationsBySession = durationLookup.apply(sessionIds).stream()
            .collect(Collectors.toMap(a -> a.sessionId(), a -> a.totalDurationSeconds()));

        BigDecimal totalCost = BigDecimal.ZERO;
        long totalSeconds = 0L;
        int matchedCount = 0;
        for (SessionCostDto cost : costs) {
            Long duration = durationsBySession.get(cost.sessionId());
            if (duration == null || duration <= 0) {
                // A session that has AI usage but whose real answer seconds cannot be matched in this source (e.g. the
                // session belongs to the other source -- an assessment session has no practice_response_turns row and
                // vice versa, or a transcribe error) -- excluded from the sample, the rate cannot be computed for this session.
                continue;
            }
            totalCost = totalCost.add(cost.totalCostUsd());
            totalSeconds += duration;
            matchedCount++;
        }

        if (matchedCount < minSampleSessions || totalSeconds <= 0) {
            log.info(
                "[quota-pricing-calibration] {} bỏ qua: chỉ khớp được {} session có cả cost lẫn giây trả lời thật, "
                    + "cần tối thiểu {}",
                source, matchedCount, minSampleSessions
            );
            return;
        }

        BigDecimal rawRate = totalCost.divide(BigDecimal.valueOf(totalSeconds), 6, RoundingMode.HALF_UP);

        BigDecimal previousApplied = quotaPricingCalibrationRepository.findLatest(source)
            .map(c -> c.getAppliedRateUsdPerSecond())
            .orElseGet(defaultRate);

        BigDecimal maxChangeRatio = calibrationConfig.maxChangeRatio();
        BigDecimal lowerSmoothBound = previousApplied.multiply(BigDecimal.ONE.subtract(maxChangeRatio));
        BigDecimal upperSmoothBound = previousApplied.multiply(BigDecimal.ONE.add(maxChangeRatio));
        BigDecimal smoothed = clamp(rawRate, lowerSmoothBound, upperSmoothBound);

        BigDecimal applied = clamp(smoothed, calibrationConfig.minRateBound(), calibrationConfig.maxRateBound());

        String note = applied.compareTo(rawRate) == 0
            ? null
            : "raw=%s bị điều chỉnh còn applied=%s (làm mượt tối đa %s%% so với lần trước %s, hoặc chặn biên an toàn)"
                .formatted(rawRate, applied, maxChangeRatio.multiply(BigDecimal.valueOf(100)), previousApplied);

        QuotaPricingCalibration calibration = QuotaPricingCalibration.builder()
            .computedAt(Instant.now())
            .windowDays(windowDays)
            .sessionCount(matchedCount)
            .totalCostUsd(totalCost)
            .totalAnsweredSeconds(totalSeconds)
            .rawRateUsdPerSecond(rawRate)
            .appliedRateUsdPerSecond(applied)
            .note(note)
            .pricingSource(source)
            .build();
        quotaPricingCalibrationRepository.save(calibration);

        log.info(
            "[quota-pricing-calibration] {} đã tính lại: sessionCount={} rawRate={} appliedRate={} (trước đó {})",
            source, matchedCount, rawRate, applied, previousApplied
        );
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
