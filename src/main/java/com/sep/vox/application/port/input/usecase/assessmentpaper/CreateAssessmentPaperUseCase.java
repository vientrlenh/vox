package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.assessment.ClassTestSectionWeightPolicy;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.mapper.AssessmentPaperDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
@RequiredArgsConstructor
public class CreateAssessmentPaperUseCase implements IUseCase<CreateAssessmentPaperCommand, AssessmentPaperDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperDto execute(CreateAssessmentPaperCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        String source = input.source() == null ? "blueprint" : input.source();
        return switch (source) {
            case "blueprint" -> createFromBlueprint(assessment, currentUserId);
            case "copy" -> createFromCopy(assessment, input.copyFromPaperId(), currentUserId);
            case "questions" -> createFromQuestions(assessment, input.sections(), currentUserId);
            default -> throw new IllegalStateException("Source không hợp lệ, chỉ hỗ trợ blueprint, copy hoặc questions");
        };
    }

    /**
     * Câu hỏi của bài trên lớp tự mở khoá khi đóng bài; kỳ thi tập trung do người quản lý tự mở.
     */
    private AssessmentSecurePoolReleaseMode releaseModeFor(Assessment assessment) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST
            ? AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE
            : AssessmentSecurePoolReleaseMode.MANUAL;
    }

    private AssessmentPaperDto createFromBlueprint(Assessment assessment, UUID currentUserId) {
        if (assessment.getBlueprintId() == null) {
            throw new IllegalStateException("Bài kiểm tra chưa gắn blueprint");
        }
        if (assessment.getBlueprintVersionId() == null) {
            throw new IllegalStateException("CHAIR chưa chốt version blueprint cho kỳ thi này");
        }

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(assessment.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint đã chốt"));
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Mã đề tạo từ blueprint " + version.getCode()
        );

        List<AssessmentBlueprintSection> sections = assessmentBlueprintSectionRepository
            .findByBlueprintVersionId(version.getId())
            .stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();

        Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));

        validateVersionWeights(sections, slotsBySectionId);

        int variant = assessmentPaperRepository.nextVariant(assessment.getId());
        AssessmentPaper paper = assessmentPaperRepository.save(AssessmentPaper.builder()
            .assessmentId(assessment.getId())
            .blueprintVersionId(assessment.getBlueprintVersionId())
            .code(assessment.getCode() + "-P" + variant)
            .variant(variant)
            .status(AssessmentPaperStatus.DRAFT)
            .timeDurationSeconds(0)
            .build());

        for (AssessmentBlueprintSection section : sections) {
            AssessmentPaperSection savedSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.builder()
                .paperId(paper.getId())
                .order(section.getOrder())
                .title(section.getTitle())
                .instruction(section.getInstruction())
                .sectionTimeLimitSeconds(section.getSectionTimeLimitSeconds())
                .weight(section.getSectionWeight())
                .build());

            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();

            for (AssessmentBlueprintSlot slot : slots) {
                UUID questionId = slot.getSlotType() == AssessmentBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    Question fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong slot"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                            throw new IllegalStateException(
                                "Câu hỏi " + fixedQuestion.getCode() + " trong khung chưa PUBLISHED, không thể sinh đề thi");
                        }
                        // Class test: the fixed question was archived after the blueprint was published. Leave this
                        // slot empty so the CHAIR picks another question through the picker, instead of blocking the paper variant altogether.
                        assessmentPaperItemRepository.save(AssessmentPaperItem.builder()
                            .sectionId(savedSection.getId())
                            .paperId(paper.getId())
                            .order(slot.getOrder())
                            .weight(slot.getWeight())
                            .build());
                        continue;
                    }
                }
                assessmentPaperItemRepository.save(AssessmentPaperItem.builder()
                    .blueprintSlotId(slot.getId())
                    .sectionId(savedSection.getId())
                    .paperId(paper.getId())
                    .questionId(questionId)
                    .order(slot.getOrder())
                    .weight(slot.getWeight())
                    .build());
                if (questionId != null) {
                    assessmentQuestionSecureLockService.lockQuestionForAssessment(
                        questionId,
                        assessment.getId(),
                        releaseModeFor(assessment),
                        currentUserId
                    );
                }
            }
        }

        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentPaperDtoMapper.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /**
     * Authoring direct questions -- only class tests have it, and only when the test is not attached to a shared
     * blueprint (once attached, every paper variant must match the blueprint and changing a question has to go through a version change).
     *
     * <p>Does not create any hidden blueprint: it operates directly on AssessmentPaperSection/AssessmentPaperItem so that a class
     * test is truly free-form.
     */
    private AssessmentPaperDto createFromQuestions(Assessment assessment, List<ClassTestSectionCommand> sections, UUID currentUserId) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ bài kiểm tra trên lớp mới được soạn câu hỏi trực tiếp");
        }
        if (assessment.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể soạn câu hỏi trực tiếp — tạo mã đề từ blueprint hoặc sao chép mã đề có sẵn");
        }
        if (sections == null || sections.isEmpty()) {
            throw new IllegalStateException("Phải có ít nhất một phần trong đề");
        }

        Set<UUID> seenQuestionIds = new HashSet<UUID>();
        List<List<Question>> questionsBySection = new ArrayList<List<Question>>();
        for (ClassTestSectionCommand section : sections) {
            if (section.questions() == null || section.questions().isEmpty()) {
                throw new IllegalStateException("Mỗi phần phải có ít nhất 1 câu hỏi");
            }
            List<Question> questions = new ArrayList<Question>();
            for (ClassTestQuestionCommand questionCommand : section.questions()) {
                if (!seenQuestionIds.add(questionCommand.questionId())) {
                    throw new IllegalStateException("Một câu hỏi không thể xuất hiện nhiều lần trong cùng 1 mã đề");
                }
                Question question = questionRepository
                    .findAccessibleById(questionCommand.questionId(), currentUserId, assessment.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException(
                        "Bạn không có quyền sử dụng câu hỏi " + questionCommand.questionId()));
                requireCanUseQuestion(question, currentUserId);
                questions.add(question);
            }
            questionsBySection.add(questions);
        }
        // Projects the duration BEFORE writing to reject early -- the paper variant does not exist yet so
        // RecalculateAssessmentTimeDurationService cannot be reused. It must be totalSeconds (which includes media) because this is the
        // LENGTH yardstick of the assessment against the plan, see PaperTimeCalculator.
        int totalSeconds = paperTotalSeconds(questionsBySection.stream().flatMap(l -> l.stream()).toList());
        assessmentTimeQuotaGuardService.requireWithinPlan(assessment.getSchoolId(), totalSeconds, "Bài kiểm tra trên lớp");

        List<BigDecimal> sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(sections);
        int variant = assessmentPaperRepository.nextVariant(assessment.getId());
        AssessmentPaper paper = assessmentPaperRepository.save(AssessmentPaper.builder()
            .assessmentId(assessment.getId())
            .code(assessment.getCode() + "-P" + variant)
            .variant(variant)
            .status(AssessmentPaperStatus.DRAFT)
            .timeDurationSeconds(0)
            .build());

        for (int i = 0; i < sections.size(); i++) {
            ClassTestSectionCommand sectionCommand = sections.get(i);
            List<BigDecimal> questionWeights = ClassTestSectionWeightPolicy.resolveQuestionWeights(sectionCommand.questions());
            AssessmentPaperSection savedSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.builder()
                .paperId(paper.getId())
                .order(i + 1)
                .title(sectionCommand.title())
                .instruction(sectionCommand.instruction())
                .weight(sectionWeights.get(i))
                .build());

            List<Question> questions = questionsBySection.get(i);
            for (int j = 0; j < questions.size(); j++) {
                assessmentPaperItemRepository.save(AssessmentPaperItem.builder()
                    .sectionId(savedSection.getId())
                    .paperId(paper.getId())
                    .questionId(questions.get(j).getId())
                    .order(j + 1)
                    .weight(questionWeights.get(j))
                    .build());
                assessmentQuestionSecureLockService.lockQuestionForAssessment(
                    questions.get(j).getId(),
                    assessment.getId(),
                    releaseModeFor(assessment),
                    currentUserId
                );
            }
        }

        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentPaperDtoMapper.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /** Thời gian thật của mã đề, gồm cả thời lượng phát AUDIO/VIDEO -- xem {@link PaperTimeCalculator}. */
    private int paperTotalSeconds(List<Question> questions) {
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }

    private void requireCanUseQuestion(Question question, UUID currentUserId) {
        boolean isOwner = question.isCreatedBy(currentUserId);
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (isOwner || isSchoolShared) {
            return;
        }
        Optional<QuestionCollaborator> collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
        if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
            throw new ForbiddenException("Quyền READ_ONLY không được phép dùng câu hỏi trong bài kiểm tra");
        }
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateVersionWeights(List<AssessmentBlueprintSection> sections, Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId) {
        BigDecimal sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể sinh đề thi");
        }
        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            BigDecimal slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ, không thể sinh đề thi");
            }
        }
    }

    private AssessmentPaperDto createFromCopy(Assessment assessment, UUID copyFromPaperId, UUID currentUserId) {
        if (copyFromPaperId == null) {
            throw new IllegalStateException("Phải cung cấp copyFromPaperId khi source là copy");
        }
        AssessmentPaper sourcePaper = assessmentPaperRepository.findById(copyFromPaperId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi nguồn để sao chép"));
        if (!sourcePaper.getAssessmentId().equals(assessment.getId())) {
            throw new IllegalStateException("Đề thi nguồn không thuộc cùng bài kiểm tra");
        }
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            sourcePaper.getTimeDurationSeconds(),
            "Mã đề sao chép " + sourcePaper.getCode()
        );

        int variant = assessmentPaperRepository.nextVariant(assessment.getId());
        AssessmentPaper paper = assessmentPaperRepository.save(AssessmentPaper.builder()
            .assessmentId(assessment.getId())
            .blueprintVersionId(sourcePaper.getBlueprintVersionId())
            .code(assessment.getCode() + "-P" + variant)
            .variant(variant)
            .status(AssessmentPaperStatus.DRAFT)
            .timeDurationSeconds(0)
            .build());

        List<AssessmentPaperSection> sourceSections = assessmentPaperSectionRepository.findByPaperId(sourcePaper.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();

        for (AssessmentPaperSection section : sourceSections) {
            AssessmentPaperSection savedSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.builder()
                .paperId(paper.getId())
                .order(section.getOrder())
                .title(section.getTitle())
                .instruction(section.getInstruction())
                .sectionTimeLimitSeconds(section.getSectionTimeLimitSeconds())
                .weight(section.getWeight())
                .build());

            List<AssessmentPaperItem> items = assessmentPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(item -> item.getOrder()))
                .toList();

            for (AssessmentPaperItem item : items) {
                assessmentPaperItemRepository.save(AssessmentPaperItem.builder()
                    .blueprintSlotId(item.getBlueprintSlotId())
                    .sectionId(savedSection.getId())
                    .paperId(paper.getId())
                    .questionId(item.getQuestionId())
                    .order(item.getOrder())
                    .weight(item.getWeight())
                    .build());
                if (item.getQuestionId() != null) {
                    assessmentQuestionSecureLockService.lockQuestionForAssessment(
                        item.getQuestionId(),
                        assessment.getId(),
                        releaseModeFor(assessment),
                        currentUserId
                    );
                }
            }
        }

        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentPaperDtoMapper.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }
}
