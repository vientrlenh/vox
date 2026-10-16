package com.sep.vox.application.port.input.usecase.assessmentgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingStatsInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/** Thẻ số đầu màn phân công — không có màn riêng cho thống kê. */
@Service
public class ViewGradingStatsUseCase implements IUseCase<ViewGradingStatsQuery, GradingStatsInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewGradingStatsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(ViewGradingStatsQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.stats(
            schoolId, input.assessmentId(), input.scheduleId(), GradingScopeKind.orCentralized(input.kind()));
    }
}
