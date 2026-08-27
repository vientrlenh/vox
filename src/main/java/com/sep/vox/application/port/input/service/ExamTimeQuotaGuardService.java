package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ExamTimeQuotaGuardService {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public ExamTimeQuotaGuardService(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    public void requireWithinPlan(UUID schoolId, Integer durationSeconds, String targetLabel) {
        if (schoolId == null || durationSeconds == null || durationSeconds <= 0) {
            return;
        }

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể gắn cấu trúc đề này."));
        var plan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói subscription"));

        var maxMinutes = plan.getMaxTimePerAttemptMin();
        if (maxMinutes == null || maxMinutes <= 0) {
            return;
        }

        long maxSeconds = maxMinutes.longValue() * 60L;
        if (durationSeconds.longValue() > maxSeconds) {
            throw new PlanLimitExceededException(
                "%s có thời lượng %s, vượt giới hạn %s của gói %s. Vui lòng giảm câu hỏi hoặc nâng cấp gói trước khi gắn."
                    .formatted(targetLabel, formatDuration(durationSeconds), formatDuration(maxSeconds), plan.getName()));
        }
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remainingSeconds = safeSeconds % 60L;
        if (remainingSeconds == 0L) {
            return minutes + " phút";
        }
        return minutes + " phút " + remainingSeconds + " giây";
    }
}
