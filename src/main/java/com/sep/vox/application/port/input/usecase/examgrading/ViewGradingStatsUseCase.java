package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.service.exam.GradingScopeKind;

/** Thẻ số đầu màn phân công — không có màn riêng cho thống kê. */
@Service
public class ViewGradingStatsUseCase implements IUseCase<ViewGradingStatsQuery, GradingStatsInfo> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewGradingStatsUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(ViewGradingStatsQuery input) {
        UUID currentUserId = examGradingAccessService.requireActiveUserId();
        UUID schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        return examGradingQueryRepository.stats(
            schoolId, input.examId(), input.scheduleId(), GradingScopeKind.orCentralized(input.kind()));
    }
}
