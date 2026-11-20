package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;

// Dùng chung type AppealSummaryInfo với school admin theo quyết định của chủ dự án -- field
// reviewerName vẫn tồn tại trên type này, nhưng FE (web/mobile) không được hỏi/hiện field đó
// cho học sinh (xem useStudentAppealQueries.ts / appeal_api.dart).
@Service
public class ViewMyAppealsUseCase
        implements IUseCase<SearchExamAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyAppealsUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            UserContextPort userContextPort) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchExamAppealsQuery input) {
        return examAppealQueryRepository.searchAppealsByStudentId(
            userContextPort.getCurrentAuthenticatedUserId(),
            input.status(),
            input.page(),
            input.size()
        );
    }
}
