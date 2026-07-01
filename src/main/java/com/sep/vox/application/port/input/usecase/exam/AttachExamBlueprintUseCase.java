package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AttachExamBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class AttachExamBlueprintUseCase implements IUseCase<AttachExamBlueprintCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;

    public AttachExamBlueprintUseCase(
            ExamRepository examRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamMemberRepository examMemberRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(AttachExamBlueprintCommand input) {
        if (input.blueprintId() == null && input.blueprintVersionId() == null) {
            throw new IllegalStateException("Phai cung cap blueprintId hoac blueprintVersionId");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay bai kiem tra"));

        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Chi ap dung cho bai kiem tra tap trung");
        }

        boolean isChair = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR);
        boolean isAuthor = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.AUTHOR);

        if (input.blueprintId() != null) {
            if (!isAuthor) {
                throw new ForbiddenException("Quyen truy cap bi tu choi");
            }

            var blueprint = examBlueprintRepository.findById(input.blueprintId())
                .orElseThrow(() -> new NotFoundException("Khong tim thay blueprint de thi"));
            if (!exam.getSchoolId().equals(blueprint.getSchoolId())) {
                throw new IllegalStateException("Blueprint khong thuoc cung truong voi bai kiem tra");
            }

            exam.setBlueprintId(input.blueprintId());
        }

        if (input.blueprintVersionId() != null) {
            if (!isChair) {
                throw new ForbiddenException("Chi CHAIR duoc chot version blueprint");
            }
            if (exam.getBlueprintId() == null) {
                throw new IllegalStateException("Bai kiem tra chua gan blueprint");
            }

            var version = examBlueprintVersionRepository.findById(input.blueprintVersionId())
                .orElseThrow(() -> new NotFoundException("Khong tim thay version blueprint"));
            if (!version.getBlueprintId().equals(exam.getBlueprintId())) {
                throw new IllegalStateException("Version khong thuoc blueprint dang gan vao exam");
            }
            if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Chi duoc chot version da PUBLISHED");
            }

            exam.setBlueprintVersionId(input.blueprintVersionId());
        }

        exam.setUpdatedAt(OffsetDateTime.now());
        exam.setUpdatedBy(currentUserId);
        return ExamDtoMapper.toDto(examRepository.save(exam));
    }
}
