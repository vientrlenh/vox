package com.sep.vox.application.port.input.usecase.scoringrule;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteScoringRuleCommand;
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
public class DeleteSystemScoringRuleUseCase implements IUseCase<DeleteScoringRuleCommand, Void> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemScoringRuleUseCase(
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
    @Transactional
    public Void execute(DeleteScoringRuleCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra Scoring Rule tồn tại
        ScoringRule rule = scoringRuleRepository.findById(command.ruleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Scoring Rule."));

        // 3. BẢO MẬT: policyId trên path phải khớp với policyId thật sự của rule
        if (!rule.getPolicyId().equals(command.policyId())) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }

        // 4. Kiểm tra Assessment Policy tồn tại và thuộc phạm vi toàn hệ thống
        AssessmentPolicy policy = assessmentPolicyRepository.findById(rule.getPolicyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể can thiệp vào Assessment Policy của trường học.");
        }

        // 5. Chỉ được xóa Scoring Rule khi Policy còn DRAFT (tránh xóa ngầm luật chấm điểm
        // của 1 Policy đã PUBLISHED đang chấm bài thi thật)
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xóa Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }

        scoringRuleRepository.deleteById(rule.getId());
        return null;
    }
}