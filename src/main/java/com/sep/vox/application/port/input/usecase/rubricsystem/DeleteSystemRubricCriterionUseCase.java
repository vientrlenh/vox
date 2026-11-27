package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSystemRubricCriterionUseCase implements IUseCase<DeleteSystemRubricCriterionCommand, Void> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricCriterionCommand command) {
        requireActiveCurrentUser();
        RubricVersion version = loadDraftVersionOfSystemRubric(command.versionId());

        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found."));
        if (!criterion.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("The criterion belongs to a different version.");
        }

        rubricCriterionRepository.deleteById(criterion.getId());

        return null;
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Account error."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");
    }

    private RubricVersion loadDraftVersionOfSystemRubric(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Can only be deleted while DRAFT.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Cannot delete data of a school.");
        }
        return version;
    }
}
