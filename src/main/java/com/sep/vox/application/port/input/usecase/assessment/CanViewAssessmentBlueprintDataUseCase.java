package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.CanViewAssessmentBlueprintDataQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Trả lời "người dùng hiện tại có được xem blueprint/blueprintVersion gắn với
 * kỳ thi này không" cho SCHOOL_ADMIN cùng trường (hoặc bất kỳ ai cùng trường
 * sau khi kỳ thi đã đóng). Không bao gồm nhánh "là member của kỳ thi" -- nhánh
 * đó cần load danh sách member qua DataLoader nên vẫn nằm ở controller.
 */
@Service
public class CanViewAssessmentBlueprintDataUseCase implements IUseCase<CanViewAssessmentBlueprintDataQuery, Boolean> {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CanViewAssessmentBlueprintDataUseCase(
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean execute(CanViewAssessmentBlueprintDataQuery input) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(input.assessmentSchoolId())) {
            return false;
        }

        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin) {
            return true;
        }

        // Sau khi kỳ thi đã đóng, ai cùng trường cũng xem được liên kết blueprint -- không cần là member nữa.
        return AssessmentStatus.CLOSED.name().equals(input.assessmentStatus())
            || AssessmentStatus.RESULTS_PUBLISHED.name().equals(input.assessmentStatus());
    }
}
