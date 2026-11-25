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
import com.sep.vox.application.port.input.command.scoringrule.DeleteSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeleteSchoolScoringRuleUseCase implements IUseCase<DeleteSchoolScoringRuleCommand, Void> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSchoolScoringRuleCommand command) {
        requireActiveSchoolAdminOf(command.schoolId());
        ScoringRule rule = requireRuleForPolicy(command.ruleId(), command.policyId());
        AssessmentPolicy policy = requirePolicyOfSchool(rule.getPolicyId(), command.schoolId());
        requireDraftStatus(policy);

        scoringRuleRepository.deleteById(rule.getId());
        return null;
    }

    private ScoringRule requireRuleForPolicy(UUID ruleId, UUID policyId) {
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Scoring Rule."));
        if (!rule.getPolicyId().equals(policyId)) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }
        return rule;
    }

    // A Scoring Rule can only be deleted while its Policy is still DRAFT (avoids silently deleting the
    // scoring rules of a PUBLISHED Policy that is grading real exams). Call it after the ownership/scope checks.
    private void requireDraftStatus(AssessmentPolicy policy) {
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được xóa Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }
    }

    private void requireActiveSchoolAdminOf(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }
    }

    private AssessmentPolicy requirePolicyOfSchool(UUID policyId, UUID schoolId) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }
        return policy;
    }

}