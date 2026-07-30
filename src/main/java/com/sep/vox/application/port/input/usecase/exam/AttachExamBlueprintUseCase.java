package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AttachExamBlueprintCommand;
import com.sep.vox.application.port.input.command.CreateBlueprintInlineCommand;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;

@Service
public class AttachExamBlueprintUseCase implements IUseCase<AttachExamBlueprintCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final ExamTimeQuotaGuardService examTimeQuotaGuardService;
    private final UserContextPort userContextPort;

    public AttachExamBlueprintUseCase(
            ExamRepository examRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            ExamTimeQuotaGuardService examTimeQuotaGuardService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.examTimeQuotaGuardService = examTimeQuotaGuardService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(AttachExamBlueprintCommand input) {
        if (input.blueprintId() == null && input.blueprintVersionId() == null && input.newBlueprint() == null) {
            throw new IllegalStateException("Phải cung cấp blueprintId, blueprintVersionId hoặc newBlueprint");
        }
        if (input.blueprintId() != null && input.newBlueprint() != null) {
            throw new IllegalStateException("Chỉ được cung cấp blueprintId hoặc newBlueprint, không được cung cấp cả hai");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra tập trung");
        }

        boolean isAuthor = examMemberRepository.canAttachBlueprint(exam.getId(), currentUserId);
        boolean isChair = examMemberRepository.canApproveBlueprintVersion(exam.getId(), currentUserId);

        if (input.blueprintId() != null) {
            if (!isAuthor) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }

            var blueprint = examBlueprintRepository.findById(input.blueprintId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
            if (!exam.getSchoolId().equals(blueprint.getSchoolId())) {
                throw new IllegalStateException("Blueprint không thuộc cùng trường với bài kiểm tra");
            }

            if (!input.blueprintId().equals(exam.getBlueprintId())) {
                requireNoExistingPapers(exam.getId());
                exam.setBlueprintVersionId(null);
            }
            exam.setBlueprintId(input.blueprintId());
        }

        if (input.newBlueprint() != null) {
            if (!isAuthor) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
            requireNoExistingPapers(exam.getId());
            var createdBlueprint = createInlineBlueprint(input.newBlueprint(), exam.getSchoolId(), exam.getLanguageId(), currentUserId);
            exam.setBlueprintId(createdBlueprint.getId());
            exam.setBlueprintVersionId(null);
        }

        if (input.blueprintVersionId() != null) {
            if (!isChair) {
                throw new ForbiddenException("Chỉ CHAIR được chốt version blueprint");
            }
            if (exam.getBlueprintId() == null) {
                throw new IllegalStateException("Bài kiểm tra chưa gắn blueprint");
            }

            var version = examBlueprintVersionRepository.findById(input.blueprintVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
            if (!version.getBlueprintId().equals(exam.getBlueprintId())) {
                throw new IllegalStateException("Version không thuộc blueprint đang gắn vào exam");
            }
            if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Chỉ được chốt version đã PUBLISHED");
            }
            examTimeQuotaGuardService.requireWithinPlan(
                exam.getSchoolId(),
                version.getTotalTimeLimitSeconds(),
                "Phiên bản blueprint " + version.getCode()
            );

            if (!input.blueprintVersionId().equals(exam.getBlueprintVersionId())) {
                requireNoExistingPapers(exam.getId());
            }
            exam.setBlueprintVersionId(input.blueprintVersionId());
        }

        exam.setUpdatedAt(OffsetDateTime.now());
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }

    private void requireNoExistingPapers(UUID examId) {
        if (examPaperRepository.existsByExamId(examId)) {
            throw new IllegalStateException(
                "Kỳ thi đã có mã đề — phải xóa hết mã đề hiện có trước khi đổi blueprint hoặc chốt sang phiên bản khác");
        }
    }

    private ExamBlueprint createInlineBlueprint(
            CreateBlueprintInlineCommand command,
            UUID schoolId,
            UUID examLanguageId,
            UUID currentUserId) {
        var now = OffsetDateTime.now();
        var name = StringNormalization.trimAndCollapseSpaces(command.name());
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Tên blueprint là bắt buộc");
        }
        if (command.schoolGradeLevelId() != null) {
            var gradeLevel = schoolGradeLevelRepository.findById(command.schoolGradeLevelId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp"));
            if (!schoolId.equals(gradeLevel.getSchoolId())) {
                throw new IllegalStateException("Khối lớp không thuộc trường hiện tại");
            }
        }
        var blueprint = new ExamBlueprint(
            schoolId,
            command.languageId() == null ? examLanguageId : command.languageId(),
            command.schoolGradeLevelId(),
            blueprintCodeOf(command.code()),
            name,
            StringNormalization.trimAndCollapseSpaces(command.description()),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );
        return examBlueprintRepository.save(blueprint);
    }

    private String blueprintCodeOf(String code) {
        var normalized = StringNormalization.normalizeCode(code);
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return "BP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
