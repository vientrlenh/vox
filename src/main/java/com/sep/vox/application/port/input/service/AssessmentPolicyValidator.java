package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;

/**
 * Validates the evaluation criteria set attached to an assessment.
 *
 * <p>If an assessment has no valid policy attached, {@code AssessmentSessionResultCalculator} throws right when computing the result,
 * which means no {@code AssessmentCandidateResult} can be produced -- and that is what the grading assignments point to.
 * The rule is identical for class tests and centralized assessments, so it is gathered in one place, differing only in whether
 * a policy is mandatory right at creation.
 *
 * <p>A system policy ({@code schoolId = null}) can be used by every school.
 */
@Service
@RequiredArgsConstructor
public class AssessmentPolicyValidator {

    private final AssessmentPolicyRepository assessmentPolicyRepository;

    /**
     * Bắt buộc phải có policy hợp lệ. Bài trên lớp chốt policy ngay lúc tạo, không để trôi sang bước
     * sửa — giáo viên tạo xong là chấm được luôn.
     */
    public void requirePublishedInSchool(UUID assessmentPolicyId, UUID schoolId) {
        if (assessmentPolicyId == null) {
            throw new IllegalArgumentException("Bộ tiêu chí đánh giá là bắt buộc");
        }
        validate(assessmentPolicyId, schoolId);
    }

    /**
     * Only checked when there is a value. A centralized assessment creates a "shell" first and chooses the policy in the edit step, so leaving it
     * empty at creation is valid -- but once it is sent it must be correct. The requirement to have a policy is moved to
     * SCHEDULE time (see {@code UpdateAssessmentStatusUseCase}).
     */
    public void validateIfPresent(UUID assessmentPolicyId, UUID schoolId) {
        if (assessmentPolicyId == null) {
            return;
        }
        validate(assessmentPolicyId, schoolId);
    }

    private void validate(UUID assessmentPolicyId, UUID schoolId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(assessmentPolicyId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ tiêu chí đánh giá"));
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được dùng bộ tiêu chí đã xuất bản");
        }
        if (policy.getSchoolId() != null && !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bộ tiêu chí không thuộc trường của bạn");
        }
    }
}
