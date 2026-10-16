package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Danh sách giáo viên có thể nhận chấm phúc khảo cho một đơn.
 *
 * <p>Người xung đột lợi ích vẫn nằm trong danh sách nhưng mang cờ {@code conflicted}
 * — xem {@link AppealReviewerLiteInfo} để biết vì sao không lọc bỏ.
 */
@Service
public class ViewAssignableReviewersUseCase
        implements IUseCase<ViewAssignableReviewersQuery, List<AppealReviewerLiteInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    public ViewAssignableReviewersUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentAppealAccessService assessmentAppealAccessService) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealReviewerLiteInfo> execute(ViewAssignableReviewersQuery input) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));

        // Đơn phải thuộc trường của người gọi — nếu không, cờ xung đột sẽ rò rỉ thông
        // tin về bài thi của trường khác.
        if (input.appealId() != null) {
            var context = assessmentAppealAccessService.load(input.appealId());
            assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        }
        return assessmentAppealQueryRepository.findAssignableReviewers(schoolId, input.appealId(), input.keyword());
    }
}
