package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;

@Service
public class UpdateClassTestQuestionsUseCase implements IUseCase<UpdateClassTestQuestionsCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
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
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
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
        if (exam.getStatus() != ExamStatus.SCHEDULED && exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Chỉ được sửa câu hỏi khi bài kiểm tra chưa đóng/hủy");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requirePrivateBlueprint(exam);
        if (input.sections() == null || input.sections().isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có ít nhất 1 section");
        }
        for (var section : input.sections()) {
            if (section.questionIds() == null || section.questionIds().isEmpty()) {
                throw new IllegalStateException("Mỗi section phải có ít nhất 1 câu hỏi");
            }
        }

        for (var section : input.sections()) {
            for (var questionId : section.questionIds()) {
                var question = questionRepository.findAccessibleById(questionId, currentUserId, exam.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi " + questionId));
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

        var version = examBlueprintVersionRepository.findByBlueprintId(exam.getBlueprintId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var existingSections = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var paper = examPaperRepository.findByExamId(exam.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var existingPaperSections = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var now = OffsetDateTime.now();
        var commonCount = Math.min(existingSections.size(), input.sections().size());

        for (int i = 0; i < commonCount; i++) {
            updateSectionQuestions(
                existingSections.get(i),
                existingPaperSections.get(i),
                input.sections().get(i).questionIds(),
                exam.getId(),
                currentUserId,
                now
            );
        }

        for (int i = input.sections().size(); i < existingSections.size(); i++) {
            deleteSection(existingSections.get(i), existingPaperSections.get(i));
        }

        for (int i = existingSections.size(); i < input.sections().size(); i++) {
            createNewSection(version, paper, input.sections().get(i), i + 1, exam.getId(), currentUserId, now);
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }

    private void updateSectionQuestions(
            ExamBlueprintSection section,
            ExamPaperSection paperSection,
            List<UUID> questionIds,
            UUID examId,
            UUID currentUserId,
            OffsetDateTime now) {
        var slots = examBlueprintSlotRepository.findBySectionId(section.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        var items = examPaperItemRepository.findBySectionId(paperSection.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var commonCount = Math.min(slots.size(), questionIds.size());

        for (int i = 0; i < commonCount; i++) {
            var slot = slots.get(i);
            var item = items.get(i);
            var newQuestionId = questionIds.get(i);
            if (!newQuestionId.equals(slot.getFixedQuestionId())) {
                slot.setFixedQuestionId(newQuestionId);
                slot.setUpdatedAt(now);
                slot.setUpdatedBy(currentUserId);
                examBlueprintSlotRepository.save(slot);

                item.setQuestionId(newQuestionId);
                examPaperItemRepository.save(item);

                examQuestionSecureLockService.lockQuestionForExam(
                    newQuestionId, examId, ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
                );
            }
        }

        for (int i = questionIds.size(); i < slots.size(); i++) {
            examPaperItemRepository.deleteById(items.get(i).getId());
            examBlueprintSlotRepository.deleteById(slots.get(i).getId());
        }

        for (int i = slots.size(); i < questionIds.size(); i++) {
            var questionId = questionIds.get(i);
            var slot = examBlueprintSlotRepository.save(new ExamBlueprintSlot(
                section.getId(),
                section.getBlueprintVersionId(),
                i + 1,
                BigDecimal.ONE,
                null,
                null,
                ExamBlueprintSlotType.FIXED,
                questionId,
                null,
                now,
                now,
                currentUserId,
                currentUserId
            ));
            examPaperItemRepository.save(new ExamPaperItem(
                slot.getId(),
                paperSection.getId(),
                paperSection.getPaperId(),
                questionId,
                slot.getOrder(),
                BigDecimal.ONE
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questionId, examId, ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void deleteSection(ExamBlueprintSection section, ExamPaperSection paperSection) {
        for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
            examPaperItemRepository.deleteById(item.getId());
        }
        examPaperSectionRepository.deleteById(paperSection.getId());

        for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
            examBlueprintSlotRepository.deleteById(slot.getId());
        }
        examBlueprintSectionRepository.deleteById(section.getId());
    }

    private void createNewSection(
            ExamBlueprintVersion version,
            ExamPaper paper,
            ClassTestSectionCommand sectionCommand,
            int order,
            UUID examId,
            UUID currentUserId,
            OffsetDateTime now) {
        var section = examBlueprintSectionRepository.save(new ExamBlueprintSection(
            version.getId(),
            order,
            sectionCommand.title(),
            sectionCommand.instruction(),
            null,
            BigDecimal.ONE,
            now,
            now,
            currentUserId,
            currentUserId
        ));
        var paperSection = examPaperSectionRepository.save(new ExamPaperSection(
            paper.getId(),
            order,
            sectionCommand.title(),
            sectionCommand.instruction(),
            null,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var questionIds = sectionCommand.questionIds();
        for (int i = 0; i < questionIds.size(); i++) {
            var questionId = questionIds.get(i);
            var slot = examBlueprintSlotRepository.save(new ExamBlueprintSlot(
                section.getId(),
                version.getId(),
                i + 1,
                BigDecimal.ONE,
                null,
                null,
                ExamBlueprintSlotType.FIXED,
                questionId,
                null,
                now,
                now,
                currentUserId,
                currentUserId
            ));
            examPaperItemRepository.save(new ExamPaperItem(
                slot.getId(),
                paperSection.getId(),
                paper.getId(),
                questionId,
                slot.getOrder(),
                BigDecimal.ONE
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questionId, examId, ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void requirePrivateBlueprint(Exam exam) {
        boolean sharedWithOtherExam = examRepository.findAllByBlueprintId(exam.getBlueprintId()).stream()
            .anyMatch(other -> !other.getId().equals(exam.getId()));
        if (sharedWithOtherExam) {
            throw new IllegalStateException(
                "Blueprint đang được dùng chung cho kỳ thi/bài kiểm tra khác, không thể sửa câu hỏi trực tiếp ở đây");
        }
    }
}
