package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeSystemRubricVersionStatusUseCase implements IUseCase<ChangeSystemRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(ChangeSystemRubricVersionStatusCommand command) {
        requireActiveCurrentUser();

        RubricVersion version = loadVersionToChange(command);
        Rubric rubric = loadSystemOwnedRubric(version);
        requireTransitionAllowed(command.status(), version, rubric);

        version.setStatus(command.status());

        rubricVersionRepository.save(version);
        return version.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Account error."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");
    }

    private RubricVersion loadVersionToChange(ChangeSystemRubricVersionStatusCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));

        if (version.getStatus() == command.status()) {
            throw new IllegalArgumentException("The version is already in status " + command.status() + ".");
        }
        return version;
    }

    private Rubric loadSystemOwnedRubric(RubricVersion version) {
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Action denied: cannot change the status of a Rubric that belongs to a school.");
        }
        return rubric;
    }

    private void requireTransitionAllowed(RubricStatus targetStatus, RubricVersion version, Rubric rubric) {
        if (targetStatus == RubricStatus.PUBLISHED) {
            requirePublishable(version, rubric);
        } else if (targetStatus == RubricStatus.ARCHIVED) {
            throw new IllegalArgumentException("Action denied: use the separate Archive function to move a version to ARCHIVED.");
        } else if (targetStatus == RubricStatus.DRAFT) {
            // Block the loophole of moving a version back to an earlier status
            throw new IllegalArgumentException("Action denied: a published/archived version cannot go back to DRAFT status. Please create a new version.");
        }
    }

    private void requirePublishable(RubricVersion version, Rubric rubric) {
        requireDraftWithCriteria(version);
        requireActiveFramework(rubric);

        RubricScoringConsistencyValidator.assertPublishable(
                version.getTotalScoreMethod(),
                version.getScoringScaleMin(),
                version.getScoringScaleMax(),
                rubricCriterionRepository.findByRubricVersionId(version.getId()));
    }

    private void requireDraftWithCriteria(RubricVersion version) {
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Only a version in DRAFT status can be published (PUBLISH).");
        }

        // A version without criteria or a scoring scale cannot grade anything: once published it would be
        // useless and could no longer be edited (only archived).
        if (rubricCriterionRepository.findByRubricVersionId(version.getId()).isEmpty()) {
            throw new IllegalArgumentException("Cannot publish this version because it has no criterion yet.");
        }
    }

    private void requireActiveFramework(Rubric rubric) {
        Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                .orElseThrow(() -> new ResourceNotFoundException("The linked Framework was not found."));
        if (!framework.isActive()) {
            throw new IllegalArgumentException("Cannot publish this Rubric because its parent Framework is disabled.");
        }
    }
}
