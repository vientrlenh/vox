package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentappeal.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * The teachers who can take the appeal grading for one appeal.
 *
 * <p>A person with a conflict of interest stays in the list but carries the {@code conflicted} flag
 * -- see {@link AppealReviewerLiteInfo} for why they are not filtered out.
 */
@Service
@RequiredArgsConstructor
public class ViewAssignableReviewersUseCase
        implements IUseCase<ViewAssignableReviewersQuery, List<AppealReviewerLiteInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<AppealReviewerLiteInfo> execute(ViewAssignableReviewersQuery input) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("The account does not belong to any school."));

        // The appeal must belong to the caller's school -- otherwise the conflict flag would leak information
        // about another school's assessment.
        if (input.appealId() != null) {
            AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(input.appealId());
            assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        }
        return assessmentAppealQueryRepository.findAssignableReviewers(schoolId, input.appealId(), input.keyword());
    }
}
