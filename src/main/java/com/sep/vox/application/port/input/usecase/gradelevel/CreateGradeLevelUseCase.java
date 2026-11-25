package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.gradelevel.CreateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The grade level is a GLOBAL catalog, so only a system admin may create one -- this use case used to let
 * a school admin create a grade level just for their own school, but now one record affects every school.
 */
@Service
@RequiredArgsConstructor
public class CreateGradeLevelUseCase implements IUseCase<CreateGradeLevelCommand, UUID> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateSystemAdmin(currentUserId);

        String normalizedCode = StringNormalization.normalizeCode(command.code());
        validateUniqueness(normalizedCode, command.order());

        return saveNewGradeLevel(command, normalizedCode, currentUserId);
    }

    private void validateSystemAdmin(UUID userId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ quản trị hệ thống mới được tạo khối học.");
        }
    }

    private void validateUniqueness(String code, Integer order) {
        if (gradeLevelRepository.existsByCode(code)) {
            throw new ResourceDuplicatedException("Mã khối học đã tồn tại.");
        }
        if (order != null && gradeLevelRepository.existsByOrder(order)) {
            throw new ResourceDuplicatedException("Thứ tự khối học đã được sử dụng.");
        }
    }

    private UUID saveNewGradeLevel(CreateGradeLevelCommand command, String code, UUID creatorId) {
        GradeLevel newGradeLevel = GradeLevel.registered(
            code,
            StringNormalization.trimAndCollapseSpaces(command.name()),
            command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
            command.order());
        try {
            return gradeLevelRepository.save(newGradeLevel).getId();
        } catch (DataConflictException e) {
            // Race-condition guard: two requests that create the same code/order both pass the exists check and only then hit the unique index.
            throw new ResourceDuplicatedException("Mã hoặc thứ tự khối học đã tồn tại.");
        }
    }
}
