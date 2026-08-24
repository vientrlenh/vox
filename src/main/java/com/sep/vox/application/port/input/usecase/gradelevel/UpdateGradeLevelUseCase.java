package com.sep.vox.application.port.input.usecase.gradelevel;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateGradeLevelUseCase implements IUseCase<UpdateGradeLevelCommand, UUID> {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final GradeLevelRepository gradeLevelRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public UpdateGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ quản trị hệ thống mới được sửa khối học.");
        }

        if (gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        var name = command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        var description = command.description() != null
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;
        validateName(name);
        validateDescription(description);
        validateOrder(command.order());

        // Atomic update (null = giữ nguyên)
        int updatedRows;
        try {
            updatedRows = gradeLevelRepository.updateGradeLevelAtomic(
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
