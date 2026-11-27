package com.sep.vox.application.port.input.usecase.scoringrule;

import com.sep.vox.domain.model.user.User;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.scoringrule.DeleteScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeleteSystemScoringRuleUseCase implements IUseCase<DeleteScoringRuleCommand, Void> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteScoringRuleCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        ScoringRule rule = requireRuleForPolicy(command.ruleId(), command.policyId());

        AssessmentPolicy policy = requirePolicy(rule.getPolicyId());
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Cannot interfere with the Assessment Policy of a school.");
        }

        // Only delete a Scoring Rule while the Policy is still DRAFT (avoids silently deleting the scoring rules
        // of a PUBLISHED Policy that is grading real assessments)
        requireDraftStatus(policy);

        scoringRuleRepository.deleteById(rule.getId());
        return null;
    }

    private ScoringRule requireRuleForPolicy(UUID ruleId, UUID policyId) {
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scoring Rule not found."));
        if (!rule.getPolicyId().equals(policyId)) {
            throw new ForbiddenException("SECURITY: This Scoring Rule does not belong to the specified Assessment Policy.");
        }
        return rule;
    }

    private AssessmentPolicy requirePolicy(UUID policyId) {
        return assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
    }

    // Only delete a Scoring Rule while the Policy is still DRAFT (avoids silently deleting the scoring rules
    // of a PUBLISHED Policy that is grading real assessments). Call it after ownership/scope has been checked.
    private void requireDraftStatus(AssessmentPolicy policy) {
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("A Scoring Rule can only be deleted while the Assessment Policy is in DRAFT status.");
        }
    }
}