package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Danh sách giáo viên có thể nhận chấm phúc khảo cho một đơn.
 *
 * <p>Người xung đột lợi ích vẫn nằm trong danh sách nhưng mang cờ {@code conflicted}
 * — xem {@link AppealReviewerLiteInfo} để biết vì sao không lọc bỏ.
 */
@Service
@RequiredArgsConstructor
public class ViewAssignableReviewersUseCase
        implements IUseCase<ViewAssignableReviewersQuery, List<AppealReviewerLiteInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<AppealReviewerLiteInfo> execute(ViewAssignableReviewersQuery input) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));

        // Đơn phải thuộc trường của người gọi — nếu không, cờ xung đột sẽ rò rỉ thông
        // tin về bài thi của trường khác.
        if (input.appealId() != null) {
            AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(input.appealId());
            assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);
        }
        return assessmentAppealQueryRepository.findAssignableReviewers(schoolId, input.appealId(), input.keyword());
    }
}
