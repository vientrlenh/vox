package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The number cards at the top of the class test grading screen of the teacher who created the test.
 *
 * <p>Kept apart from {@link ViewGradingStatsUseCase} instead of loosening it: that one accepts an empty
 * {@code assessmentId} meaning "the whole school", so loosening the permission there would let a teacher
 * read the grading progress of the whole school. Here {@code assessmentId} is mandatory and must be an assessment
 * where the caller is the CHAIR.
 *
 * <p>Counts by ATTEMPT and not by student: a student who took two attempts is two attempts to be
 * graded, so a total larger than the class size is correct, not a duplicate-row bug.
 */
@Service
@RequiredArgsConstructor
public class ViewClassTestGradingStatsUseCase implements IUseCase<UUID, GradingStatsInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(UUID assessmentId) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(assessmentId, currentUserId);
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.stats(schoolId, assessmentId, null, AssessmentKind.CLASS_TEST.name());
    }
}
