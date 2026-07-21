package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateClassTestSectionUseCase implements IUseCase<UpdateClassTestSectionCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public UpdateClassTestSectionUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateClassTestSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được sửa khi bài kiểm tra chưa mở cho học sinh làm bài (đang ở trạng thái đã lên lịch)");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requireNoAttachedBlueprint(exam);

        var paperSection = examPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));

        var now = OffsetDateTime.now();

        if (input.title() != null) {
            paperSection.setTitle(input.title());
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(paperSection);
        }

        if (input.instruction() != null) {
            paperSection.setInstruction(input.instruction());
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(paperSection);
        }

        if (input.weight() != null) {
            paperSection.setWeight(input.weight());
            var sections = examPaperSectionRepository.findByPaperId(paperSection.getPaperId()).stream()
                .map(section -> section.getId().equals(paperSection.getId()) ? paperSection : section)
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "Tổng trọng số section phải bằng 1.00");
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(paperSection);
        }

        if (input.questions() != null) {
            if (input.questions().isEmpty()) {
                throw new IllegalStateException("Section phải có ít nhất 1 câu hỏi");
            }
            for (var questionCommand : input.questions()) {
                var question = questionRepository.findAccessibleById(questionCommand.questionId(), currentUserId, exam.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi " + questionCommand.questionId()));
                boolean isOwner = currentUserId.equals(question.getCreatedBy());
                boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
                if (!isOwner && !isSchoolShared) {
                    var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
                    if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                        throw new ForbiddenException("Quyền READ_ONLY không được phép dùng câu hỏi trong bài kiểm tra");
                    }
                }
            }
            updateSectionQuestions(paperSection, input.questions(), exam.getId(), currentUserId, now);
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Exam exam) {
        if (exam.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }

    private void updateSectionQuestions(
            ExamPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID examId,
            UUID currentUserId,
            OffsetDateTime now) {
        var existingItems = examPaperItemRepository.findBySectionId(paperSection.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        for (var item : existingItems) {
            examPaperItemRepository.deleteById(item.getId());
        }

        var weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            var questionId = questions.get(i).questionId();
            examPaperItemRepository.save(new ExamPaperItem(
                null,
                paperSection.getId(),
                paperSection.getPaperId(),
                questionId,
                i + 1,
                weights.get(i)
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questionId, examId, ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
        paperSection.setUpdatedAt(now);
        paperSection.setUpdatedBy(currentUserId);
        examPaperSectionRepository.save(paperSection);
    }
}
