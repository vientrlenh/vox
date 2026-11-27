package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.Assessment;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.assessmentappeal.SearchAppealsByAssessmentQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * The appeals of ONE assessment, for the chair of that assessment (and the school admin of the same school).
 *
 * <p>Kept apart from {@link ViewAssessmentAppealsUseCase} instead of loosening it: the school admin's screen sweeps the
 * whole school and does not take {@code assessmentId}; loosening it would open the appeal data of the whole school
 * to a teacher.
 *
 * <p>Previously it only served class tests ({@code classTestAppeals}). It now accepts every kind of
 * assessment through {@link AssessmentGradingAccessService#isAssessmentChair}, because the chair of a centralized
 * assessment is the one who presses the publish-results button, and an open appeal is one of the two
 * things that block that button -- they must be able to see which appeals remain and who is holding them.
 *
 * <p>Deliberately stops at the LIST and does not open {@link ViewAssessmentAppealDetailUseCase}: the appeal detail
 * carries the student's answer, transcript and audio, while to unblock publishing the chair
 * only needs to know which appeals are open, what step they are at and when they are due. The decision to approve/reject
 * an appeal remains the school's.
 */
@Service
@RequiredArgsConstructor
public class ViewAssessmentAppealsByAssessmentUseCase
        implements IUseCase<SearchAppealsByAssessmentQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAppealsByAssessmentQuery input) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found."));
        assessmentGradingAccessService.authorizeSchoolAdminOrAssessmentChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);

        // The school is taken from the login session first; fall back to the school of the assessment when the caller
        // is not tied to any school -- that is the system admin, and they have already passed the permission gate
        // above. Same handling as ViewAssessmentAppealDetailUseCase; throwing ForbiddenException
        // here would block exactly the person who has just been allowed in.
        UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(assessment.getSchoolId());
        return assessmentAppealQueryRepository.searchAppeals(
            schoolId, input.assessmentId(), input.status(), input.keyword(), input.page(), input.size());
    }
}
