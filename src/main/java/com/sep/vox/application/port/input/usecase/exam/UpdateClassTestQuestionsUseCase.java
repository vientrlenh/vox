package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateClassTestQuestionsUseCase implements IUseCase<UpdateClassTestQuestionsCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public UpdateClassTestQuestionsUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateClassTestQuestionsCommand input) {
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
        if (input.sections() == null || input.sections().isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có ít nhất 1 section");
        }
        var seenQuestionIds = new java.util.HashSet<UUID>();
        for (var section : input.sections()) {
            if (section.questions() == null || section.questions().isEmpty()) {
                throw new IllegalStateException("Mỗi section phải có ít nhất 1 câu hỏi");
            }
            for (var questionCommand : section.questions()) {
                if (!seenQuestionIds.add(questionCommand.questionId())) {
                    throw new IllegalStateException("Một câu hỏi không thể xuất hiện nhiều lần trong cùng 1 bài kiểm tra");
                }
            }
        }

        for (var section : input.sections()) {
            for (var questionCommand : section.questions()) {
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
        }

        var paper = examPaperRepository.findByExamId(exam.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var existingPaperSections = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var now = OffsetDateTime.now();
        var commonCount = Math.min(existingPaperSections.size(), input.sections().size());
        var sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(input.sections());

        for (int i = 0; i < commonCount; i++) {
            var existingPaperSection = existingPaperSections.get(i);
            var sectionCommand = input.sections().get(i);

            existingPaperSection.setTitle(sectionCommand.title());
            existingPaperSection.setInstruction(sectionCommand.instruction());
            existingPaperSection.setWeight(sectionWeights.get(i));
            existingPaperSection.setUpdatedAt(now);
            existingPaperSection.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(existingPaperSection);

            updateSectionQuestions(existingPaperSection, sectionCommand.questions(), exam.getId(), currentUserId, now);
        }

        for (int i = input.sections().size(); i < existingPaperSections.size(); i++) {
            deleteSection(existingPaperSections.get(i));
        }

        for (int i = existingPaperSections.size(); i < input.sections().size(); i++) {
            createNewSection(paper, input.sections().get(i), sectionWeights.get(i), i + 1, exam.getId(), currentUserId, now);
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }

    private void updateSectionQuestions(
            ExamPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID examId,
            UUID currentUserId,
            OffsetDateTime now) {
        for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
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
    }

    private void deleteSection(ExamPaperSection paperSection) {
        for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
            examPaperItemRepository.deleteById(item.getId());
        }
        examPaperSectionRepository.deleteById(paperSection.getId());
    }

    private void createNewSection(
            ExamPaper paper,
            ClassTestSectionCommand sectionCommand,
            BigDecimal sectionWeight,
            int order,
            UUID examId,
            UUID currentUserId,
            OffsetDateTime now) {
        var paperSection = examPaperSectionRepository.save(new ExamPaperSection(
            paper.getId(),
            order,
            sectionCommand.title(),
            sectionCommand.instruction(),
            null,
            sectionWeight,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var questions = sectionCommand.questions();
        var weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            var questionId = questions.get(i).questionId();
            examPaperItemRepository.save(new ExamPaperItem(
                null,
                paperSection.getId(),
                paper.getId(),
                questionId,
                i + 1,
                weights.get(i)
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questionId, examId, ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void requireNoAttachedBlueprint(Exam exam) {
        if (exam.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }

}
