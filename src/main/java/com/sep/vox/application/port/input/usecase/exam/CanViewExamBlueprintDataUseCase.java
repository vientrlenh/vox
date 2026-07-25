package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.CanViewExamBlueprintDataQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Trả lời "người dùng hiện tại có được xem blueprint/blueprintVersion gắn với
 * kỳ thi này không" cho SCHOOL_ADMIN cùng trường (hoặc bất kỳ ai cùng trường
 * sau khi kỳ thi đã đóng). Không bao gồm nhánh "là member của kỳ thi" -- nhánh
 * đó cần load danh sách member qua DataLoader nên vẫn nằm ở controller.
 */
@Service
public class CanViewExamBlueprintDataUseCase implements IUseCase<CanViewExamBlueprintDataQuery, Boolean> {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CanViewExamBlueprintDataUseCase(
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean execute(CanViewExamBlueprintDataQuery input) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(input.examSchoolId())) {
            return false;
        }

        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin) {
            return true;
        }

        // Sau khi kỳ thi đã đóng, ai cùng trường cũng xem được liên kết blueprint -- không cần là member nữa.
        return ExamStatus.CLOSED.name().equals(input.examStatus())
            || ExamStatus.RESULTS_PUBLISHED.name().equals(input.examStatus());
    }
}
