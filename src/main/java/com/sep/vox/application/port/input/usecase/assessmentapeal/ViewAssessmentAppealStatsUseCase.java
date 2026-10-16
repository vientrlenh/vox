package com.sep.vox.application.port.input.usecase.assessmentapeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.projection.dto.AppealStatsInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewAssessmentAppealStatsUseCase {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    public ViewAssessmentAppealStatsUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentAppealAccessService assessmentAppealAccessService) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
    }

    @Transactional(readOnly = true)
    public AppealStatsInfo execute() {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return assessmentAppealQueryRepository.countByStatus(schoolId);
    }
}
