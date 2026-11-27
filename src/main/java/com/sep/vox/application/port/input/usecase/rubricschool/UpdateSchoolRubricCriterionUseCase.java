package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
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
public class UpdateSchoolRubricCriterionUseCase implements IUseCase<UpdateSchoolRubricCriterionCommand, UUID> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RubricCriterionExamplesNormalizer examplesNormalizer;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricCriterionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveSchoolMember(currentUserId, command.schoolId());

        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found."));
        RubricVersion version = loadDraftVersionOwnedBySchool(criterion, command.schoolId());

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

    private void requireActiveSchoolMember(UUID currentUserId, UUID schoolId) {
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("The account is not linked to a school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: you are not allowed to edit data of another school.");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("This school is disabled.");
        }
    }

    // The Criterion must belong to a Rubric of the caller's school.
    private RubricVersion loadDraftVersionOwnedBySchool(RubricCriterion criterion, UUID schoolId) {
        RubricVersion version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The Rubric Version containing this criterion was not found."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be edited while the Rubric version is in DRAFT status.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Action denied: this criterion does not belong to your school.");
        }
        return version;
    }

    private void requireValidWeightAndOrder(UpdateSchoolRubricCriterionCommand command) {
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

    private void requireValidScoresAndOrder(UpdateSchoolRubricCriterionCommand command, RubricCriterion criterion,
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

    private void updateCriterionAtomically(UpdateSchoolRubricCriterionCommand command, String safeName, String safeDesc,
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