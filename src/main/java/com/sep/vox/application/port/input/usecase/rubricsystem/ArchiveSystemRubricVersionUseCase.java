package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.ArchiveSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveSystemRubricVersionUseCase implements IUseCase<ArchiveSystemRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    @Override
    @Transactional
    public UUID execute(ArchiveSystemRubricVersionCommand command) {
        requireActiveCurrentUser();
        RubricVersion version = loadSystemOwnedVersion(command.versionId());
        requireArchivable(version);

        Instant now = Instant.now();
        version.setStatus(RubricStatus.ARCHIVED);
        // effectiveTo must not move before effectiveFrom when the version has not reached its effective date yet.
        version.setEffectiveTo(now.isBefore(version.getEffectiveFrom()) ? version.getEffectiveFrom() : now);

        RubricVersion saved = rubricVersionRepository.save(version);
        return saved.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }
    }

    private RubricVersion loadSystemOwnedVersion(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("This Rubric version was not found."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Action denied: cannot interfere with a version of a school.");
        }
        return version;
    }

    private void requireArchivable(RubricVersion version) {
        if (version.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a version in PUBLISHED status can be archived (ARCHIVE).");
        }

        // Archiving unconditionally would leave an active Policy pointing at an ARCHIVED version. Symmetric
        // with existsOtherActiveByRubricVersionId on the Policy archive side (ArchiveSystemAssessmentPolicyUseCase).
        if (assessmentPolicyRepository.existsActiveByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException(
                    "Cannot archive this version because a DRAFT/PUBLISHED Assessment Policy still uses it. "
                            + "Archive those Assessment Policies first.");
        }
    }
}
