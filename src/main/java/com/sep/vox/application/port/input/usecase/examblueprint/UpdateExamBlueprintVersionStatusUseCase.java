package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.ExamBlueprintVersionPublishedEvent;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintVersionStatusUseCase
        implements IUseCase<UpdateExamBlueprintVersionStatusCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public UpdateExamBlueprintVersionStatusUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            OutboxRepository outboxRepository, 
            JsonSerializationPort jsonSerializationPort, 
            UserRoleQueryRepository userRoleQueryRepository) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public ExamBlueprintVersionDto execute(UpdateExamBlueprintVersionStatusCommand input) {
        var command = new UpdateExamBlueprintVersionStatusCommand(
            input.versionId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var version = examBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));

        requireStatusActor(version, blueprint, currentUserId, currentSchoolId);
        var now = Instant.now();

        switch (command.action()) {
            case "PUBLISH" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ được publish version ở trạng thái DRAFT");
                }
                validatePublishable(version);
                version.setStatus(ExamBlueprintVersionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
                    throw new IllegalStateException("Chỉ được archive version ở trạng thái PUBLISHED");
                }
                version.setStatus(ExamBlueprintVersionStatus.ARCHIVED);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        version.setUpdatedAt(Instant.now());
        version.setUpdatedBy(currentUserId);
        var saved = examBlueprintVersionRepository.save(version);

        if (command.action().equals("PUBLISH")) {
            var schoolAdminIds = schoolUserRepository.findBySchoolIdWithRole(blueprint.getSchoolId(), "SCHOOL_ADMIN")
                .stream()
                .map(su -> su.getUserId())
                .toList();
            var event = new ExamBlueprintVersionPublishedEvent(
                schoolAdminIds,
                blueprint.getCode(),
                blueprint.getName()
            );
            var payload = jsonSerializationPort.toJson(event);
            var outbox = Outbox.create(AggregateTypeConstant.EXAM_BLUEPRINT_VERSION, version.getId(), EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED, payload, now);
            outboxRepository.save(outbox);
        }

        return ExamBlueprintVersionDtoMapper.toDto(saved);
    }

    private void requireStatusActor(ExamBlueprintVersion version, ExamBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        if (!examBlueprintRepository.canChangeVersionStatus(blueprint.getId(), currentUserId, currentSchoolId)) {
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
        // chốt được vào kỳ thi của chính mình (AttachExamBlueprintUseCase đòi version đã
        // PUBLISHED). Với trường ít người thì "người khác" đó thường không tồn tại, và CHAIR
        // vốn đã là người chịu trách nhiệm cuối cùng cho kỳ thi -- bắt họ xin duyệt khung đề
        // của chính kỳ thi mình chủ trì chỉ tạo ra một chữ ký hình thức.
        //
        // REVIEWER: KHÔNG. Họ tồn tại đúng để làm người thứ hai, cho tự duyệt là xoá luôn vai
        // trò của mình. Đây là lý do phải hỏi isChairOfExamUsingBlueprint chứ không dùng lại
        // canChangeVersionStatus ở trên -- câu đó gộp CHAIR với REVIEWER.
        var isAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()) || "SYSTEM_ADMIN".equals(role.roleCode()));
        var isChair = examBlueprintRepository.isChairOfExamUsingBlueprint(
            blueprint.getId(), currentUserId, currentSchoolId
        );
        if (!isAdmin && !isChair && currentUserId.equals(version.getCreatedBy())) {
            throw new ForbiddenException("Người tạo version không được tự đổi trạng thái version của chính mình");
        }
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validatePublishable(ExamBlueprintVersion version) {
        var sections = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (sections.isEmpty()) {
            throw new IllegalStateException("Blueprint version phải có ít nhất một section trước khi publish");
        }
        var weightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (weightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException("Tổng trọng số section phải bằng 1.00 trước khi publish");
        }

        var allSlots = examBlueprintSlotRepository.findByBlueprintVersionId(version.getId());
        allSlots.stream()
            .filter(slot -> slot.getSlotType() == ExamBlueprintSlotType.FIXED)
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
