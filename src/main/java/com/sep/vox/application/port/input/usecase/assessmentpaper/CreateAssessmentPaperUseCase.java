package com.sep.vox.application.port.input.usecase.assessmentpaper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ClassTestSectionWeightPolicy;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperDto;
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
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
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

    public CreateAssessmentPaperUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperAuthoringAccessService = assessmentPaperAuthoringAccessService;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentPaperDto execute(CreateAssessmentPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        var source = input.source() == null ? "blueprint" : input.source();
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

        var version = assessmentBlueprintVersionRepository.findById(assessment.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint đã chốt"));
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

        var slotsBySectionId = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));

        validateVersionWeights(sections, slotsBySectionId);

        var now = Instant.now();
        var variant = assessmentPaperRepository.nextVariant(assessment.getId());
        var paper = assessmentPaperRepository.save(new AssessmentPaper(
            assessment.getId(),
            assessment.getBlueprintVersionId(),
            assessment.getCode() + "-P" + variant,
            variant,
            AssessmentPaperStatus.DRAFT,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        for (var section : sections) {
            var savedSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
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
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();

            for (var slot : slots) {
                var questionId = slot.getSlotType() == AssessmentBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    var fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi cố định trong slot"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                            throw new IllegalStateException(
                                "Câu hỏi " + fixedQuestion.getCode() + " trong khung chưa PUBLISHED, không thể sinh đề thi");
                        }
                        // Bài trên lớp: câu hỏi fixed đã bị archived sau khi blueprint publish — để trống
                        // ô này, CHAIR tự chọn câu khác qua picker thay vì chặn hẳn việc tạo mã đề.
                        assessmentPaperItemRepository.save(new AssessmentPaperItem(
                            null,
                            savedSection.getId(),
                            paper.getId(),
                            null,
                            slot.getOrder(),
                            slot.getWeight()
                        ));
                        continue;
                    }
                }
                assessmentPaperItemRepository.save(new AssessmentPaperItem(
                    slot.getId(),
                    savedSection.getId(),
                    paper.getId(),
                    questionId,
                    slot.getOrder(),
                    slot.getWeight()
                ));
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
        return AssessmentPaperDto.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /**
     * Soạn câu hỏi trực tiếp — chỉ bài kiểm tra trên lớp mới có, và chỉ khi bài không gắn blueprint
     * dùng chung (gắn rồi thì mọi mã đề phải khớp blueprint, đổi câu phải qua đổi version).
     *
     * <p>Không sinh blueprint ẩn nào: thao tác thẳng trên AssessmentPaperSection/AssessmentPaperItem để bài trên
     * lớp thực sự tự do.
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

        var seenQuestionIds = new HashSet<UUID>();
        var questionsBySection = new ArrayList<List<Question>>();
        for (var section : sections) {
            if (section.questions() == null || section.questions().isEmpty()) {
                throw new IllegalStateException("Mỗi phần phải có ít nhất 1 câu hỏi");
            }
            var questions = new ArrayList<Question>();
            for (var questionCommand : section.questions()) {
                if (!seenQuestionIds.add(questionCommand.questionId())) {
                    throw new IllegalStateException("Một câu hỏi không thể xuất hiện nhiều lần trong cùng 1 mã đề");
                }
                var question = questionRepository
                    .findAccessibleById(questionCommand.questionId(), currentUserId, assessment.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException(
                        "Bạn không có quyền sử dụng câu hỏi " + questionCommand.questionId()));
                requireCanUseQuestion(question, currentUserId);
                questions.add(question);
            }
            questionsBySection.add(questions);
        }
        // Chiếu thời lượng TRƯỚC khi ghi để từ chối sớm -- mã đề chưa tồn tại nên không dùng lại được
        // RecalculateAssessmentTimeDurationService. Phải là totalSeconds (đã gồm media) vì đây là thước đo
        // ĐỘ DÀI bài thi so với gói, xem PaperTimeCalculator.
        var totalSeconds = paperTotalSeconds(questionsBySection.stream().flatMap(l -> l.stream()).toList());
        assessmentTimeQuotaGuardService.requireWithinPlan(assessment.getSchoolId(), totalSeconds, "Bài kiểm tra trên lớp");

        var sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(sections);
        var now = Instant.now();
        var variant = assessmentPaperRepository.nextVariant(assessment.getId());
        var paper = assessmentPaperRepository.save(new AssessmentPaper(
            assessment.getId(),
            null,
            assessment.getCode() + "-P" + variant,
            variant,
            AssessmentPaperStatus.DRAFT,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        for (int i = 0; i < sections.size(); i++) {
            var sectionCommand = sections.get(i);
            var questionWeights = ClassTestSectionWeightPolicy.resolveQuestionWeights(sectionCommand.questions());
            var savedSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
                paper.getId(),
                i + 1,
                sectionCommand.title(),
                sectionCommand.instruction(),
                null,
                sectionWeights.get(i),
                now,
                now,
                currentUserId,
                currentUserId
            ));

            var questions = questionsBySection.get(i);
            for (int j = 0; j < questions.size(); j++) {
                assessmentPaperItemRepository.save(new AssessmentPaperItem(
                    null,
                    savedSection.getId(),
                    paper.getId(),
                    questions.get(j).getId(),
                    j + 1,
                    questionWeights.get(j)
                ));
                assessmentQuestionSecureLockService.lockQuestionForAssessment(
                    questions.get(j).getId(),
                    assessment.getId(),
                    releaseModeFor(assessment),
                    currentUserId
                );
            }
        }

        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentPaperDto.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /** Thời gian thật của mã đề, gồm cả thời lượng phát AUDIO/VIDEO -- xem {@link PaperTimeCalculator}. */
    private int paperTotalSeconds(List<Question> questions) {
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }

    private void requireCanUseQuestion(Question question, UUID currentUserId) {
        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (isOwner || isSchoolShared) {
            return;
        }
        var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
        if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
            throw new ForbiddenException("Quyền READ_ONLY không được phép dùng câu hỏi trong bài kiểm tra");
        }
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateVersionWeights(List<AssessmentBlueprintSection> sections, Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId) {
        var sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể sinh đề thi");
        }
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
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
        var sourcePaper = assessmentPaperRepository.findById(copyFromPaperId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi nguồn để sao chép"));
        if (!sourcePaper.getAssessmentId().equals(assessment.getId())) {
            throw new IllegalStateException("Đề thi nguồn không thuộc cùng bài kiểm tra");
        }
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            sourcePaper.getTimeDurationSeconds(),
            "Mã đề sao chép " + sourcePaper.getCode()
        );

        var now = Instant.now();
        var variant = assessmentPaperRepository.nextVariant(assessment.getId());
        var paper = assessmentPaperRepository.save(new AssessmentPaper(
            assessment.getId(),
            sourcePaper.getBlueprintVersionId(),
            assessment.getCode() + "-P" + variant,
            variant,
            AssessmentPaperStatus.DRAFT,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var sourceSections = assessmentPaperSectionRepository.findByPaperId(sourcePaper.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();

        for (var section : sourceSections) {
            var savedSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
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

            var items = assessmentPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(item -> item.getOrder()))
                .toList();

            for (var item : items) {
                assessmentPaperItemRepository.save(new AssessmentPaperItem(
                    item.getBlueprintSlotId(),
                    savedSection.getId(),
                    paper.getId(),
                    item.getQuestionId(),
                    item.getOrder(),
                    item.getWeight()
                ));
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
        return AssessmentPaperDto.toDto(assessmentPaperRepository.findById(paper.getId()).orElse(paper));
    }
}
