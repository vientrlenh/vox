package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintVersionStatusUseCase
        implements IUseCase<UpdateExamBlueprintVersionStatusCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateExamBlueprintVersionStatusUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
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

        version.setUpdatedAt(OffsetDateTime.now());
        version.setUpdatedBy(currentUserId);
        var saved = examBlueprintVersionRepository.save(version);

        if (command.action().equals("PUBLISH")) {
            eventPublisherPort.publish(new ExamBlueprintVersionPublishedEvent(
                blueprint.getSchoolId(),
                blueprint.getCode(),
                blueprint.getName()
            ));
        }

        return ExamBlueprintVersionDtoMapper.toDto(saved);
    }

    private void requireStatusActor(ExamBlueprintVersion version, ExamBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        if (!examBlueprintRepository.canChangeVersionStatus(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException(
                "Quyền truy cập bị từ chối — cần là SCHOOL_ADMIN (nếu blueprint chưa gắn kỳ thi) hoặc CHAIR/REVIEWER của kỳ thi đã gắn");
        }
        if (currentUserId.equals(version.getCreatedBy())) {
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
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException("Tổng trọng số section phải bằng 1.00 trước khi publish");
        }

        var slotsBySectionId = examBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(ExamBlueprintSlot::getSectionId));
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Tổng trọng số ô câu hỏi trong phần \"" + section.getTitle() + "\" phải bằng 1.00 trước khi publish");
            }
        }
    }
}
