package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UpdateSchoolClassUseCase implements IUseCase<UpdateSchoolClassCommand, UpdateSchoolClassResponse> {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public UpdateSchoolClassResponse execute(UpdateSchoolClassCommand input) {
        UpdateSchoolClassCommand command = normalize(input);
        validateCommand(command);
        SchoolClassStatus status = command.status();

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        int updatedRows = schoolClassRepository.updateMutableFields(
            command.id(),
            schoolId,
            command.name(),
            command.nameProvided(),
            command.description(),
            command.descriptionProvided(),
            status,
            command.statusProvided(),
            now,
            currentUserId.toString()
        );
        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }

        return new UpdateSchoolClassResponse(command.id());
    }

    private UpdateSchoolClassCommand normalize(UpdateSchoolClassCommand input) {
        return new UpdateSchoolClassCommand(
            input.id(),
            input.nameProvided() ? StringNormalization.trimAndCollapseSpaces(input.name()) : null,
            input.nameProvided(),
            input.descriptionProvided() && input.description() != null
                ? StringNormalization.trimAndCollapseSpaces(input.description())
                : null,
            input.descriptionProvided(),
            input.statusProvided() ? input.status() : null,
            input.statusProvided()
        );
    }

    private void validateCommand(UpdateSchoolClassCommand command) {
        if (!command.nameProvided() && !command.descriptionProvided() && !command.statusProvided()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một trường để cập nhật");
        }
        if (command.nameProvided()) {
            validateName(command.name());
        }
        if (command.descriptionProvided() && command.description() != null) {
            validateDescription(command.description());
        }
        if (command.statusProvided()) {
            validateStatus(command.status());
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên lớp học không được để trống");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên lớp học không được vượt quá 255 ký tự");
        }
    }

    private void validateDescription(String description) {
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 2048 ký tự");
        }
    }

    private void validateStatus(SchoolClassStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái lớp học không hợp lệ");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học không hoạt động");
        }
    }
}
