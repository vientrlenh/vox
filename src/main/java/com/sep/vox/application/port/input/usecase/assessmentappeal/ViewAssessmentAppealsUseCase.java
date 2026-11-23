package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentAppealsUseCase implements IUseCase<SearchAssessmentAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAssessmentAppealsQuery input) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, input.status(), input.keyword(), input.page(), input.size());
    }
}
