package com.sep.vox.application.port.input.usecase.assessmentgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingTaskInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.shared.PageResult;

/**
 * Hàng đợi bài của giáo viên đang đăng nhập. Phạm vi là chính họ — teacherId lấy
 * từ token, không nhận từ client.
 *
 * <p>CHỈ kỳ thi tập trung, khoá cứng không nhận tham số: bài kiểm tra trên lớp có màn
 * riêng theo từng bài ({@code myClassTestGradingTasks}), nơi giáo viên còn thấy tên học
 * sinh và lớp. Trộn chung hai loại vào đây là lỗi đang sửa.
 *
 * <p>{@code assessmentId} thì ngược lại — nhận từ client, nhưng chỉ để thu hẹp: nó nằm CẠNH
 * điều kiện teacherId chứ không thay nó, nên không mở thêm được gì.
 */
@Service
public class ViewMyGradingTasksUseCase
        implements IUseCase<ViewMyGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewMyGradingTasksUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyGradingTasksQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            currentUserId,
            input.assessmentId(),
            AssessmentKind.CENTRALIZED.name(),
            input.status(),
            input.roundType(),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size());
    }
}
