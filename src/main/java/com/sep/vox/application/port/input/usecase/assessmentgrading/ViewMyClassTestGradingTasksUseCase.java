package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The grading queue of a class test. Locks the kind right here: {@code assessmentId}
 * has already narrowed the scope, but if the kind were left empty, when a client mistakenly sent the id of a centralized
 * assessment, the class test screen would silently return the data of the other screen.
 */
@Service
@RequiredArgsConstructor
public class ViewMyClassTestGradingTasksUseCase
        implements IUseCase<ViewMyClassTestGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyClassTestGradingTasksQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            currentUserId,
            input.assessmentId(),
            AssessmentKind.CLASS_TEST.name(),
            input.status(),
            input.roundType(),
            input.page(),
            input.size()
        );
    }
}
