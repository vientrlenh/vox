package com.sep.vox.application.port.input.usecase.scoringrule;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolScoringRuleDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.mapper.ScoringRuleDtoMapper;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolScoringRuleDetailUseCase implements IUseCase<ViewSchoolScoringRuleDetailQuery, ScoringRuleDto> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolScoringRuleDetailUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoringRuleDto execute(ViewSchoolScoringRuleDetailQuery query) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra tài khoản thuộc đúng trường học yêu cầu
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xem Scoring Rule của trường khác.");
        }

        // 3. Kiểm tra Scoring Rule tồn tại
        ScoringRule rule = scoringRuleRepository.findById(query.ruleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Scoring Rule."));

        // 4. BẢO MẬT: policyId trên path phải khớp với policyId thật sự của rule
        if (!rule.getPolicyId().equals(query.policyId())) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }

        // 5. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        var policy = assessmentPolicyRepository.findById(rule.getPolicyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xem Scoring Rule của trường khác.");
        }

        return ScoringRuleDtoMapper.toDto(rule);
    }
}
