package com.sep.vox.application.port.input.usecase.scoringrule;

import com.sep.vox.domain.model.user.User;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.scoringrule.CreateScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CriterionScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.SetFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

@Service
@RequiredArgsConstructor
public class CreateSystemScoringRuleUseCase implements IUseCase<CreateScoringRuleCommand, UUID> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final ScoringRuleParamsMapper scoringRuleParamsMapper;

    @Override
    @Transactional
    public UUID execute(CreateScoringRuleCommand command) {
        requireActiveAccount();
        AssessmentPolicy policy = requireSystemPolicy(command.policyId());
        requireDraftPolicy(policy);

        String safeCode = command.code().strip();
        requireCodeUnused(command.policyId(), safeCode);
        requirePositivePriority(command.priority());

        // Convert the raw JSON Map -> the concrete Value Object matching the chosen conditionType/actionType
        // (if the params do not match the expected structure, ScoringRuleParamsMapper throws IllegalArgumentException)
        ScoringRuleConditionParams conditionParams = scoringRuleParamsMapper.toConditionParams(command.conditionType(), command.conditionParams());
        ScoringRuleActionParams actionParams = scoringRuleParamsMapper.toActionParams(command.actionType(), command.actionParams());

        // Check that criterionCode/bandCode (if present) really exist in the Policy's Rubric Version
        validateCriterionReferences(policy.getRubricVersionId(), policy.getFrameworkVersionId(), conditionParams, actionParams);

        return scoringRuleRepository.save(toNewRule(command, safeCode, conditionParams, actionParams)).getId();
    }

    // Checks that the criterionCode/bandCode values (given as codes, not UUIDs) in the ScoringRule's conditionParams/actionParams
    // really exist in the Rubric Version of the Assessment Policy, and that frameworkResultBandId (if present)
    // really exists and belongs to the Framework Version of the Policy.
    private void validateCriterionReferences(UUID rubricVersionId, UUID frameworkVersionId,
            ScoringRuleConditionParams conditionParams, ScoringRuleActionParams actionParams) {
        switch (conditionParams) {
            case CriterionScoreThresholdParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            default -> {
            }
        }

        switch (actionParams) {
            case CapCriterionScoreParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case CriterionScoreDeltaParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case SetFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.frameworkResultBandId());
            case CapFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.maxResultBandId());
            default -> {
            }
        }
    }

    private RubricCriterion requireCriterion(UUID rubricVersionId, String criterionCode) {
        return rubricCriterionRepository.findByRubricVersionIdAndCode(rubricVersionId, criterionCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Criterion code (criterionCode) '" + criterionCode + "' does not exist in the Rubric Version of this Assessment Policy."));
    }

    private void requireFrameworkResultBand(UUID frameworkVersionId, UUID frameworkResultBandId) {
        FrameworkResultBand band = frameworkResultBandRepository.findById(frameworkResultBandId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Framework Result Band not found with ID '" + frameworkResultBandId + "'."));
        if (!band.getFrameworkVersionId().equals(frameworkVersionId)) {
            throw new ResourceNotFoundException(
                    "Framework Result Band '" + frameworkResultBandId + "' does not belong to the Framework Version of this Assessment Policy.");
        }
    }

    private void requireActiveAccount() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }
    }

    private AssessmentPolicy requireSystemPolicy(UUID policyId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Cannot interfere with the Assessment Policy of a school.");
        }
        return policy;
    }

    private void requireDraftPolicy(AssessmentPolicy policy) {
        // A Scoring Rule can only be added while the Policy is still DRAFT. Reason: a PUBLISHED Policy is
        // already grading real exams; adding a rule silently midway would make exams of the same Policy
        // be graded inconsistently (before/after the rule was added) with nobody able to control it.
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("A Scoring Rule can only be added while the Assessment Policy is in DRAFT status.");
        }
    }

    private void requireCodeUnused(UUID policyId, String safeCode) {
        if (scoringRuleRepository.existsByPolicyIdAndCode(policyId, safeCode)) {
            throw new IllegalArgumentException("Scoring Rule code '" + safeCode + "' already exists in this Assessment Policy.");
        }
    }

    private void requirePositivePriority(int priority) {
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be greater than 0.");
        }
    }

    private ScoringRule toNewRule(CreateScoringRuleCommand command, String safeCode,
            ScoringRuleConditionParams conditionParams, ScoringRuleActionParams actionParams) {
        return ScoringRule.forPolicy(
            command.policyId(),
            safeCode,
            command.name().strip(),
            command.description(),
            command.conditionType(),
            conditionParams,
            command.actionType(),
            actionParams,
            command.priority(),
            command.severity(),
            command.stopProcessing(),
            command.isActive());
    }

}