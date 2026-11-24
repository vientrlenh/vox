package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolGradeUseCase implements IUseCase<UpdateSchoolGradeCommand, UUID> {
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeCommand command) {
        // 1. Check that the school grade exists
        SchoolGrade grade = schoolGradeRepository.findById(command.schoolGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học/khóa học (School Grade)."));

        // 2. school_grades carries schoolId directly -- the grade level no longer has to serve as a "bridge".

        // 3. Validate the user (optimized with an exists call)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // SECURITY GUARD: check the school user's rights (a system admin skips it)
        if (!userContextPort.isSystemAdmin()) {
            SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác."));
            // Compare the user's SchoolId with the SchoolId of this school grade
            if (!schoolUser.getSchoolId().equals(grade.getSchoolId())) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác.");
            }
        }

        // 4. Validate the date logic
        LocalDate startDate = (command.startDate() != null) ? command.startDate() : grade.getStartDate();
        LocalDate endDate = (command.endDate() != null) ? command.endDate() : grade.getEndDate();

        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải diễn ra trước ngày kết thúc.");
        }

        // 5. Atomic update
        int updatedRows = schoolGradeRepository.updateSchoolGradeAtomic(
                command.schoolGradeId(),
                command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null,
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.descriptionProvided(),
                command.startDate(),
                command.endDate(),
                Instant.now(),
                currentUserId.toString()
        );

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Cập nhật thất bại hoặc không có thông tin nào thay đổi.");
        }

        return command.schoolGradeId();
    }
}