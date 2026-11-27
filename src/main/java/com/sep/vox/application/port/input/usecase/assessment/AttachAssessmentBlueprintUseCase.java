package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.AttachAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.assessment.CreateBlueprintInlineCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;

@Service
@RequiredArgsConstructor
public class AttachAssessmentBlueprintUseCase implements IUseCase<AttachAssessmentBlueprintCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(AttachAssessmentBlueprintCommand input) {
        requireValidBlueprintSelection(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (assessment.getKind() != AssessmentKind.CENTRALIZED) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra tập trung");
        }

        applyBlueprintSelection(input, assessment, currentUserId);

        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private void applyBlueprintSelection(AttachAssessmentBlueprintCommand input, Assessment assessment, UUID currentUserId) {
        // Attaching or changing the blueprint: school admin, the question author, or the council chair
        // (the chair runs the whole process, so the chair must be able to choose the blueprint).
        // Pinning the version stays limited to the school admin and the chair.
        boolean canAttach = assessmentMemberRepository.canAttachBlueprint(assessment.getId(), currentUserId);
        boolean isChair = assessmentMemberRepository.canApproveBlueprintVersion(assessment.getId(), currentUserId);

        if (input.blueprintId() != null) {
            attachExistingBlueprint(input, assessment, canAttach);
        }
        if (input.newBlueprint() != null) {
            attachNewBlueprint(input, assessment, canAttach);
        }
        if (input.blueprintVersionId() != null) {
            pinBlueprintVersion(input, assessment, isChair);
        }
    }

    private void requireValidBlueprintSelection(AttachAssessmentBlueprintCommand input) {
        if (input.blueprintId() == null && input.blueprintVersionId() == null && input.newBlueprint() == null) {
            throw new IllegalArgumentException("Phải cung cấp blueprintId, blueprintVersionId hoặc newBlueprint");
        }
        if (input.blueprintId() != null && input.newBlueprint() != null) {
            throw new IllegalArgumentException("Chỉ được cung cấp blueprintId hoặc newBlueprint, không được cung cấp cả hai");
        }
    }

    private void attachExistingBlueprint(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean canAttach) {
        if (!canAttach) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessment.getSchoolId().equals(blueprint.getSchoolId())) {
            throw new IllegalArgumentException("Blueprint không thuộc cùng trường với bài kiểm tra");
        }

        if (!input.blueprintId().equals(assessment.getBlueprintId())) {
            requireNoExistingPapers(assessment.getId());
            assessment.setBlueprintVersionId(null);
        }
        assessment.setBlueprintId(input.blueprintId());
    }

    private void attachNewBlueprint(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean canAttach) {
        if (!canAttach) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        requireNoExistingPapers(assessment.getId());
        AssessmentBlueprint createdBlueprint = createInlineBlueprint(input.newBlueprint(), assessment.getSchoolId(), assessment.getLanguageId());
        assessment.setBlueprintId(createdBlueprint.getId());
        assessment.setBlueprintVersionId(null);
    }

    private void pinBlueprintVersion(AttachAssessmentBlueprintCommand input, Assessment assessment, boolean isChair) {
        if (!isChair) {
            throw new ForbiddenException("Chỉ CHAIR được chốt version blueprint");
        }
        if (assessment.getBlueprintId() == null) {
            throw new IllegalArgumentException("Bài kiểm tra chưa gắn blueprint");
        }

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        requirePublishedVersionOfAttachedBlueprint(version, assessment);
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + version.getCode()
        );

        if (!input.blueprintVersionId().equals(assessment.getBlueprintVersionId())) {
            requireNoExistingPapers(assessment.getId());
        }
        assessment.setBlueprintVersionId(input.blueprintVersionId());
    }

    private void requirePublishedVersionOfAttachedBlueprint(AssessmentBlueprintVersion version, Assessment assessment) {
        if (!version.getBlueprintId().equals(assessment.getBlueprintId())) {
            throw new IllegalArgumentException("Version không thuộc blueprint đang gắn vào exam");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ được chốt version đã PUBLISHED");
        }
    }

    private void requireNoExistingPapers(UUID assessmentId) {
        if (assessmentPaperRepository.existsByAssessmentId(assessmentId)) {
            throw new IllegalArgumentException(
                "Kỳ thi đã có mã đề — phải xóa hết mã đề hiện có trước khi đổi blueprint hoặc chốt sang phiên bản khác");
        }
    }

    private AssessmentBlueprint createInlineBlueprint(
            CreateBlueprintInlineCommand command,
            UUID schoolId,
            UUID assessmentLanguageId) {
        String name = StringNormalization.trimAndCollapseSpaces(command.name());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên blueprint là bắt buộc");
        }
        // Grade level is a shared catalog: it only has to exist, it no longer belongs to a school.
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy khối lớp");
        }
        AssessmentBlueprint blueprint = AssessmentBlueprint.create(
            schoolId,
            command.languageId() == null ? assessmentLanguageId : command.languageId(),
            command.gradeLevelId(),
            blueprintCodeOf(command.code()),
            name,
            StringNormalization.trimAndCollapseSpaces(command.description()));
        return assessmentBlueprintRepository.save(blueprint);
    }

    private String blueprintCodeOf(String code) {
        String normalized = StringNormalization.normalizeCode(code);
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
