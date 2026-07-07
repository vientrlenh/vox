package com.sep.vox.application.port.input.usecase.scoringrule;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolScoringRuleCommand;
import com.sep.vox.application.common.ScoringRuleCriterionValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.mapper.ScoringRuleParamsMapper;
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
public class CreateSchoolScoringRuleUseCase implements IUseCase<CreateSchoolScoringRuleCommand, UUID> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final ScoringRuleCriterionValidator scoringRuleCriterionValidator;

    public CreateSchoolScoringRuleUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            ScoringRuleCriterionValidator scoringRuleCriterionValidator) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.scoringRuleCriterionValidator = scoringRuleCriterionValidator;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolScoringRuleCommand command) {
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
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        // 4. Chỉ được thêm Scoring Rule khi Policy còn DRAFT (giống hệt lý do bên System:
        // tránh thay đổi luật chấm điểm ngầm khi Policy đã PUBLISHED đang chấm bài thi thật)
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }

        // 5. Kiểm tra trùng mã Code trong phạm vi Policy này
        String safeCode = command.code().strip();
        if (scoringRuleRepository.existsByPolicyIdAndCode(command.policyId(), safeCode)) {
            throw new IllegalStateException("Mã Scoring Rule (code) '" + safeCode + "' đã tồn tại trong Assessment Policy này.");
        }

        // 6. Validate priority (khớp check constraint DB: priority > 0)
        if (command.priority() <= 0) {
            throw new IllegalArgumentException("Độ ưu tiên (priority) phải lớn hơn 0.");
        }

        // 7. Convert Map JSON thô -> Value Object cụ thể theo đúng conditionType/actionType đã chọn
        var conditionParams = ScoringRuleParamsMapper.toConditionParams(command.conditionType(), command.conditionParams());
        var actionParams = ScoringRuleParamsMapper.toActionParams(command.actionType(), command.actionParams());

        // 7b. Kiểm tra criterionCode/bandCode (nếu có) có thực sự tồn tại trong Rubric Version của Policy
        scoringRuleCriterionValidator.validate(policy.getRubricVersionId(), conditionParams, actionParams);

        // 8. Tạo mới và lưu
        OffsetDateTime now = OffsetDateTime.now();
        ScoringRule rule = new ScoringRule(
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
                command.isActive(),
                now, now, currentUserId, currentUserId
        );

        return scoringRuleRepository.save(rule).getId();
    }
}