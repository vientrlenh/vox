package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.ArchiveSchoolRubricVersionCommand;
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
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveSchoolRubricVersionUseCase implements IUseCase<ArchiveSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    @Override
    @Transactional
    public UUID execute(ArchiveSchoolRubricVersionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        RubricVersion version = loadVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());
        requireArchivable(version);

        Instant now = Instant.now();
        version.setStatus(RubricStatus.ARCHIVED);
        // effectiveTo must not move before effectiveFrom when the version has not become effective yet.
        version.setEffectiveTo(now.isBefore(version.getEffectiveFrom()) ? version.getEffectiveFrom() : now);

        RubricVersion saved = rubricVersionRepository.save(version);
        return saved.getId();
    }

    private RubricVersion loadVersionOwnedBySchool(ArchiveSchoolRubricVersionCommand command, UUID currentUserId) {
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

    private void requireArchivable(RubricVersion version) {
        if (version.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a version in PUBLISHED status can be archived (ARCHIVE).");
        }

        // Block archiving while an Assessment Policy in DRAFT/PUBLISHED still uses this version: archiving
        // unconditionally would leave an effective Policy pointing at an ARCHIVED version. Mirrors
        // existsOtherActiveByRubricVersionId on the policy-archive side (ArchiveSchoolAssessmentPolicyUseCase).
        if (assessmentPolicyRepository.existsActiveByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException(
                    "Cannot archive this version because a DRAFT/PUBLISHED Assessment Policy still uses it. "
                            + "Archive those Assessment Policies first.");
        }
    }
}