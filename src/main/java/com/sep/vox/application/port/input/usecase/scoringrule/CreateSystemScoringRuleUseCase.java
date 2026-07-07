package com.sep.vox.application.port.input.usecase.scoringrule;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateScoringRuleCommand;
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
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSystemScoringRuleUseCase implements IUseCase<CreateScoringRuleCommand, UUID> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final ScoringRuleCriterionValidator scoringRuleCriterionValidator;

    public CreateSystemScoringRuleUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            ScoringRuleCriterionValidator scoringRuleCriterionValidator) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.scoringRuleCriterionValidator = scoringRuleCriterionValidator;
    }

    @Override
    @Transactional
    public UUID execute(CreateScoringRuleCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra Assessment Policy tồn tại và thuộc phạm vi toàn hệ thống
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể can thiệp vào Assessment Policy của trường học.");
        }

        // 3. Chỉ được thêm Scoring Rule khi Policy còn DRAFT.
        // Lý do: Policy đã PUBLISHED đang được dùng để chấm điểm bài thi thật, nếu cho thêm luật
        // ngầm giữa chừng thì các bài thi cùng Policy sẽ bị chấm không nhất quán (bài trước/sau
        // khi thêm rule chịu luật khác nhau) mà không ai kiểm soát được.
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }

        // 4. Kiểm tra trùng mã Code trong phạm vi Policy này
        String safeCode = command.code().strip();
        if (scoringRuleRepository.existsByPolicyIdAndCode(command.policyId(), safeCode)) {
            throw new IllegalStateException("Mã Scoring Rule (code) '" + safeCode + "' đã tồn tại trong Assessment Policy này.");
        }

        // 5. Validate priority (khớp với check constraint DB: priority > 0), fail sớm thay vì đợi DB báo lỗi
        if (command.priority() <= 0) {
            throw new IllegalArgumentException("Độ ưu tiên (priority) phải lớn hơn 0.");
        }

        // 6. Convert Map JSON thô -> Value Object cụ thể theo đúng conditionType/actionType đã chọn
        // (nếu params không khớp cấu trúc mong đợi, ScoringRuleParamsMapper sẽ ném IllegalArgumentException)
        var conditionParams = ScoringRuleParamsMapper.toConditionParams(command.conditionType(), command.conditionParams());
        var actionParams = ScoringRuleParamsMapper.toActionParams(command.actionType(), command.actionParams());

        // 6b. Kiểm tra criterionCode/bandCode (nếu có) có thực sự tồn tại trong Rubric Version của Policy
        scoringRuleCriterionValidator.validate(policy.getRubricVersionId(), conditionParams, actionParams);

        // 7. Tạo mới và lưu
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