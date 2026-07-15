package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyAppealTasksQuery;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealTaskInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;

@Service
public class ViewMyAppealTasksUseCase implements IUseCase<ViewMyAppealTasksQuery, PageResult<AppealTaskInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewMyAppealTasksUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealTaskInfo> execute(ViewMyAppealTasksQuery input) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        return examAppealQueryRepository.findTasksByReviewerId(
            currentUserId, input.status(), input.page(), input.size());
    }
}
