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

    // Kiểm tra criterionCode/bandCode (điền dạng code, không phải UUID) trong conditionParams/actionParams
    // của ScoringRule có thực sự tồn tại trong Rubric Version của Assessment Policy hay không, và
    // frameworkResultBandId (nếu có) có thực sự tồn tại và thuộc đúng Framework Version của Policy hay không.
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
                        "Mã tiêu chí (criterionCode) '" + criterionCode + "' không tồn tại trong Rubric Version của Assessment Policy này."));
    }

    private void requireFrameworkResultBand(UUID frameworkVersionId, UUID frameworkResultBandId) {
        FrameworkResultBand band = frameworkResultBandRepository.findById(frameworkResultBandId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Framework Result Band với ID '" + frameworkResultBandId + "'."));
        if (!band.getFrameworkVersionId().equals(frameworkVersionId)) {
            throw new ResourceNotFoundException(
                    "Framework Result Band '" + frameworkResultBandId + "' không thuộc Framework Version của Assessment Policy này.");
        }
    }

    private void requireActiveAccount() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }
    }

    private AssessmentPolicy requireSystemPolicy(UUID policyId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể can thiệp vào Assessment Policy của trường học.");
        }
        return policy;
    }

    private void requireDraftPolicy(AssessmentPolicy policy) {
        // A Scoring Rule can only be added while the Policy is still DRAFT. Reason: a PUBLISHED Policy is
        // already grading real exams; adding a rule silently midway would make exams of the same Policy
        // be graded inconsistently (before/after the rule was added) with nobody able to control it.
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được thêm Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }
    }

    private void requireCodeUnused(UUID policyId, String safeCode) {
        if (scoringRuleRepository.existsByPolicyIdAndCode(policyId, safeCode)) {
            throw new IllegalArgumentException("Mã Scoring Rule (code) '" + safeCode + "' đã tồn tại trong Assessment Policy này.");
        }
    }

    private void requirePositivePriority(int priority) {
        if (priority <= 0) {
            throw new IllegalArgumentException("Độ ưu tiên (priority) phải lớn hơn 0.");
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