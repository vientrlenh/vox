package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;

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
            input.status(),
            input.roundType(),
            input.page(),
            input.size()
        );
    }
}
