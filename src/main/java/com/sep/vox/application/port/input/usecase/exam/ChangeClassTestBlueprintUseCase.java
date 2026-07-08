package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ChangeClassTestBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ChangeClassTestBlueprintUseCase implements IUseCase<ChangeClassTestBlueprintCommand, ExamDto> {

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public ChangeClassTestBlueprintUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamBlueprintRepository examBlueprintRepository,
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
        this.examBlueprintRepository = examBlueprintRepository;
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
    public ExamDto execute(ChangeClassTestBlueprintCommand input) {
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
            throw new IllegalStateException("Chỉ được đổi blueprint khi bài kiểm tra chưa mở cho học sinh làm bài (đang ở trạng thái đã lên lịch)");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể đổi blueprint khi đã có học sinh nộp bài");
        }
        if ((input.blueprintId() == null) != (input.blueprintVersionId() == null)) {
            throw new IllegalStateException(
                "Phải cung cấp đầy đủ blueprintId và blueprintVersionId, hoặc để trống cả hai để gỡ blueprint");
        }

        var now = OffsetDateTime.now();
        var paper = examPaperRepository.findByExamId(exam.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        clearExistingPaperContent(paper, currentUserId);

        if (input.blueprintId() != null) {
            switchToExistingBlueprint(input, exam, paper, currentUserId, now);
        } else {
            // Gỡ blueprint: để trống hoàn toàn, không tạo blueprint ẩn nào — bài trở về chế độ câu hỏi trực tiếp tự do.
            exam.setBlueprintId(null);
            exam.setBlueprintVersionId(null);
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }

    private void clearExistingPaperContent(ExamPaper paper, UUID currentUserId) {
        for (var paperSection : examPaperSectionRepository.findByPaperId(paper.getId())) {
            for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
                if (item.getQuestionId() != null) {
                    examQuestionSecureLockService.unlockQuestion(item.getQuestionId(), currentUserId);
                }
                examPaperItemRepository.deleteById(item.getId());
            }
            examPaperSectionRepository.deleteById(paperSection.getId());
        }
    }

    private void switchToExistingBlueprint(
            ChangeClassTestBlueprintCommand input,
            Exam exam,
            ExamPaper paper,
            UUID currentUserId,
            OffsetDateTime now) {
        var blueprint = examBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(exam.getSchoolId())) {
            throw new IllegalStateException("Blueprint không thuộc trường của giáo viên");
        }
        var version = examBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalStateException("Version không thuộc blueprint đã chọn");
        }
        if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được dùng version đã PUBLISHED");
        }

        var sections = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        if (sections.isEmpty()) {
            throw new IllegalStateException("Blueprint version không có section nào");
        }
        var slotsBySectionId = examBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        validateVersionWeights(sections, slotsBySectionId);
        validateReusableSlots(slotsBySectionId.values().stream().flatMap(sectionSlots -> sectionSlots.stream()).toList(), currentUserId);

        exam.setBlueprintId(blueprint.getId());
        exam.setBlueprintVersionId(version.getId());

        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();
            var paperSection = examPaperSectionRepository.save(new ExamPaperSection(
                paper.getId(),
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                now,
                now,
                currentUserId,
                currentUserId
            ));
            for (var slot : slots) {
                var questionId = slot.getSlotType() == ExamBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                var item = new com.sep.vox.domain.model.exam.ExamPaperItem(
                    slot.getId(),
                    paperSection.getId(),
                    paper.getId(),
                    questionId,
                    slot.getOrder(),
                    slot.getWeight()
                );
                examPaperItemRepository.save(item);
                if (questionId != null) {
                    examQuestionSecureLockService.lockQuestionForExam(
                        questionId, exam.getId(), ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
                    );
                }
            }
        }
    }

    private void validateVersionWeights(List<ExamBlueprintSection> sections, Map<UUID, List<ExamBlueprintSlot>> slotsBySectionId) {
        var sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể dùng cho bài kiểm tra");
        }
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ");
            }
        }
    }

    private void validateReusableSlots(List<ExamBlueprintSlot> slots, UUID currentUserId) {
        for (var slot : slots) {
            if (slot.getSlotType() == ExamBlueprintSlotType.SELECTION) {
                continue;
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED phải có fixedQuestionId");
            }
            var question = questionRepository.findById(slot.getFixedQuestionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong blueprint"));
            validateQuestionUnlocked(question, currentUserId);
        }
    }

    private void validateQuestionUnlocked(Question question, UUID currentUserId) {
        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        if (question.isLocked() && !isOwner) {
            throw new IllegalStateException("Câu hỏi " + question.getCode() + " đang bị khóa bởi kỳ thi khác");
        }
    }
}
