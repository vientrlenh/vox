package com.sep.vox.application.port.input.usecase.scoringrule;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.mapper.ScoringRuleParamsMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CriterionScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.SetFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionBandThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

@Service
public class UpdateSchoolScoringRuleUseCase implements IUseCase<UpdateSchoolScoringRuleCommand, UUID> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public UpdateSchoolScoringRuleUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCriterionBandRepository rubricCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolScoringRuleCommand command) {
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

        // 3. Kiểm tra Scoring Rule tồn tại
        ScoringRule rule = scoringRuleRepository.findById(command.ruleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Scoring Rule."));

        // 4. BẢO MẬT: policyId trên path phải khớp với policyId thật sự của rule
        if (!rule.getPolicyId().equals(command.policyId())) {
            throw new ForbiddenException("BẢO MẬT: Scoring Rule này không thuộc Assessment Policy đã chỉ định.");
        }

        // 5. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        AssessmentPolicy policy = assessmentPolicyRepository.findById(rule.getPolicyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        // 6. Chỉ được sửa Scoring Rule khi Policy còn DRAFT
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được sửa Scoring Rule khi Assessment Policy đang ở trạng thái DRAFT.");
        }

        // 7. Validate priority
        if (command.priority() <= 0) {
            throw new IllegalArgumentException("Độ ưu tiên (priority) phải lớn hơn 0.");
        }

        // 8. Convert Map JSON thô -> Value Object cụ thể theo conditionType/actionType MỚI
        var conditionParams = ScoringRuleParamsMapper.toConditionParams(command.conditionType(), command.conditionParams());
        var actionParams = ScoringRuleParamsMapper.toActionParams(command.actionType(), command.actionParams());

        // 8b. Kiểm tra criterionCode/bandCode (nếu có) có thực sự tồn tại trong Rubric Version của Policy
        validateCriterionReferences(policy.getRubricVersionId(), policy.getFrameworkVersionId(), conditionParams, actionParams);

        // 9. Cập nhật các field được phép sửa (code và policyId là bất biến, không đụng tới)
        rule.setName(command.name().strip());
        rule.setDescription(command.description());
        rule.setConditionType(command.conditionType());
        rule.setConditionParams(conditionParams);
        rule.setActionType(command.actionType());
        rule.setActionParams(actionParams);
        rule.setPriority(command.priority());
        rule.setSeverity(command.severity());
        rule.setStopProcessing(command.stopProcessing());
        rule.setActive(command.isActive());
        rule.setUpdatedAt(OffsetDateTime.now());
        rule.setUpdatedBy(currentUserId);

        return scoringRuleRepository.save(rule).getId();
    }

    // Kiểm tra criterionCode/bandCode (điền dạng code, không phải UUID) trong conditionParams/actionParams
    // của ScoringRule có thực sự tồn tại trong Rubric Version của Assessment Policy hay không, và
    // frameworkResultBandId (nếu có) có thực sự tồn tại và thuộc đúng Framework Version của Policy hay không.
    private void validateCriterionReferences(UUID rubricVersionId, UUID frameworkVersionId,
            ScoringRuleConditionParams conditionParams, ScoringRuleActionParams actionParams) {
        switch (conditionParams) {
            case CriterionScoreThresholdParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case CriterionBandThresholdParams p -> {
                RubricCriterion criterion = requireCriterion(rubricVersionId, p.criterionCode());
                requireCriterionBand(criterion, p.bandCode());
            }
            default -> {
            }
        }

        switch (actionParams) {
            case CapCriterionScoreParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case CriterionScoreDeltaParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case SetFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.frameworkResultBandId());
            case CapFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.maxResultBandId());
            default -> {
            }
        }
    }

    private RubricCriterion requireCriterion(UUID rubricVersionId, String criterionCode) {
        return rubricCriterionRepository.findByRubricVersionIdAndCode(rubricVersionId, criterionCode)
                .orElseThrow(() -> new NotFoundException(
                        "Mã tiêu chí (criterionCode) '" + criterionCode + "' không tồn tại trong Rubric Version của Assessment Policy này."));
    }

    private void requireCriterionBand(RubricCriterion criterion, String bandCode) {
        rubricCriterionBandRepository.findByCriterionIdAndCode(criterion.getId(), bandCode)
                .orElseThrow(() -> new NotFoundException(
                        "Mã mức độ (bandCode) '" + bandCode + "' không tồn tại trong tiêu chí '" + criterion.getCode() + "'."));
    }

    private void requireFrameworkResultBand(UUID frameworkVersionId, UUID frameworkResultBandId) {
        FrameworkResultBand band = frameworkResultBandRepository.findById(frameworkResultBandId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy Framework Result Band với ID '" + frameworkResultBandId + "'."));
        if (!band.getFrameworkVersionId().equals(frameworkVersionId)) {
            throw new NotFoundException(
                    "Framework Result Band '" + frameworkResultBandId + "' không thuộc Framework Version của Assessment Policy này.");
        }
    }
}
