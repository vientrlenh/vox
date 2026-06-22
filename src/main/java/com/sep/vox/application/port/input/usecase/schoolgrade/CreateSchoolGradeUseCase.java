package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository; // Bổ sung
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository; // Bổ sung
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;


@Service
public class CreateSchoolGradeUseCase implements IUseCase<CreateSchoolGradeCommand, UUID> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolGradeUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolGradeCommand command) {
        // 1. Kiểm tra ngày tháng
        validateDates(command);

        // 2. Validate User & Security
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        checkUserAccess(currentUserId, command.schoolId());

        // 3. Validate Khối lớp (Grade Level) thuộc về trường
        var gradeLevel = schoolGradeLevelRepository.findById(command.schoolGradeLevelId())
                .filter(gl -> gl.getSchoolId().equals(command.schoolId()))
                .orElseThrow(() -> new NotFoundException("Khối lớp không tồn tại hoặc không thuộc trường này."));

        // 4. Kiểm tra trùng mã
        String normalizedCode = StringNormalization.normalizeCode(command.code());
        if (schoolGradeRepository.existsBySchoolGradeLevelIdAndCode(command.schoolGradeLevelId(), normalizedCode)) {
            throw new DuplicatedException("Mã năm học đã tồn tại trong Khối lớp này.");
        }

        // 5. Lưu và trả về UUID
        return saveNewGrade(command, normalizedCode, currentUserId);
    }

    private void validateDates(CreateSchoolGradeCommand command) {
        if (!command.startDate().isBefore(command.endDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }
    }

    private void checkUserAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // Dùng phương thức đã tối ưu để lấy schoolId
        UUID userSchoolId = schoolUserRepository.findSchoolIdByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Người dùng chưa được gán vào trường học nào."));

        if (!userSchoolId.equals(targetSchoolId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên trường học này.");
        }
    }

    private UUID saveNewGrade(CreateSchoolGradeCommand command, String code, UUID creatorId) {
        OffsetDateTime now = OffsetDateTime.now();
        SchoolGrade newGrade = new SchoolGrade(
                command.schoolGradeLevelId(),
                code,
                StringNormalization.trimAndCollapseSpaces(command.name()),
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.startDate(),
                command.endDate(),
                SchoolGradeStatus.INACTIVE,
                now, now, creatorId, creatorId
        );
        return schoolGradeRepository.save(newGrade).getId();
    }
}