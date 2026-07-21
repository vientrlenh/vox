package com.sep.vox.application.port.input.usecase.exampaper;

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
import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.question.QuestionStatus;
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
public class CreateExamPaperUseCase implements IUseCase<CreateExamPaperCommand, ExamPaperDto> {

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
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public CreateExamPaperUseCase(
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
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
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
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperDto execute(CreateExamPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Bài kiểm tra trên lớp không tạo đề qua endpoint này");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.AUTHOR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var source = input.source() == null ? "blueprint" : input.source();
        return switch (source) {
            case "blueprint" -> createFromBlueprint(exam, currentUserId);
            case "copy" -> createFromCopy(exam, input.copyFromPaperId(), currentUserId);
            default -> throw new IllegalStateException("Source không hợp lệ, chỉ hỗ trợ blueprint hoặc copy");
        };
    }

    private ExamPaperDto createFromBlueprint(Exam exam, UUID currentUserId) {
        if (exam.getBlueprintId() == null) {
            throw new IllegalStateException("Bài kiểm tra chưa gắn blueprint");
        }
        if (exam.getBlueprintVersionId() == null) {
            throw new IllegalStateException("CHAIR chưa chốt version blueprint cho kỳ thi này");
        }

        var version = examBlueprintVersionRepository.findById(exam.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint đã chốt"));

        List<ExamBlueprintSection> sections = examBlueprintSectionRepository
            .findByBlueprintVersionId(version.getId())
            .stream()
            .sorted(Comparator.comparingInt(ExamBlueprintSection::getOrder))
            .toList();

        var slotsBySectionId = examBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(ExamBlueprintSlot::getSectionId));

        validateVersionWeights(sections, slotsBySectionId);

        var now = OffsetDateTime.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            exam.getBlueprintVersionId(),
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        for (var section : sections) {
            var savedSection = examPaperSectionRepository.save(new ExamPaperSection(
                paper.getId(),
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                section.getSectionWeight(),
                now,
                now,
                currentUserId,
                currentUserId
            ));

            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(ExamBlueprintSlot::getOrder))
                .toList();

            for (var slot : slots) {
                var questionId = slot.getSlotType() == ExamBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    var fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong slot"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        throw new IllegalStateException(
                            "Câu hỏi " + fixedQuestion.getCode() + " trong khung chưa PUBLISHED, không thể sinh đề thi");
                    }
                }
                examPaperItemRepository.save(new ExamPaperItem(
                    slot.getId(),
                    savedSection.getId(),
                    paper.getId(),
                    questionId,
                    slot.getOrder(),
                    slot.getWeight()
                ));
                if (questionId != null) {
                    examQuestionSecureLockService.lockQuestionForExam(
                        questionId,
                        exam.getId(),
                        ExamSecurePoolReleaseMode.MANUAL,
                        currentUserId
                    );
                }
            }
        }

        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamPaperDtoMapper.toDto(paper);
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateVersionWeights(List<ExamBlueprintSection> sections, Map<UUID, List<ExamBlueprintSlot>> slotsBySectionId) {
        var sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể sinh đề thi");
        }
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ, không thể sinh đề thi");
            }
        }
    }

    private ExamPaperDto createFromCopy(Exam exam, UUID copyFromPaperId, UUID currentUserId) {
        if (copyFromPaperId == null) {
            throw new IllegalStateException("Phải cung cấp copyFromPaperId khi source là copy");
        }
        var sourcePaper = examPaperRepository.findById(copyFromPaperId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi nguồn để sao chép"));
        if (!sourcePaper.getExamId().equals(exam.getId())) {
            throw new IllegalStateException("Đề thi nguồn không thuộc cùng bài kiểm tra");
        }

        var now = OffsetDateTime.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            sourcePaper.getBlueprintVersionId(),
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var sourceSections = examPaperSectionRepository.findByPaperId(sourcePaper.getId()).stream()
            .sorted(Comparator.comparingInt(ExamPaperSection::getOrder))
            .toList();

        for (var section : sourceSections) {
            var savedSection = examPaperSectionRepository.save(new ExamPaperSection(
                paper.getId(),
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                section.getWeight(),
                now,
                now,
                currentUserId,
                currentUserId
            ));

            var items = examPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(ExamPaperItem::getOrder))
                .toList();

            for (var item : items) {
                examPaperItemRepository.save(new ExamPaperItem(
                    item.getBlueprintSlotId(),
                    savedSection.getId(),
                    paper.getId(),
                    item.getQuestionId(),
                    item.getOrder(),
                    item.getWeight()
                ));
                if (item.getQuestionId() != null) {
                    examQuestionSecureLockService.lockQuestionForExam(
                        item.getQuestionId(),
                        exam.getId(),
                        ExamSecurePoolReleaseMode.MANUAL,
                        currentUserId
                    );
                }
            }
        }

        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamPaperDtoMapper.toDto(paper);
    }
}
