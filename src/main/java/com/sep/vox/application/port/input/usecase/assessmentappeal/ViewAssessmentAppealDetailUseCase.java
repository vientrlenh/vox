package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentAppealDetailUseCase implements IUseCase<UUID, AppealDetailInfo> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    /**
     * ⚠️ The permission check lives HERE, not in the controller's {@code @PreAuthorize}. The GraphQL surface was
     * widened to {@code TEACHER} so that the teacher who created a class test can read the appeals of their own
     * test -- if it only filtered by school as before, EVERY teacher could read EVERY appeal of the school.
     */
    @Override
    @Transactional(readOnly = true)
    public AppealDetailInfo execute(UUID appealId) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(appealId);
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(context.schoolId());
        return assessmentAppealQueryRepository.findDetailById(appealId, schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal not found."));
    }
}
