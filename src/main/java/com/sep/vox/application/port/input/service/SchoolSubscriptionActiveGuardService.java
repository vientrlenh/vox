package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Chặn các hành động tốn tài nguyên/AI (tạo Exam tập trung, tạo Class Test, bắt đầu luyện tập AI)
 * khi trường không còn SchoolSubscription nào đang ACTIVE -- factor ra từ đoạn
 * findActiveBySchoolId(...).orElseThrow(...) đang lặp lại inline ở ExamTimeQuotaGuardService/
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
