package com.sep.vox.application.port.input.usecase.schoolgradelevel;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolGradeLevelUseCase implements IUseCase<UpdateSchoolGradeLevelCommand, UUID> {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public UpdateSchoolGradeLevelUseCase(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeLevelCommand command) {
        // 1. Kiểm tra tồn tại của Khối học
        SchoolGradeLevel gradeLevel = schoolGradeLevelRepository.findById(command.gradeLevelId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối học."));

        // 2. Validate User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // 3. Vòng bảo mật: kiểm tra quyền School User
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent() && !schoolUserOpt.get().getSchoolId().equals(gradeLevel.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác.");
        }

        // 4. Khối học phải thuộc đúng trường được chỉ định
        if (!gradeLevel.getSchoolId().equals(command.schoolId())) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        // 5. Validate dữ liệu đầu vào (chỉ khi được cung cấp)
        var name = command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        var description = command.description() != null
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;
        validateName(name);
        validateDescription(description);
        validateOrder(command.order());

        // 6. Atomic update (null = giữ nguyên)
        int updatedRows;
        try {
            updatedRows = schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
                    command.gradeLevelId(),
                    name,
                    description,
                    command.order(),
                    Instant.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Thứ tự khối học đã được sử dụng.");
        }

        if (updatedRows == 0) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        return command.gradeLevelId();
    }

    private void validateName(String name) {
        if (name == null) {
            return;
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tên khối học không được để trống");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên khối học không được vượt quá 255 ký tự");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 2048 ký tự");
        }
    }

    private void validateOrder(Integer order) {
        if (order != null && order <= 0) {
            throw new IllegalArgumentException("Thứ tự khối học phải lớn hơn 0");
        }
    }
}
