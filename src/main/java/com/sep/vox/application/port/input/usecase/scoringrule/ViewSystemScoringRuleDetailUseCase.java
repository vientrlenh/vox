package com.sep.vox.application.port.input.usecase.scoringrule;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewScoringRuleDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.mapper.ScoringRuleDtoMapper;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSystemScoringRuleDetailUseCase implements IUseCase<ViewScoringRuleDetailQuery, ScoringRuleDto> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemScoringRuleDetailUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoringRuleDto execute(ViewScoringRuleDetailQuery query) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra Scoring Rule tồn tại
        ScoringRule rule = scoringRuleRepository.findById(query.ruleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Scoring Rule."));

        // 3. BẢO MẬT: policyId trên path phải khớp với policyId thật sự của rule
        if (!rule.getPolicyId().equals(query.policyId())) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }

        // 4. Kiểm tra Assessment Policy tồn tại và thuộc phạm vi toàn hệ thống
        var policy = assessmentPolicyRepository.findById(rule.getPolicyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể xem Scoring Rule của Assessment Policy thuộc trường học.");
        }

        return ScoringRuleDtoMapper.toDto(rule);
    }
}