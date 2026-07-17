package com.sep.vox.application.common;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;

// Kiểm tra dùng chung bởi DeleteSchoolScoringRuleUseCase và DeleteSystemScoringRuleUseCase:
// (1) policyId trên path phải khớp với policyId thật sự của rule (chống truy cập chéo qua path param),
// (2) chỉ được xóa Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT (tránh xóa ngầm luật
// chấm điểm của 1 Policy đã PUBLISHED đang chấm bài thi thật).
@Component
public class ScoringRuleDeletionGuard {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    public ScoringRuleDeletionGuard(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
    }

    public ScoringRule requireRuleForPolicy(UUID ruleId, UUID policyId) {
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Scoring Rule."));
        if (!rule.getPolicyId().equals(policyId)) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }
        return rule;
    }

    public AssessmentPolicy requirePolicy(UUID policyId) {
        return assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
    }

    // Chỉ được xóa Scoring Rule khi Policy còn DRAFT (tránh xóa ngầm luật chấm điểm
    // của 1 Policy đã PUBLISHED đang chấm bài thi thật). Gọi sau khi đã kiểm tra quyền sở hữu/phạm vi.
    public void requireDraftStatus(AssessmentPolicy policy) {
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xóa Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }
    }

    public void delete(ScoringRule rule) {
        scoringRuleRepository.deleteById(rule.getId());
    }
}