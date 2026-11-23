package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;

/**
 * The teacher's grading screen.
 *
 * <p>Does not reuse {@code ViewAssessmentItemResponseEvaluationUseCase}: it checks permission
 * through {@code AssessmentResultAccessService}, which is only open to the candidate and the members of the
 * assessment -- a teacher assigned to grade belongs to neither group so they would be blocked.
 * Here the permission comes from the assignment row, and the query returns empty when nothing matches.
 */
@Service
@RequiredArgsConstructor
public class ViewGradingTaskDetailUseCase implements IUseCase<UUID, GradingTaskDetailInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public GradingTaskDetailInfo execute(UUID assignmentId) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findTaskDetail(assignmentId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("BẢO MẬT: Bạn không được phân công chấm bài thi này."));
    }
}
