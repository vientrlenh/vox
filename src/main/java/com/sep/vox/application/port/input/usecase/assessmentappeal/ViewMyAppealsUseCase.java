package com.sep.vox.application.port.input.usecase.assessmentappeal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentappeal.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;

// Shares the AppealSummaryInfo type with the school admin by the project owner's decision -- the
// reviewerName field still exists on this type, but the frontend (web/mobile) must not ask for or show that field to
// the student (see useStudentAppealQueries.ts / appeal_api.dart).
@Service
@RequiredArgsConstructor
public class ViewMyAppealsUseCase
        implements IUseCase<SearchAssessmentAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAssessmentAppealsQuery input) {
        return assessmentAppealQueryRepository.searchAppealsByStudentId(
            userContextPort.getCurrentAuthenticatedUserId(),
            input.status(),
            input.page(),
            input.size()
        );
    }
}
