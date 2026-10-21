package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentBlueprintVersionPublishedEvent;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

@Service
public class UpdateAssessmentBlueprintVersionStatusUseCase
        implements IUseCase<UpdateAssessmentBlueprintVersionStatusCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public UpdateAssessmentBlueprintVersionStatusUseCase(
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            OutboxRepository outboxRepository, 
            JsonSerializationPort jsonSerializationPort, 
            UserRoleQueryRepository userRoleQueryRepository) {
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(UpdateAssessmentBlueprintVersionStatusCommand input) {
        var command = new UpdateAssessmentBlueprintVersionStatusCommand(
            input.versionId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var version = assessmentBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));

        requireStatusActor(version, blueprint, currentUserId, currentSchoolId);
        var now = Instant.now();

        switch (command.action()) {
            case "PUBLISH" -> {
                if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ được publish version ở trạng thái DRAFT");
                }
                validatePublishable(version);
                version.setStatus(AssessmentBlueprintVersionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
                    throw new IllegalStateException("Chỉ được archive version ở trạng thái PUBLISHED");
                }
                version.setStatus(AssessmentBlueprintVersionStatus.ARCHIVED);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        version.setUpdatedAt(Instant.now());
        version.setUpdatedBy(currentUserId);
        var saved = assessmentBlueprintVersionRepository.save(version);

        if (command.action().equals("PUBLISH")) {
            var schoolAdminIds = schoolUserRepository.findBySchoolIdWithRole(blueprint.getSchoolId(), "SCHOOL_ADMIN")
                .stream()
                .map(su -> su.getUserId())
                .toList();
            var event = new AssessmentBlueprintVersionPublishedEvent(
                schoolAdminIds,
                blueprint.getCode(),
                blueprint.getName(),
                blueprint.getId(),
                saved.getId()
            );
            var payload = jsonSerializationPort.toJson(event);
            var outbox = Outbox.create(AggregateTypeConstant.ASSESSMENT_BLUEPRINT_VERSION, version.getId(), EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED, payload, now);
            outboxRepository.save(outbox);
        }

        return AssessmentBlueprintVersionDto.toDto(saved);
    }

    private void requireStatusActor(AssessmentBlueprintVersion version, AssessmentBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        if (!assessmentBlueprintRepository.canChangeVersionStatus(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException(
                "Quyền truy cập bị từ chối — cần là SCHOOL_ADMIN hoặc CHAIR/REVIEWER của kỳ thi đã gắn blueprint này");
        }
        // Ai được TỰ DUYỆT version do chính mình tạo.
        //
        // SCHOOL_ADMIN/SYSTEM_ADMIN: được, vì là thẩm quyền cao nhất của blueprint và thực tế
        // một trường thường chỉ có 1 admin -- chặn tuyệt đối thì version kẹt vĩnh viễn ở DRAFT.
        //
        // CHAIR: được, ngang quyền admin (đổi 2026-08-15). Trước đây maker-checker áp cho cả
        // nhánh giáo viên, nên CHAIR tạo version xong phải đi tìm người khác bấm publish mới
        // chốt được vào kỳ thi của chính mình (AttachAssessmentBlueprintUseCase đòi version đã
        // PUBLISHED). Với trường ít người thì "người khác" đó thường không tồn tại, và CHAIR
        // vốn đã là người chịu trách nhiệm cuối cùng cho kỳ thi -- bắt họ xin duyệt khung đề
        // của chính kỳ thi mình chủ trì chỉ tạo ra một chữ ký hình thức.
        //
        // REVIEWER: KHÔNG. Họ tồn tại đúng để làm người thứ hai, cho tự duyệt là xoá luôn vai
        // trò của mình. Đây là lý do phải hỏi isChairOfAssessmentUsingBlueprint chứ không dùng lại
        // canChangeVersionStatus ở trên -- câu đó gộp CHAIR với REVIEWER.
        var isAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()) || "SYSTEM_ADMIN".equals(role.roleCode()));
        var isChair = assessmentBlueprintRepository.isChairOfAssessmentUsingBlueprint(
            blueprint.getId(), currentUserId, currentSchoolId
        );
        if (!isAdmin && !isChair && currentUserId.equals(version.getCreatedBy())) {
            throw new ForbiddenException("Người tạo version không được tự đổi trạng thái version của chính mình");
        }
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validatePublishable(AssessmentBlueprintVersion version) {
        var sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (sections.isEmpty()) {
            throw new IllegalStateException("Blueprint version phải có ít nhất một section trước khi publish");
        }
        var weightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (weightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException("Tổng trọng số section phải bằng 1.00 trước khi publish");
        }

        var allSlots = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId());
        allSlots.stream()
            .filter(slot -> slot.getSlotType() == AssessmentBlueprintSlotType.FIXED)
            .forEach(slot -> {
                var question = slot.getFixedQuestionId() == null
                    ? null
                    : questionRepository.findById(slot.getFixedQuestionId()).orElse(null);
                if (question == null || question.getStatus() != QuestionStatus.PUBLISHED) {
                    throw new IllegalStateException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
                }
            });

        var slotsBySectionId = allSlots.stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Tổng trọng số ô câu hỏi trong phần \"" + section.getTitle() + "\" phải bằng 1.00 trước khi publish");
            }
        }
    }
}
