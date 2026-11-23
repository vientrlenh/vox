package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolgrade.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository; // Bổ sung
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CreateSchoolGradeUseCase implements IUseCase<CreateSchoolGradeCommand, UUID> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolGradeCommand command) {
        // 1. Kiểm tra ngày tháng
        validateDates(command);

        // 2. Validate User & Security
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        checkUserAccess(currentUserId, command.schoolId());

        // 3. Validate Khối lớp -- catalog dùng chung nên không còn kiểm tra thuộc trường nào,
        //    chỉ cần khối tồn tại và chưa bị xóa mềm.
        gradeLevelRepository.findById(command.gradeLevelId())
                .filter(gl -> gl.getStatus() == GradeLevelStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Khối lớp không tồn tại hoặc đã ngừng sử dụng."));

        // 4. Kiểm tra trùng mã trong phạm vi (trường, khối)
        String normalizedCode = StringNormalization.normalizeCode(command.code());
        if (schoolGradeRepository.existsBySchoolIdAndGradeLevelIdAndCode(
                command.schoolId(), command.gradeLevelId(), normalizedCode)) {
            throw new ResourceDuplicatedException("Mã năm học đã tồn tại trong Khối lớp này.");
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
        SchoolGrade newGrade = SchoolGrade.builder()
            .schoolId(command.schoolId())
            .gradeLevelId(command.gradeLevelId())
            .code(code)
            .name(StringNormalization.trimAndCollapseSpaces(command.name()))
            .description(command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null)
            .startDate(command.startDate())
            .endDate(command.endDate())
            .status(SchoolGradeStatus.ACTIVE)
            .build();
        try {
            return schoolGradeRepository.save(newGrade).getId();
        } catch (DataConflictException e) {
            // Race-condition guard: two requests that create the same code both pass the exists check and only then hit the unique index.
            throw new ResourceDuplicatedException("Mã năm học đã tồn tại trong Khối lớp này.");
        }
    }
}