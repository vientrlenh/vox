package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolRubricVersionUseCase implements IUseCase<DeleteSchoolRubricVersionCommand, Void> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricVersionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        RubricVersion version = loadVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());
        requireDeletable(version);

        rubricCriterionRepository.deleteByRubricVersionId(version.getId());
        rubricResultBandRepository.deleteByRubricVersionId(version.getId());
        rubricVersionRepository.deleteById(version.getId());
        return null;
    }

    private RubricVersion loadVersionOwnedBySchool(DeleteSchoolRubricVersionCommand command, UUID currentUserId) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("This Rubric version was not found."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("SECURITY: you are not allowed to interfere with a version of another school.");
        }
        return version;
    }

    private void requireActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled in the system.");
        }
    }

    private void requireDeletable(RubricVersion version) {
        // Hard delete only while DRAFT; other statuses use the separate Archive function.
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Only a version in DRAFT status can be deleted. Use the Archive function if the version is PUBLISHED.");
        }

        // Do not delete while an Assessment Policy still points at the version.
        //
        // Since V44 one rubric version can be shared by several policies, so a wrong delete no longer
        // strands a single policy but a whole group of them. The missing guard was already wrong before
        // (no foreign key watches it); only the consequence is bigger now, so it must be blocked outright.
        if (assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())
                || assessmentPolicyRepository.existsNotPublishedByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException(
                    "Cannot delete this version because an Assessment Policy is using it. Delete or archive those policies first.");
        }
    }
}