package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Hàng đợi chấm của một bài kiểm tra trên lớp. Khoá loại bài ngay ở đây: {@code examId}
 * đã thu hẹp phạm vi rồi, nhưng để trống loại bài thì khi client gửi nhầm id của một kỳ
 * thi tập trung, màn bài trên lớp sẽ lặng lẽ trả về dữ liệu của màn kia.
 */
@Service
public class ViewMyClassTestGradingTasksUseCase
        implements IUseCase<ViewMyClassTestGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewMyClassTestGradingTasksUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyClassTestGradingTasksQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        return examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            currentUserId,
            input.examId(),
            ExamKind.CLASS_TEST.name(),
            input.status(),
            input.roundType(),
            input.page(),
            input.size()
        );
    }
}
