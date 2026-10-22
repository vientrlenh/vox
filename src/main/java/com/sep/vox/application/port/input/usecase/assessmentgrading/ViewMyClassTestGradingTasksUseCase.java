package com.sep.vox.application.port.input.usecase.assessmentgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingTaskInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.shared.PageResult;

/**
 * Hàng đợi chấm của một bài kiểm tra trên lớp. Khoá loại bài ngay ở đây: {@code assessmentId}
 * đã thu hẹp phạm vi rồi, nhưng để trống loại bài thì khi client gửi nhầm id của một kỳ
 * thi tập trung, màn bài trên lớp sẽ lặng lẽ trả về dữ liệu của màn kia.
 */
@Service
public class ViewMyClassTestGradingTasksUseCase
        implements IUseCase<ViewMyClassTestGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewMyClassTestGradingTasksUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyClassTestGradingTasksQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            currentUserId,
            input.assessmentId(),
            AssessmentKind.CLASS_TEST.name(),
            input.status(),
            input.roundType(),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );
    }
}
