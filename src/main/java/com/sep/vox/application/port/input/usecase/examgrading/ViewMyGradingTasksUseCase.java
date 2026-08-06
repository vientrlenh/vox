package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Hàng đợi bài của giáo viên đang đăng nhập. Phạm vi là chính họ — teacherId lấy
 * từ token, không nhận từ client.
 *
 * <p>CHỈ kỳ thi tập trung, khoá cứng không nhận tham số: bài kiểm tra trên lớp có màn
 * riêng theo từng bài ({@code myClassTestGradingTasks}), nơi giáo viên còn thấy tên học
 * sinh và lớp. Trộn chung hai loại vào đây là lỗi đang sửa.
 */
@Service
public class ViewMyGradingTasksUseCase
        implements IUseCase<ViewMyGradingTasksQuery, PageResult<GradingTaskInfo>> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewMyGradingTasksUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingTaskInfo> execute(ViewMyGradingTasksQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        return examGradingQueryRepository.findTasksByTeacherId(
            currentUserId,
            ExamKind.CENTRALIZED.name(),
            input.status(),
            input.roundType(),
            input.page(),
            input.size());
    }
}
