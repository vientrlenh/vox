package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchMyExamAppealsQuery;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;

@Service
public class ViewMyExamAppealsUseCase implements IUseCase<SearchMyExamAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewMyExamAppealsUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchMyExamAppealsQuery input) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        return examAppealQueryRepository.searchMyAppeals(
            currentUserId, input.status(), input.page(), input.size());
    }
}
