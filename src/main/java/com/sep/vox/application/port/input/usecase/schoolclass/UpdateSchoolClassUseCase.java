package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolClassUseCase implements IUseCase<UpdateSchoolClassCommand, UpdateSchoolClassResponse> {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateSchoolClassUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public UpdateSchoolClassResponse execute(UpdateSchoolClassCommand input) {
        var command = normalize(input);
        validateCommand(command);
        var status = command.statusProvided() ? parseStatus(command.status()) : null;

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var updatedRows = schoolClassRepository.updateMutableFields(
            command.id(),
            schoolId,
            command.name(),
            command.nameProvided(),
            command.description(),
            command.descriptionProvided(),
            status,
            command.statusProvided(),
            now,
            currentUserId
        );
        if (updatedRows == 0) {
            throw new NotFoundException("Không tìm thấy lớp học");
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
            input.statusProvided() ? StringNormalization.trimAndCollapseSpaces(input.status()) : null,
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

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái lớp học không hợp lệ");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        return schoolUserRepository.findByUserId(currentUser.getId())
            .map(su -> su.getSchoolId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private SchoolClassStatus parseStatus(String status) {
        try {
            return SchoolClassStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Trạng thái lớp học không hợp lệ");
        }
    }
}
