package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolGradeUseCase implements IUseCase<UpdateSchoolGradeCommand, UUID> {
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository; // Bổ sung repo cầu nối
    private final SchoolUserRepository schoolUserRepository; // Bổ sung repo bảo mật
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public UpdateSchoolGradeUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeCommand command) {
        // 1. Kiểm tra tồn tại của School Grade
        SchoolGrade grade = schoolGradeRepository.findById(command.schoolGradeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy năm học/khóa học (School Grade)."));

        // 2. Lấy School Grade Level làm "cầu nối" để xác định School ID của cục dữ liệu này
        var gradeLevel = schoolGradeLevelRepository.findById(grade.getSchoolGradeLevelId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối Lớp chứa năm học này."));

        // 3. Validate User (Tối ưu bằng hàm exists)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // VÒNG BẢO MẬT: KIỂM TRA QUYỀN SCHOOL USER (system admin bỏ qua)
        if (!userContextPort.isSystemAdmin()) {
            SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác."));
            // So sánh SchoolId của người dùng với SchoolId của Khối Lớp chứa năm học này
            if (!schoolUser.getSchoolId().equals(gradeLevel.getSchoolId())) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác.");
            }
        }

        // 4. Validate logic ngày tháng
        LocalDate startDate = (command.startDate() != null) ? command.startDate() : grade.getStartDate();
        LocalDate endDate = (command.endDate() != null) ? command.endDate() : grade.getEndDate();

        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải diễn ra trước ngày kết thúc.");
        }

        // 5. Atomic Update
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
            throw new NotFoundException("Cập nhật thất bại hoặc không có thông tin nào thay đổi.");
        }

        return command.schoolGradeId();
    }
}