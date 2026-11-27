package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * EVERY submission of a class test, including submissions that have NO assignment yet.
 *
 * <p>Different from {@link ViewMyClassTestGradingTasksUseCase}, which only returns the submissions assigned to
 * the caller: a cleanly AI-graded submission goes straight to {@code RELEASED} so no assignment is opened
 * automatically, and before this screen existed a teacher had no way to see it -- specifically
 * a student's second attempt vanished from the grading screen. Claiming a submission needs {@code candidateResultId},
 * and the only place that hands that id to a teacher is this one.
 *
 * <p>It reuses {@code searchAssignments} of the coordination table instead of writing a new query: the same
 * question ("every submission in scope, with the assignment if any"), only the scope and who
 * is asking differ. The scope here closes to exactly the submissions of tests the caller is CHAIR of.
 */
@Service
@RequiredArgsConstructor
public class ViewClassTestGradingResultsUseCase
        implements IUseCase<ViewClassTestGradingResultsQuery, PageResult<GradingAssignmentRowInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(ViewClassTestGradingResultsQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(input.assessmentId(), currentUserId);
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);

        return assessmentGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
            null,
            null,
            input.resultStatus() == null ? null : input.resultStatus().name(),
            null,
            null,
            input.unassignedOnly(),
            false,
            null,
            input.search(),
            // Locks the kind even though assessmentId has already narrowed the scope: the permission above only accepts
            // class tests, so this is the second fence for the same rule.
            AssessmentKind.CLASS_TEST.name()
        ), input.page(), input.size());
    }
}
