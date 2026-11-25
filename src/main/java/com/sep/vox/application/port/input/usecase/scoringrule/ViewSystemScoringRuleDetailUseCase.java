package com.sep.vox.application.port.input.usecase.scoringrule;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.user.User;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.scoringrule.ScoringRuleDtoMapper;
import com.sep.vox.application.port.input.query.scoringrule.ViewScoringRuleDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSystemScoringRuleDetailUseCase implements IUseCase<ViewScoringRuleDetailQuery, ScoringRuleDto> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final ScoringRuleDtoMapper scoringRuleDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public ScoringRuleDto execute(ViewScoringRuleDetailQuery query) {
        requireActiveAccount();
        ScoringRule rule = requireRuleOfPolicy(query.ruleId(), query.policyId());
        requireSystemPolicy(rule.getPolicyId());

        return scoringRuleDtoMapper.toDto(rule);
    }

    private void requireActiveAccount() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }
    }

    private ScoringRule requireRuleOfPolicy(UUID ruleId, UUID policyId) {
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Scoring Rule."));
        if (!rule.getPolicyId().equals(policyId)) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }
        return rule;
    }

    private void requireSystemPolicy(UUID policyId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể xem Scoring Rule của Assessment Policy thuộc trường học.");
        }
    }

}