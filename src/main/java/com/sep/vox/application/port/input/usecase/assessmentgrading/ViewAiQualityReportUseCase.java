package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ViewAiQualityReportQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;

/**
 * "How far off the AI grades", computed from the post-check results that already exist.
 *
 * <p>The scope is always locked to the caller's school -- the grading quality figures of one
 * school are not something another school may see.
 */
@Service
@RequiredArgsConstructor
public class ViewAiQualityReportUseCase implements IUseCase<ViewAiQualityReportQuery, AiQualityReportInfo> {

    private final AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public AiQualityReportInfo execute(ViewAiQualityReportQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        return assessmentResultAuditQueryRepository.aiQualityReport(schoolId, input.assessmentId());
    }
}
