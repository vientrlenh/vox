package com.sep.vox.application.port.input.usecase.assessmentapeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AppealSummaryInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewAssessmentAppealsUseCase implements IUseCase<SearchAssessmentAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    public ViewAssessmentAppealsUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentAppealAccessService assessmentAppealAccessService) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAssessmentAppealsQuery input) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return assessmentAppealQueryRepository.searchAppeals(
            schoolId, null, input.status(), input.keyword(), input.page(), input.size());
    }
}
