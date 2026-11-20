package com.sep.vox.infrastructure.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.QuotaPricingCalibrationService;

// Previously estimatedCostPerAssessmentSecondUsd (ClassTestTokenQuotaGuardService) was a static constant that had to be
// guessed/edited in .env by hand (see AI_COST_CALIBRATION_LOG.md) -- this job automatically reruns exactly that
// computation periodically on the real ai_usage_record/assessment_item_responses data, so the guard adapts
// to the real AI cost without constant human intervention.
@Component
public class QuotaPricingCalibrationJob {

    private final QuotaPricingCalibrationService quotaPricingCalibrationService;

    public QuotaPricingCalibrationJob(QuotaPricingCalibrationService quotaPricingCalibrationService) {
        this.quotaPricingCalibrationService = quotaPricingCalibrationService;
    }

    @Scheduled(cron = "0 0 3 * * MON")
    public void run() {
        quotaPricingCalibrationService.recalibrateAssessment();
    }

    // A separate @Scheduled (not merged into a sequential run() call) so that a failure in this source does not block the
    // other -- each rate bucket is handled independently, in the spirit of ASSESSMENT/PRACTICE being separate in
    // CompleteAssessmentSessionGradingUseCase.
    @Scheduled(cron = "0 0 3 * * MON")
    public void runPractice() {
        quotaPricingCalibrationService.recalibratePractice();
    }
}
