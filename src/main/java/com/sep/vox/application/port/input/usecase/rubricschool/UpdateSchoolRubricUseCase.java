package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricUseCase implements IUseCase<UpdateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveUser(currentUserId);
        requireOwnedBySchoolAdmin(command, currentUserId);

        String finalName = normalizeName(command.name());
        // A blank description means keep the stored one (the SQL leaves it untouched); an explicit null clears it.
        // Same rule as UpdateSchoolRubricCriterionUseCase/UpdateSchoolRubricResultBandUseCase.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String finalDesc = (command.description() != null && !descriptionBlank)
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        rubricRepository.updateRubricAtomic(
                command.rubricId(),
                finalName,
                finalDesc,
                descriptionProvided
        );

        return command.rubricId();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String finalName = StringNormalization.trimAndCollapseSpaces(name);
        if (finalName.isBlank()) {
            throw new IllegalArgumentException("The Rubric name is invalid or empty.");
        }
        return finalName;
    }

    private void requireActiveUser(UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }
    }

    private void requireOwnedBySchoolAdmin(UpdateSchoolRubricCommand command, UUID currentUserId) {
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("This Rubric was not found."));

        // Security checkpoint: ownership is checked through SchoolUser.
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("SECURITY: you are not allowed to edit a Rubric of another school.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled in the system.");
        }
    }
}