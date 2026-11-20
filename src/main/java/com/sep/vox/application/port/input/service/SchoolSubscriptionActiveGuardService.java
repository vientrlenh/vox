package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Blocks actions that consume resources/AI (creating a centralized Assessment, creating a Class Test, starting AI practice)
 * when the school has no SchoolSubscription that is ACTIVE -- factored out of the
 * findActiveBySchoolId(...).orElseThrow(...) snippet that was repeated inline in AssessmentTimeQuotaGuardService/
 * ClassTestTokenQuotaGuardService.
 */
@Service
public class SchoolSubscriptionActiveGuardService {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;

    public SchoolSubscriptionActiveGuardService(SchoolSubscriptionRepository schoolSubscriptionRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
    }

    public void requireActiveForSchool(UUID schoolId, String actionLabel) {
        schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể " + actionLabel + "."));
    }

    public void requireActiveForStudent(UUID studentId, String actionLabel) {
        schoolSubscriptionRepository.findActiveSubscriptionIdForUser(studentId)
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể " + actionLabel + "."));
    }
}
