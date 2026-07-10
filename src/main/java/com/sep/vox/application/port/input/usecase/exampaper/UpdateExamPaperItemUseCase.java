package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamPaperItemCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.mapper.ExamPaperItemDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamPaperItemUseCase implements IUseCase<UpdateExamPaperItemCommand, ExamPaperItemDto> {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamMemberRepository examMemberRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public UpdateExamPaperItemUseCase(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamMemberRepository examMemberRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            SchoolUserRepository schoolUserRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examMemberRepository = examMemberRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperItemDto execute(UpdateExamPaperItemCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var item = examPaperItemRepository.findById(input.itemId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong đề thi"));
        if (!item.getPaperId().equals(input.paperId())) {
            throw new NotFoundException("Không tìm thấy câu hỏi trong đề thi");
        }
        var paper = examPaperRepository.findById(item.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var exam = examRepository.findById(paper.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        // Bài trên lớp luôn tạo mã đề ở trạng thái LOCKED (không dùng luồng duyệt như kỳ thi tập trung),
        // nên LOCKED ở đây chỉ có ý nghĩa "khoá sửa" đối với CENTRALIZED.
        if (exam.getKind() == ExamKind.CENTRALIZED && paper.getStatus() == ExamPaperStatus.LOCKED) {
            throw new IllegalStateException("Đề thi đã bị khoá, không thể sửa câu hỏi");
        }
        if (exam.getStatus() == ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi bài kiểm tra đang diễn ra");
        }
        var requiredRole = exam.getKind() == ExamKind.CLASS_TEST ? ExamMemberRole.CHAIR : ExamMemberRole.AUTHOR;
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, requiredRole)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (item.getBlueprintSlotId() != null) {
            var slot = examBlueprintSlotRepository.findById(item.getBlueprintSlotId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy slot blueprint"));
            if (slot.getSlotType() == ExamBlueprintSlotType.FIXED) {
                throw new IllegalStateException("Slot FIXED không cho phép đổi câu hỏi");
            }
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var question = questionRepository.findAccessibleById(input.questionId(), currentUserId, currentSchoolId, false, false)
            .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi này"));
        if (exam.getKind() == ExamKind.CENTRALIZED && question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được gán câu hỏi đã PUBLISHED vào đề thi");
        }

        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("Quyền READ_ONLY không được phép gán câu hỏi vào đề thi");
            }
        }
        if (question.isLocked() && !isOwner) {
            throw new IllegalStateException("Câu hỏi " + question.getCode() + " đang bị khoá bởi kỳ thi khác");
        }

        item.setQuestionId(question.getId());
        var savedItem = examPaperItemRepository.save(item);

        examQuestionSecureLockService.lockQuestionForExam(
            question.getId(),
            paper.getExamId(),
            ExamSecurePoolReleaseMode.MANUAL,
            currentUserId
        );

        if (paper.getStatus() == ExamPaperStatus.APPROVED) {
            paper.setStatus(ExamPaperStatus.IN_REVIEW);
        }
        paper.setUpdatedAt(OffsetDateTime.now());
        paper.setUpdatedBy(currentUserId);
        examPaperRepository.save(paper);

        return ExamPaperItemDtoMapper.toDto(savedItem);
    }
}
