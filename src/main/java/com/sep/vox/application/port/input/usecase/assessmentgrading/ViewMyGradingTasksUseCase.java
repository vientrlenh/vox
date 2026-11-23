package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The queue of attempts of the logged-in teacher. The scope is themself -- teacherId is taken
 * from the token, not accepted from the client.
 *
 * <p>CENTRALIZED assessments ONLY, hard-locked with no parameter accepted: a class test has its own
 * screen per test ({@code myClassTestGradingTasks}), where the teacher also sees the student's name and class.
 * Mixing the two kinds in here is the bug being fixed.
 *
 * <p>{@code assessmentId} is the opposite -- accepted from the client, but only to narrow: it sits NEXT TO the
 * teacherId condition and does not replace it, so it cannot open anything more.
 */
@Service
@RequiredArgsConstructor
public class ViewMyGradingTasksUseCase
        implements IUseCase<ViewMyGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyGradingTasksQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            currentUserId,
            input.assessmentId(),
            AssessmentKind.CENTRALIZED.name(),
            input.status(),
            input.roundType(),
            input.page(),
            input.size());
    }
}
