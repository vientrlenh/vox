package com.sep.vox.application.port.input.usecase.schoolgradelevel;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CreateSchoolGradeLevelUseCase implements IUseCase<CreateSchoolGradeLevelCommand, UUID> {

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolGradeLevelUseCase(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        // 1. Kiểm tra User & Quyền truy cập
        validateUserAndAccess(currentUserId, command.schoolId());

        // 2. Kiểm tra sự tồn tại của trường
        if (!schoolRepository.existsById(command.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        // 3. Chuẩn hóa dữ liệu
        String normalizedCode = StringNormalization.normalizeCode(command.code());

        // 4. Kiểm tra ràng buộc duy nhất (Uniqueness)
        validateUniqueness(command.schoolId(), normalizedCode, command.order());

        // 5. Khởi tạo và Lưu DB
        return saveNewGradeLevel(command, normalizedCode, currentUserId);
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // Nếu là School Admin, phải check trùng schoolId
        schoolUserRepository.findSchoolIdByUserId(userId).ifPresent(userSchoolId -> {
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("Bạn không có quyền tạo khối học cho trường khác.");
            }
        });
    }

    private void validateUniqueness(UUID schoolId, String code, int order) {
        if (schoolGradeLevelRepository.existsBySchoolIdAndCode(schoolId, code)) {
            throw new DuplicatedException("Mã khối học đã tồn tại trong trường.");
        }
        if (schoolGradeLevelRepository.existsBySchoolIdAndOrder(schoolId, order)) {
            throw new DuplicatedException("Thứ tự khối học đã được sử dụng.");
        }
    }

    private UUID saveNewGradeLevel(CreateSchoolGradeLevelCommand command, String code, UUID creatorId) {
        OffsetDateTime now = OffsetDateTime.now();
        SchoolGradeLevel newGradeLevel = new SchoolGradeLevel(
                command.schoolId(),
                code,
                StringNormalization.trimAndCollapseSpaces(command.name()),
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.order(),
                SchoolGradeLevelStatus.ACTIVE,
                now, now, creatorId, creatorId
        );
        try {
            return schoolGradeLevelRepository.save(newGradeLevel).getId();
        } catch (DataIntegrityViolationException e) {
            // Chống race-condition: hai request cùng tạo trùng mã/thứ tự vượt qua check exists rồi mới đụng unique index.
            throw new DuplicatedException("Mã hoặc thứ tự khối học đã tồn tại trong trường.");
        }
    }
}