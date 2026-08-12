package com.sep.vox.infrastructure.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.QuotaPricingCalibrationService;

// Trước đây estimatedCostPerExamSecondUsd (ClassTestTokenQuotaGuardService) là hằng số tĩnh phải
// tự tay đoán/sửa .env (xem AI_COST_CALIBRATION_LOG.md) -- job này tự động chạy lại đúng phép
// tính đó định kỳ trên dữ liệu ai_usage_record/exam_item_responses thật, để guard tự thích nghi
// theo chi phí AI thật tế mà không cần con người can thiệp liên tục.
@Component
public class QuotaPricingCalibrationJob {

    private final QuotaPricingCalibrationService quotaPricingCalibrationService;

    public QuotaPricingCalibrationJob(QuotaPricingCalibrationService quotaPricingCalibrationService) {
        this.quotaPricingCalibrationService = quotaPricingCalibrationService;
    }

    @Scheduled(cron = "0 0 3 * * MON")
    public void run() {
        quotaPricingCalibrationService.recalibrate();
    }
}
