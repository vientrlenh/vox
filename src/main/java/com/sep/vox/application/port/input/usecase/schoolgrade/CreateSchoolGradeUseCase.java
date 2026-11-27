package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolgrade.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CreateSchoolGradeUseCase implements IUseCase<CreateSchoolGradeCommand, UUID> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolGradeCommand command) {
        validateDates(command);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        checkUserAccess(currentUserId, command.schoolId());

        // The grade level catalogue is shared, so there is no school check any more: the level only has to exist and
        // not be soft-deleted.
        gradeLevelRepository.findById(command.gradeLevelId())
                .filter(gl -> gl.getStatus() == GradeLevelStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("The grade level does not exist or is no longer in use."));

        String normalizedCode = StringNormalization.normalizeCode(command.code());
        if (schoolGradeRepository.existsBySchoolIdAndGradeLevelIdAndCode(
                command.schoolId(), command.gradeLevelId(), normalizedCode)) {
            throw new ResourceDuplicatedException("The school year code already exists in this grade level.");
        }

        return saveNewGrade(command, normalizedCode, currentUserId);
    }

    private void validateDates(CreateSchoolGradeCommand command) {
        if (!command.startDate().isBefore(command.endDate())) {
            throw new IllegalArgumentException("The start date must be before the end date.");
        }
    }

    private void checkUserAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }

        UUID userSchoolId = schoolUserRepository.findSchoolIdByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("The user has not been assigned to any school."));

        if (!userSchoolId.equals(targetSchoolId)) {
            throw new ForbiddenException("You are not allowed to act on this school.");
        }
    }

    private UUID saveNewGrade(CreateSchoolGradeCommand command, String code, UUID creatorId) {
        SchoolGrade newGrade = SchoolGrade.opened(
            command.schoolId(),
            command.gradeLevelId(),
            code,
            StringNormalization.trimAndCollapseSpaces(command.name()),
            command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
            command.startDate(),
            command.endDate());
        try {
            return schoolGradeRepository.save(newGrade).getId();
        } catch (DataConflictException e) {
            // Race-condition guard: two requests that create the same code both pass the exists check and only then hit the unique index.
            throw new ResourceDuplicatedException("The school year code already exists in this grade level.");
        }
    }
}