package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.AppealSummaryInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.shared.PageResult;

// Dùng chung type AppealSummaryInfo với school admin theo quyết định của chủ dự án -- field
// reviewerName vẫn tồn tại trên type này, nhưng FE (web/mobile) không được hỏi/hiện field đó
// cho học sinh (xem useStudentAppealQueries.ts / appeal_api.dart).
@Service
public class ViewMyAppealsUseCase
        implements IUseCase<SearchAssessmentAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyAppealsUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAssessmentAppealsQuery input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        return assessmentAppealQueryRepository.searchAppealsByStudentId(
            studentId,
            input.status(),
            input.page(),
            input.size()
        );
    }
}
