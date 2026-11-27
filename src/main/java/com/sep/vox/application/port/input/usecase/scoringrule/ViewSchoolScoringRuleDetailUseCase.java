package com.sep.vox.application.port.input.usecase.scoringrule;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.scoringrule.ScoringRuleDtoMapper;
import com.sep.vox.application.port.input.query.scoringrule.ViewSchoolScoringRuleDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolScoringRuleDetailUseCase implements IUseCase<ViewSchoolScoringRuleDetailQuery, ScoringRuleDto> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final ScoringRuleDtoMapper scoringRuleDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public ScoringRuleDto execute(ViewSchoolScoringRuleDetailQuery query) {
        requireActiveSchoolAdminOf(query.schoolId());
        ScoringRule rule = requireRuleOfPolicy(query.ruleId(), query.policyId());
        requirePolicyOfSchool(rule.getPolicyId(), query.schoolId());

        return scoringRuleDtoMapper.toDto(rule);
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
            throw new ForbiddenException("SECURITY: You are not allowed to view the Scoring Rules of another school.");
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

    private void requirePolicyOfSchool(UUID policyId, UUID schoolId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to view the Scoring Rules of another school.");
        }
    }

}
