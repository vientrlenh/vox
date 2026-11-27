package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolRubricUseCase implements IUseCase<DeleteSchoolRubricCommand, Void> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("This Rubric was not found."));
        requireOwnedBySchoolOfUser(rubric, command, currentUserId);
        requireActiveSchool(command.schoolId());

        // Hard delete is only allowed while the Rubric has no version: RubricCriterion/RubricResultBand are
        // always children of a RubricVersion (never attached directly to the Rubric), so having no version
        // already implies no criterion/result band, and the two tables need no extra query.
        if (!rubricVersionRepository.findByRubricId(rubric.getId()).isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot delete this Rubric because it still contains versions. "
                    + "Delete all DRAFT versions or archive the PUBLISHED versions first.");
        }

        rubricRepository.deleteById(rubric.getId());
        return null;
    }

    // Security gate through SchoolUser, same pattern as UpdateSchoolRubricUseCase.
    private void requireOwnedBySchoolOfUser(Rubric rubric, DeleteSchoolRubricCommand command, UUID currentUserId) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("SECURITY: you are not allowed to delete a Rubric of another school.");
        }
    }

    private void requireActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled in the system.");
        }
    }
}
