package com.sep.vox.application.port.input.usecase.scoringrule;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.scoringrule.UpdateSchoolScoringRuleCommand;
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
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
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
public class UpdateSchoolScoringRuleUseCase implements IUseCase<UpdateSchoolScoringRuleCommand, UUID> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final ScoringRuleParamsMapper scoringRuleParamsMapper;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolScoringRuleCommand command) {
        requireActiveSchoolAdminOf(command.schoolId());
        ScoringRule rule = requireRuleOfPolicy(command.ruleId(), command.policyId());
        AssessmentPolicy policy = requirePolicyOfSchool(rule.getPolicyId(), command.schoolId());
        requireDraftPolicy(policy);
        requirePositivePriority(command.priority());

        // Convert the raw JSON Map -> the concrete Value Object matching the NEW conditionType/actionType
        ScoringRuleConditionParams conditionParams = scoringRuleParamsMapper.toConditionParams(command.conditionType(), command.conditionParams());
        ScoringRuleActionParams actionParams = scoringRuleParamsMapper.toActionParams(command.actionType(), command.actionParams());

        // Check that criterionCode/bandCode (if present) really exist in the Rubric Version of the Policy
        validateCriterionReferences(policy.getRubricVersionId(), policy.getFrameworkVersionId(), conditionParams, actionParams);

        // code and policyId are immutable and are left alone
        applyEdits(rule, command, conditionParams, actionParams);

        return scoringRuleRepository.save(rule).getId();
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

    private void requireActiveSchoolAdminOf(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not assigned to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to interfere with the Assessment Policy of another school.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled in the system.");
        }
    }

    private ScoringRule requireRuleOfPolicy(UUID ruleId, UUID policyId) {
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scoring Rule not found."));
        if (!rule.getPolicyId().equals(policyId)) {
            throw new ForbiddenException("SECURITY: This Scoring Rule does not belong to the specified Assessment Policy.");
        }
        return rule;
    }

    private AssessmentPolicy requirePolicyOfSchool(UUID policyId, UUID schoolId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to interfere with the Assessment Policy of another school.");
        }
        return policy;
    }

    private void requireDraftPolicy(AssessmentPolicy policy) {
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("A Scoring Rule can only be edited while the Assessment Policy is in DRAFT status.");
        }
    }

    private void requirePositivePriority(int priority) {
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be greater than 0.");
        }
    }

    private void applyEdits(ScoringRule rule, UpdateSchoolScoringRuleCommand command,
            ScoringRuleConditionParams conditionParams, ScoringRuleActionParams actionParams) {
        rule.setName(command.name().strip());
        if (command.descriptionProvided()) {
            rule.setDescription(command.description());
        }
        rule.setConditionType(command.conditionType());
        rule.setConditionParams(conditionParams);
        rule.setActionType(command.actionType());
        rule.setActionParams(actionParams);
        rule.setPriority(command.priority());
        rule.setSeverity(command.severity());
        rule.setStopProcessing(command.stopProcessing());
        rule.setActive(command.isActive());
    }

}
