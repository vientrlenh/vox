package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The assessment list for the filter of the teacher's queue -- ONLY the assessments in which the caller currently
 * has or has had an assignment, not every assessment of the school.
 *
 * <p>It cannot go through the common assessment read path: there the visibility condition is system admin /
 * school admin / assessment member / closed assessment, but a teacher grading submissions is not a
 * member of a centralized assessment -- their permission comes from the assignment row itself. So the scope of the
 * dropdown must also be derived from it, and it closes by itself to exactly the set of submissions assigned to them.
 *
 * <p>ONLY centralized assessments, hard-locked like {@link ViewMyGradingTasksUseCase}: both places must
 * filter on the same set, otherwise the dropdown lists an assessment for which the queue returns nothing.
 */
@Service
@RequiredArgsConstructor
public class ViewMyGradingAssessmentsUseCase implements IUseCase<Void, List<GradingAssessmentOptionInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<GradingAssessmentOptionInfo> execute(Void input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findAssessmentsWithTasksByTeacherId(
            currentUserId, AssessmentKind.CENTRALIZED.name());
    }
}
