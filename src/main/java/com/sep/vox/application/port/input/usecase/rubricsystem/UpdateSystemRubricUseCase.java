package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSystemRubricUseCase implements IUseCase<UpdateSystemRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricCommand command) {
        requireActiveCurrentUser();
        loadSystemOwnedRubric(command.rubricId());

        String finalName = normalizeNameWhenProvided(command.name());

        // A blank description means keep the stored one (the SQL leaves it untouched); an explicit null clears it.
        // Same rule as UpdateSystemRubricCriterionUseCase/UpdateSystemRubricResultBandUseCase.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String finalDesc = (command.description() != null && !descriptionBlank)
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        // Atomic update with plain SQL (using COALESCE).
        rubricRepository.updateRubricAtomic(
                command.rubricId(),
                finalName,
                finalDesc,
                descriptionProvided
        );

        return command.rubricId();
    }

    private String normalizeNameWhenProvided(String name) {
        if (name == null) {
            return null;
        }
        String finalName = StringNormalization.trimAndCollapseSpaces(name);
        if (finalName.isBlank()) {
            throw new IllegalArgumentException("Tên Rubric không hợp lệ hoặc bị để trống.");
        }
        return finalName;
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }
    }

    private void loadSystemOwnedRubric(UUID rubricId) {
        Rubric rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric này."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }
    }
}
