package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperItem;
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
        if (input.questionIds() == null || input.questionIds().isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có ít nhất 1 câu hỏi");
        }

        List<UUID> questionIds = input.questionIds();
        for (var questionId : questionIds) {
            questionRepository.findAccessibleById(questionId, currentUserId, exam.getSchoolId(), false, false)
                .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi " + questionId));
        }

        var version = examBlueprintVersionRepository.findByBlueprintId(exam.getBlueprintId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var section = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section blueprint"));
        var slots = examBlueprintSlotRepository.findBySectionId(section.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var paper = examPaperRepository.findByExamId(exam.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var paperSection = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section đề thi"));
        var items = examPaperItemRepository.findBySectionId(paperSection.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var now = OffsetDateTime.now();
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
                    newQuestionId, exam.getId(), ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
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
                questionId, exam.getId(), ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }
}
