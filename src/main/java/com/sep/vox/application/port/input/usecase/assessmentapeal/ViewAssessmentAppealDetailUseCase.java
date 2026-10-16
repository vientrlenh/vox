package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AppealDetailInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewAssessmentAppealDetailUseCase implements IUseCase<UUID, AppealDetailInfo> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    public ViewAssessmentAppealDetailUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentAppealAccessService assessmentAppealAccessService) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
    }

    /**
     * ⚠️ Chốt quyền nằm Ở ĐÂY, không ở {@code @PreAuthorize} của controller. Bề mặt
     * GraphQL đã nới sang {@code TEACHER} để giáo viên tạo bài kiểm tra trên lớp đọc
     * được đơn của bài mình — nếu chỉ lọc theo trường như trước thì MỌI giáo viên đọc
     * được MỌI đơn phúc khảo của trường.
     */
    @Override
    @Transactional(readOnly = true)
    public AppealDetailInfo execute(UUID appealId) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.load(appealId);
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(context.schoolId());
        return assessmentAppealQueryRepository.findDetailById(appealId, schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn phúc khảo."));
    }
}
