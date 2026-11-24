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
        // 1. Authenticate the System Admin account
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        // 2. Load the original Rubric (to check that it exists and the permission)
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric này."));

        // 3. Security checkpoint
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }

        String finalName = null;
        if (command.name() != null) {
            finalName = StringNormalization.trimAndCollapseSpaces(command.name());
            if (finalName.isBlank()) {
                throw new IllegalArgumentException("Tên Rubric không hợp lệ hoặc bị để trống.");
            }
        }

        // A blank description means keep the stored one (the SQL leaves it untouched); an explicit null clears it.
        // Same rule as UpdateSystemRubricCriterionUseCase/UpdateSystemRubricResultBandUseCase.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String finalDesc = (command.description() != null && !descriptionBlank)
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        // 5. ATOMIC UPDATE WITH PLAIN SQL (using COALESCE)
        rubricRepository.updateRubricAtomic(
                command.rubricId(),
                finalName,
                finalDesc,
                descriptionProvided
        );

        // 6. Return the UUID
        return command.rubricId();
    }
}