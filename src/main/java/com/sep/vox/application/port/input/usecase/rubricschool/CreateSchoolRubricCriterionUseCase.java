package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateSchoolRubricCriterionUseCase implements IUseCase<CreateSchoolRubricCriteriaCommand, List<UUID>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricCriteriaCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account has been locked.");
        }

        RubricVersion version = loadDraftVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());

        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();
        Set<Integer> ordersSoFar = collectStoredOrders(command.versionId());

        List<RubricCriterion> criteriaToSave = command.criteria().stream()
                .map(cCmd -> toCriterion(version, cCmd, uniqueCodes, uniqueFrameworkIds, ordersSoFar))
                .collect(Collectors.toList());

        saveCriteria(criteriaToSave);

        return criteriaToSave.stream().map(c -> c.getId()).collect(Collectors.toList());
    }

    // Orders already stored in the version plus the new ones of this batch, to reject duplicate orders.
    private Set<Integer> collectStoredOrders(UUID versionId) {
        Set<Integer> ordersSoFar = new HashSet<>();
        rubricCriterionRepository.findByRubricVersionId(versionId)
                .forEach(c -> ordersSoFar.add(c.getOrder()));
        return ordersSoFar;
    }

    private void saveCriteria(List<RubricCriterion> criteriaToSave) {
        try {
            rubricCriterionRepository.saveAll(criteriaToSave);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Data save error: the criterion code, Framework or order already exists in this version.");
        }
    }

    private RubricVersion loadDraftVersionOwnedBySchool(CreateSchoolRubricCriteriaCommand command, UUID currentUserId) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("This Rubric version was not found."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be added while the Rubric is in DRAFT status.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));

        if (!rubric.getSchoolId().equals(command.schoolId()) || !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("SECURITY: you are not allowed to edit a Rubric of another school.");
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

    private RubricCriterion toCriterion(RubricVersion version,
                                        CreateSchoolRubricCriteriaCommand.CriterionItemCommand cCmd,
                                        Set<String> uniqueCodes, Set<UUID> uniqueFrameworkIds,
                                        Set<Integer> ordersSoFar) {
        String safeCode = StringNormalization.trimAndCollapseSpaces(cCmd.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(cCmd.name());

        requireUniqueInBatch(cCmd, safeCode, safeName, uniqueCodes, uniqueFrameworkIds);
        requireValidScoresAndWeight(cCmd, safeCode);
        RubricOrderValidator.assertNoDuplicateOrder(ordersSoFar, cCmd.order(), safeName);
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                cCmd.minScore(), cCmd.maxScore(), safeName);

        return RubricCriterion.create(
                version.getId(), cCmd.frameworkCriterionId(), safeCode, safeName,
                cCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(cCmd.description()) : null,
                toExamples(cCmd), cCmd.weight(), cCmd.minScore(), cCmd.maxScore(), cCmd.order(), cCmd.isRequired());
    }

    private void requireUniqueInBatch(CreateSchoolRubricCriteriaCommand.CriterionItemCommand cCmd,
                                      String safeCode, String safeName,
                                      Set<String> uniqueCodes, Set<UUID> uniqueFrameworkIds) {
        if (!uniqueCodes.add(safeCode)) {
            throw new IllegalArgumentException("The submitted data repeats the criterion code (Code): " + safeCode);
        }

        if (!uniqueFrameworkIds.add(cCmd.frameworkCriterionId())) {
            throw new IllegalArgumentException("The submitted data repeats the Framework Criterion for the criterion: " + safeName);
        }
    }

    private void requireValidScoresAndWeight(CreateSchoolRubricCriteriaCommand.CriterionItemCommand cCmd, String safeCode) {
        if (cCmd.minScore().compareTo(cCmd.maxScore()) > 0) {
            throw new IllegalArgumentException("Criterion '" + safeCode + "': the floor score must not be greater than the ceiling score.");
        }
        // Guard added 2026-08-14: it used to check only >= 0, so the REST path took a percentage (20)
        // directly while the file import path divides by 100 before saving (see
        // RubricCriterionImportCommitHandler). The same "20%" produced two values 100 times apart, and
        // it only blew up at publish time when the weights did not add up.
        if (cCmd.weight() != null
                && (cCmd.weight().compareTo(BigDecimal.ZERO) < 0
                        || cCmd.weight().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Criterion '" + safeCode
                    + "': the weight must be between 0 and 1 -- stored as a fraction,"
                    + " (0.2 = 20%, 1 = 100%).");
        }
    }

    private RubricCriterionExamples toExamples(CreateSchoolRubricCriteriaCommand.CriterionItemCommand cCmd) {
        if (cCmd.examples() == null || cCmd.examples().isEmpty()) {
            return null;
        }
        List<RubricCriterionExample> exampleList = cCmd.examples().stream()
                .map(exCmd -> new RubricCriterionExample(exCmd.transcript(), exCmd.explanation(), exCmd.expectedScore()))
                .toList();
        return new RubricCriterionExamples(exampleList);
    }
}