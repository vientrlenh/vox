package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AttachAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.CreateBlueprintInlineCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class AttachAssessmentBlueprintUseCase implements IUseCase<AttachAssessmentBlueprintCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final UserContextPort userContextPort;

    public AttachAssessmentBlueprintUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            GradeLevelRepository gradeLevelRepository,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(AttachAssessmentBlueprintCommand input) {
        if (input.blueprintId() == null && input.blueprintVersionId() == null && input.newBlueprint() == null) {
            throw new IllegalStateException("Phải cung cấp blueprintId, blueprintVersionId hoặc newBlueprint");
        }
        if (input.blueprintId() != null && input.newBlueprint() != null) {
            throw new IllegalStateException("Chỉ được cung cấp blueprintId hoặc newBlueprint, không được cung cấp cả hai");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (assessment.getKind() != AssessmentKind.CENTRALIZED) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra tập trung");
        }

        // Gắn/đổi khung đề: quản trị trường, người ra đề, hoặc chủ tịch hội đồng (chủ tịch chạy trọn
        // quy trình nên phải tự chọn được khung đề). Chốt version thì vẫn chỉ quản trị trường/chủ tịch.
        boolean canAttach = assessmentMemberRepository.canAttachBlueprint(assessment.getId(), currentUserId);
        boolean isChair = assessmentMemberRepository.canApproveBlueprintVersion(assessment.getId(), currentUserId);

        if (input.blueprintId() != null) {
            if (!canAttach) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }

            var blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
            if (!assessment.getSchoolId().equals(blueprint.getSchoolId())) {
                throw new IllegalStateException("Blueprint không thuộc cùng trường với bài kiểm tra");
            }

            if (!input.blueprintId().equals(assessment.getBlueprintId())) {
                requireNoExistingPapers(assessment.getId());
                assessment.setBlueprintVersionId(null);
            }
            assessment.setBlueprintId(input.blueprintId());
        }

        if (input.newBlueprint() != null) {
            if (!canAttach) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
            requireNoExistingPapers(assessment.getId());
            var createdBlueprint = createInlineBlueprint(input.newBlueprint(), assessment.getSchoolId(), assessment.getLanguageId(), currentUserId);
            assessment.setBlueprintId(createdBlueprint.getId());
            assessment.setBlueprintVersionId(null);
        }

        if (input.blueprintVersionId() != null) {
            if (!isChair) {
                throw new ForbiddenException("Chỉ CHAIR được chốt version blueprint");
            }
            if (assessment.getBlueprintId() == null) {
                throw new IllegalStateException("Bài kiểm tra chưa gắn blueprint");
            }

            var version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
            if (!version.getBlueprintId().equals(assessment.getBlueprintId())) {
                throw new IllegalStateException("Version không thuộc blueprint đang gắn vào assessment");
            }
            if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Chỉ được chốt version đã PUBLISHED");
            }
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

        assessment.setUpdatedAt(Instant.now());
        assessment.setUpdatedBy(currentUserId);
        return AssessmentDto.toDto(assessmentRepository.save(assessment));
    }

    private void requireNoExistingPapers(UUID assessmentId) {
        if (assessmentPaperRepository.existsByAssessmentId(assessmentId)) {
            throw new IllegalStateException(
                "Kỳ thi đã có mã đề — phải xóa hết mã đề hiện có trước khi đổi blueprint hoặc chốt sang phiên bản khác");
        }
    }

    private AssessmentBlueprint createInlineBlueprint(
            CreateBlueprintInlineCommand command,
            UUID schoolId,
            UUID assessmentLanguageId,
            UUID currentUserId) {
        var now = Instant.now();
        var name = StringNormalization.trimAndCollapseSpaces(command.name());
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Tên blueprint là bắt buộc");
        }
        // Khối lớp là catalog dùng chung: chỉ cần tồn tại, không còn ràng buộc thuộc trường nào.
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy khối lớp");
        }
        var blueprint = new AssessmentBlueprint(
            schoolId,
            command.languageId() == null ? assessmentLanguageId : command.languageId(),
            command.gradeLevelId(),
            blueprintCodeOf(command.code()),
            name,
            StringNormalization.trimAndCollapseSpaces(command.description()),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );
        return assessmentBlueprintRepository.save(blueprint);
    }

    private String blueprintCodeOf(String code) {
        var normalized = StringNormalization.normalizeCode(code);
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
