package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateSystemRubricCriterionUseCase implements IUseCase<UpdateSystemRubricCriterionCommand, UUID> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricCriterionExamplesNormalizer examplesNormalizer;

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricCriterionCommand command) {
        requireActiveCurrentUser();

        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found."));
        RubricVersion version = loadDraftVersionOfSystemRubric(criterion);

        // The code must not change after creation, so it is never updated.
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        // A blank description keeps the stored one; an explicit null clears it.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String safeDesc = (command.description() != null && !descriptionBlank) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;
        requireValidWeightAndOrder(command);
        requireValidScoresAndOrder(command, criterion, version, safeName);

        // Validate the examples JSON (guarded by the RubricCriterionExamples value object) and rewrite it in
        // the {"values": [...]} shape before saving to the database.
        String examplesJsonToPersist = examplesNormalizer.normalize(command.examplesJson());
        updateCriterionAtomically(command, safeName, safeDesc, descriptionProvided, examplesJsonToPersist);

        return command.criterionId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }
    }

    private RubricVersion loadDraftVersionOfSystemRubric(RubricCriterion criterion) {
        RubricVersion version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The Rubric Version containing this criterion was not found."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be edited while the Rubric version is in DRAFT status.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Action denied: this criterion belongs to a School.");
        }
        return version;
    }

    private void requireValidWeightAndOrder(UpdateSystemRubricCriterionCommand command) {
        // Guard added 2026-08-14: see the reason in CreateSchoolRubricCriterionUseCase -- the REST path and the
        // file-import path read "20%" in two ways that differ by a factor of 100.
        if (command.weight() != null
                && (command.weight().compareTo(BigDecimal.ZERO) < 0
                        || command.weight().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(
                    "The weight must be between 0 and 1 -- stored as a fraction,"
                            + " (0.2 = 20%, 1 = 100%).");
        }
        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("The order must be greater than 0.");
        }
    }

    private void requireValidScoresAndOrder(UpdateSystemRubricCriterionCommand command, RubricCriterion criterion,
                                            RubricVersion version, String safeName) {
        BigDecimal finalMinScore = command.minScore() != null ? command.minScore() : criterion.getMinScore();
        BigDecimal finalMaxScore = command.maxScore() != null ? command.maxScore() : criterion.getMaxScore();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("The minimum score (minScore) must not be greater than the maximum score (maxScore).");
        }

        String nameForError = safeName != null ? safeName : criterion.getName();
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);

        if (command.order() != null) {
            Set<Integer> siblingOrders = rubricCriterionRepository.findByRubricVersionId(criterion.getRubricVersionId())
                    .stream()
                    .filter(c -> !c.getId().equals(criterion.getId()))
                    .map(c -> c.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }
    }

    private void updateCriterionAtomically(UpdateSystemRubricCriterionCommand command, String safeName, String safeDesc,
                                           boolean descriptionProvided, String examplesJsonToPersist) {
        try {
            rubricCriterionRepository.updateCriterionAtomic(
                    command.criterionId(),
                    null,
                    safeName,
                    safeDesc,
                    descriptionProvided,
                    examplesJsonToPersist,
                    command.examplesJsonProvided(),
                    command.weight(),
                    command.minScore(),
                    command.maxScore(),
                    command.order(),
                    command.isRequired()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Cannot update the criterion because the data is duplicated or invalid.");
        }
    }
}
