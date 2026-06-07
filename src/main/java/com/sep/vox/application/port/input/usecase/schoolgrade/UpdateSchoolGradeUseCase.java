package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolGradeUseCase implements IUseCase<UpdateSchoolGradeCommand, UUID> {
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public UpdateSchoolGradeUseCase(SchoolGradeRepository schoolGradeRepository, SchoolRepository schoolRepository, UserContextPort userContextPort, UserRepository userRepository) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeCommand command) {
        // 1. Kiểm tra tồn tại
        SchoolGrade grade = schoolGradeRepository.findById(command.schoolGradeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp."));

        // 2. Validate quyền
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(grade.getSchoolId())) {
            throw new ForbiddenException("Bạn không có quyền sửa khối lớp của trường khác.");
        }

        // 3. Validate logic ngày tháng
        LocalDate startDate = (command.startDate() != null) ? command.startDate() : grade.getStartDate();
        LocalDate endDate = (command.endDate() != null) ? command.endDate() : grade.getEndDate();

        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }

        // 4. Atomic Update
        int updatedRows = schoolGradeRepository.updateSchoolGradeAtomic(
                command.schoolGradeId(),
                command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null,
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.startDate(),
                command.endDate(),
                OffsetDateTime.now(),
                currentUserId
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Cập nhật thất bại.");
        }

        return command.schoolGradeId();
    }
}