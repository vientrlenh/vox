package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.assessment.EstimateAssessmentTokenQuotaQuery;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.AssessmentTokenEstimateResponse;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Warns about the estimated AI cost RIGHT WHEN an assessment is created (before publish). It never throws; it only
 * lets the frontend show an early warning banner. The real BLOCKING still lives in
 * ClassTestTokenQuotaGuardService.requireWithinTokenQuota, called on publish, edit and adding candidates.
 */
@Service
@RequiredArgsConstructor
public class EstimateAssessmentTokenQuotaUseCase implements IUseCase<EstimateAssessmentTokenQuotaQuery, AssessmentTokenEstimateResponse> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public AssessmentTokenEstimateResponse execute(EstimateAssessmentTokenQuotaQuery input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        if (!userContextPort.isSystemAdmin() && !assessment.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return classTestTokenQuotaGuardService.estimateTokenQuota(assessment);
    }
}
