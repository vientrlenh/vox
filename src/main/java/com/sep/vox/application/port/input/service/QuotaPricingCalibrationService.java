package com.sep.vox.application.port.input.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;
import com.sep.vox.application.query.repository.SessionCostQueryRepository;
import com.sep.vox.application.port.output.QuotaPricingCalibrationConfigPort;
import com.sep.vox.application.port.output.QuotaPricingConfigPort;

/**
 * Tự tính lại estimatedCostPer{Exam,Practice}SecondUsd (xem QuotaPricingProperties) từ chi phí AI
 * THẬT đã ghi nhận (ai_usage_record) và giây trả lời THẬT trong cửa sổ trailing gần nhất -- thay
 * cho việc phải tự tay đoán/sửa .env mỗi lần muốn calibrate lại (xem AI_COST_CALIBRATION_LOG.md --
 * đây chính là câu SQL trong file đó, viết lại bằng code, chạy định kỳ qua QuotaPricingCalibrationJob).
 *
 * <p>EXAM và PRACTICE tách 2 rate riêng (xem QuotaPricingSource) vì pipeline AI khác hẳn nhau
 * (evalGraph chấm exam nặng hơn hẳn realtimeCorrectionGraph của PRACTICE) -- gộp chung sẽ làm lệch
 * rate của cả 2 bên tùy khối lượng dữ liệu bên nào nhiều hơn. 2 nguồn dùng CHUNG 1 bộ ngưỡng tuning
 * (windowDays/minSampleSessions/maxChangeRatio/min-maxRateBound) -- chỉ khác bảng JOIN để lấy giây
 * trả lời thật (exam_item_responses vs practice_response_turns) và rate mặc định fallback.
 *
 * <p>Dùng giây TRẢ LỜI THẬT làm mẫu số (không phải examTimeDurationSecond cấu hình) -- quyết
 * định nghiệp vụ đã chốt: cho margin an toàn lớn hơn vì học sinh luôn dùng ít hơn thời gian được
 * cấp, khớp cách đã calibrate tay 2 lần trước đó.
 *
 * <p>Không insert row nếu mẫu quá nhỏ (dưới minSampleSessions) -- phía đọc (QuotaPricingService)
 * luôn chỉ cần lấy row mới nhất theo nguồn, không phải lọc "có đủ tin cậy không".
 */
@Service
public class QuotaPricingCalibrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaPricingCalibrationService.class);

    private final SessionCostQueryRepository sessionCostQueryRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final QuotaPricingCalibrationConfigPort calibrationConfig;
    private final QuotaPricingConfigPort quotaPricingConfig;

    public QuotaPricingCalibrationService(
            SessionCostQueryRepository sessionCostQueryRepository,
            ExamItemResponseRepository examItemResponseRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            QuotaPricingCalibrationRepository quotaPricingCalibrationRepository,
            QuotaPricingCalibrationConfigPort calibrationConfig,
            QuotaPricingConfigPort quotaPricingConfig) {
        this.sessionCostQueryRepository = sessionCostQueryRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.quotaPricingCalibrationRepository = quotaPricingCalibrationRepository;
        this.calibrationConfig = calibrationConfig;
        this.quotaPricingConfig = quotaPricingConfig;
    }

    /** Calibrate estimatedCostPerExamSecondUsd -- giây trả lời thật lấy từ exam_item_responses. */
    @Transactional
    public void recalibrateExam() {
        recalibrate(
            QuotaPricingSource.EXAM,
            examItemResponseRepository::sumDurationSecondsGroupedBySessionIds,
            quotaPricingConfig::estimatedCostPerExamSecondUsd
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
        var windowDays = calibrationConfig.windowDays();
        var minSampleSessions = calibrationConfig.minSampleSessions();
        var since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        var costs = sessionCostQueryRepository.sumCostUsdGroupedBySessionSince(since);
        if (costs.size() < minSampleSessions) {
            LOGGER.info(
                "[quota-pricing-calibration] {} bỏ qua: chỉ có {} session phát sinh usage trong {} ngày gần nhất, "
                    + "cần tối thiểu {}",
                source, costs.size(), windowDays, minSampleSessions
            );
            return;
        }

        var sessionIds = costs.stream().map(a -> a.sessionId()).toList();
        Map<UUID, Long> durationsBySession = durationLookup.apply(sessionIds).stream()
            .collect(Collectors.toMap(a -> a.sessionId(), a -> a.totalDurationSeconds()));

        var totalCost = BigDecimal.ZERO;
        var totalSeconds = 0L;
        var matchedCount = 0;
        for (var cost : costs) {
            var duration = durationsBySession.get(cost.sessionId());
            if (duration == null || duration <= 0) {
                // Session có usage AI nhưng không khớp được giây trả lời thật ở nguồn này (vd
                // session thuộc nguồn kia -- exam session không có row practice_response_turns và
                // ngược lại, hoặc lỗi transcribe) -- loại khỏi mẫu, không thể tính rate cho session này.
                continue;
            }
            totalCost = totalCost.add(cost.totalCostUsd());
            totalSeconds += duration;
            matchedCount++;
        }

        if (matchedCount < minSampleSessions || totalSeconds <= 0) {
            LOGGER.info(
                "[quota-pricing-calibration] {} bỏ qua: chỉ khớp được {} session có cả cost lẫn giây trả lời thật, "
                    + "cần tối thiểu {}",
                source, matchedCount, minSampleSessions
            );
            return;
        }

        var rawRate = totalCost.divide(BigDecimal.valueOf(totalSeconds), 6, RoundingMode.HALF_UP);

        var previousApplied = quotaPricingCalibrationRepository.findLatest(source)
            .map(c -> c.getAppliedRateUsdPerSecond())
            .orElseGet(defaultRate);

        var maxChangeRatio = calibrationConfig.maxChangeRatio();
        var lowerSmoothBound = previousApplied.multiply(BigDecimal.ONE.subtract(maxChangeRatio));
        var upperSmoothBound = previousApplied.multiply(BigDecimal.ONE.add(maxChangeRatio));
        var smoothed = clamp(rawRate, lowerSmoothBound, upperSmoothBound);

        var applied = clamp(smoothed, calibrationConfig.minRateBound(), calibrationConfig.maxRateBound());

        String note = applied.compareTo(rawRate) == 0
            ? null
            : "raw=%s bị điều chỉnh còn applied=%s (làm mượt tối đa %s%% so với lần trước %s, hoặc chặn biên an toàn)"
                .formatted(rawRate, applied, maxChangeRatio.multiply(BigDecimal.valueOf(100)), previousApplied);

        var calibration = new QuotaPricingCalibration(
            Instant.now(),
            windowDays,
            matchedCount,
            totalCost,
            totalSeconds,
            rawRate,
            applied,
            note,
            source
        );
        quotaPricingCalibrationRepository.save(calibration);

        LOGGER.info(
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
