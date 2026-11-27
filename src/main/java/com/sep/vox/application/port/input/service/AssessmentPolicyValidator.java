package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
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
     * A valid policy is required. A class test settles its policy right at creation, not left to drift into
     * the edit step — once the teacher creates it, it can be graded right away.
     */
    public void requirePublishedInSchool(UUID assessmentPolicyId, UUID schoolId) {
        if (assessmentPolicyId == null) {
            throw new IllegalArgumentException("The assessment criteria set is required");
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
            .orElseThrow(() -> new ResourceNotFoundException("Assessment criteria set not found"));
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a published criteria set may be used");
        }
        if (policy.getSchoolId() != null && !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("The criteria set does not belong to your school");
        }
    }
}
