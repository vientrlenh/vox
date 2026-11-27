package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/** The number cards at the top of the assignment screen -- there is no separate screen for the statistics. */
@Service
@RequiredArgsConstructor
public class ViewGradingStatsUseCase implements IUseCase<ViewGradingStatsQuery, GradingStatsInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(ViewGradingStatsQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.stats(
            schoolId, input.assessmentId(), input.scheduleId(), GradingScopeKind.orCentralized(input.kind()));
    }
}
