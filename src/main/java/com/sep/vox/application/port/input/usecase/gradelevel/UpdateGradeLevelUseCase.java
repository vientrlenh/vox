package com.sep.vox.application.port.input.usecase.gradelevel;

import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.gradelevel.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UpdateGradeLevelUseCase implements IUseCase<UpdateGradeLevelCommand, UUID> {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final GradeLevelRepository gradeLevelRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UUID execute(UpdateGradeLevelCommand command) {
        requireActiveSystemAdmin();

        if (gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Grade level not found.");
        }

        String name = command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String description = command.description() != null
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;
        validateName(name);
        validateDescription(description);
        validateOrder(command.order());

        int updatedRows = updateAtomically(command, name, description);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Grade level not found.");
        }

        return command.gradeLevelId();
    }

    private void requireActiveSystemAdmin() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Only a system admin can edit a grade level.");
        }
    }

    // Atomic update (null = keep the current value)
    private int updateAtomically(UpdateGradeLevelCommand command, String name, String description) {
        try {
            return gradeLevelRepository.updateGradeLevelAtomic(
                    command.gradeLevelId(),
                    name,
                    description,
                    command.descriptionProvided(),
                    command.order()
            );
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Grade level order is already in use.");
        }
    }

    private void validateName(String name) {
        if (name == null) {
            return;
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Grade level name must not be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Grade level name must not exceed 255 characters");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must not exceed 2048 characters");
        }
    }

    private void validateOrder(Integer order) {
        if (order != null && order <= 0) {
            throw new IllegalArgumentException("Grade level order must be greater than 0");
        }
    }
}
