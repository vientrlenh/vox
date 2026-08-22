package com.sep.vox.application.port.input.usecase.exampaper;

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
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exam.ClassTestSectionWeightPolicy;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.exam.PaperTimeCalculator;

@Service
public class CreateExamPaperUseCase implements IUseCase<CreateExamPaperCommand, ExamPaperDto> {

    private final ExamRepository examRepository;
    private final ExamPaperAuthoringAccessService examPaperAuthoringAccessService;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final ExamTimeQuotaGuardService examTimeQuotaGuardService;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public CreateExamPaperUseCase(
            ExamRepository examRepository,
            ExamPaperAuthoringAccessService examPaperAuthoringAccessService,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            ExamTimeQuotaGuardService examTimeQuotaGuardService,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperAuthoringAccessService = examPaperAuthoringAccessService;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.examTimeQuotaGuardService = examTimeQuotaGuardService;
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperDto execute(CreateExamPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        examPaperAuthoringAccessService.requireCanAuthor(exam, currentUserId);

        var source = input.source() == null ? "blueprint" : input.source();
        return switch (source) {
            case "blueprint" -> createFromBlueprint(exam, currentUserId);
            case "copy" -> createFromCopy(exam, input.copyFromPaperId(), currentUserId);
            case "questions" -> createFromQuestions(exam, input.sections(), currentUserId);
            default -> throw new IllegalStateException("Source không hợp lệ, chỉ hỗ trợ blueprint, copy hoặc questions");
        };
    }

    /**
     * Câu hỏi của bài trên lớp tự mở khoá khi đóng bài; kỳ thi tập trung do người quản lý tự mở.
     */
    private ExamSecurePoolReleaseMode releaseModeFor(Exam exam) {
        return exam.getKind() == ExamKind.CLASS_TEST
            ? ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE
            : ExamSecurePoolReleaseMode.MANUAL;
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
        examTimeQuotaGuardService.requireWithinPlan(
            exam.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Mã đề tạo từ blueprint " + version.getCode()
        );

        List<ExamBlueprintSection> sections = examBlueprintSectionRepository
            .findByBlueprintVersionId(version.getId())
            .stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();

        var slotsBySectionId = examBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));

        validateVersionWeights(sections, slotsBySectionId);

        var now = Instant.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            exam.getBlueprintVersionId(),
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            0,
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
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();

            for (var slot : slots) {
                var questionId = slot.getSlotType() == ExamBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    var fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong slot"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        if (exam.getKind() != ExamKind.CLASS_TEST) {
                            throw new IllegalStateException(
                                "Câu hỏi " + fixedQuestion.getCode() + " trong khung chưa PUBLISHED, không thể sinh đề thi");
                        }
                        // Bài trên lớp: câu hỏi fixed đã bị archived sau khi blueprint publish — để trống
                        // ô này, CHAIR tự chọn câu khác qua picker thay vì chặn hẳn việc tạo mã đề.
                        examPaperItemRepository.save(new ExamPaperItem(
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
                        releaseModeFor(exam),
                        currentUserId
                    );
                }
            }
        }

        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamPaperDtoMapper.toDto(examPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /**
     * Soạn câu hỏi trực tiếp — chỉ bài kiểm tra trên lớp mới có, và chỉ khi bài không gắn blueprint
     * dùng chung (gắn rồi thì mọi mã đề phải khớp blueprint, đổi câu phải qua đổi version).
     *
     * <p>Không sinh blueprint ẩn nào: thao tác thẳng trên ExamPaperSection/ExamPaperItem để bài trên
     * lớp thực sự tự do.
     */
    private ExamPaperDto createFromQuestions(Exam exam, List<ClassTestSectionCommand> sections, UUID currentUserId) {
        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ bài kiểm tra trên lớp mới được soạn câu hỏi trực tiếp");
        }
        if (exam.getBlueprintId() != null) {
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
                    .findAccessibleById(questionCommand.questionId(), currentUserId, exam.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException(
                        "Bạn không có quyền sử dụng câu hỏi " + questionCommand.questionId()));
                requireCanUseQuestion(question, currentUserId);
                questions.add(question);
            }
            questionsBySection.add(questions);
        }
        // Chiếu thời lượng TRƯỚC khi ghi để từ chối sớm -- mã đề chưa tồn tại nên không dùng lại được
        // RecalculateExamTimeDurationService. Phải là totalSeconds (đã gồm media) vì đây là thước đo
        // ĐỘ DÀI bài thi so với gói, xem PaperTimeCalculator.
        var totalSeconds = paperTotalSeconds(questionsBySection.stream().flatMap(List::stream).toList());
        examTimeQuotaGuardService.requireWithinPlan(exam.getSchoolId(), totalSeconds, "Bài kiểm tra trên lớp");

        var sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(sections);
        var now = Instant.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            null,
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        for (int i = 0; i < sections.size(); i++) {
            var sectionCommand = sections.get(i);
            var questionWeights = ClassTestSectionWeightPolicy.resolveQuestionWeights(sectionCommand.questions());
            var savedSection = examPaperSectionRepository.save(new ExamPaperSection(
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
                examPaperItemRepository.save(new ExamPaperItem(
                    null,
                    savedSection.getId(),
                    paper.getId(),
                    questions.get(j).getId(),
                    j + 1,
                    questionWeights.get(j)
                ));
                examQuestionSecureLockService.lockQuestionForExam(
                    questions.get(j).getId(),
                    exam.getId(),
                    releaseModeFor(exam),
                    currentUserId
                );
            }
        }

        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamPaperDtoMapper.toDto(examPaperRepository.findById(paper.getId()).orElse(paper));
    }

    /** Thời gian thật của mã đề, gồm cả thời lượng phát AUDIO/VIDEO -- xem {@link PaperTimeCalculator}. */
    private int paperTotalSeconds(List<Question> questions) {
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(Question::getId).distinct().toList()));
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

    private void validateVersionWeights(List<ExamBlueprintSection> sections, Map<UUID, List<ExamBlueprintSlot>> slotsBySectionId) {
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

    private ExamPaperDto createFromCopy(Exam exam, UUID copyFromPaperId, UUID currentUserId) {
        if (copyFromPaperId == null) {
            throw new IllegalStateException("Phải cung cấp copyFromPaperId khi source là copy");
        }
        var sourcePaper = examPaperRepository.findById(copyFromPaperId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi nguồn để sao chép"));
        if (!sourcePaper.getExamId().equals(exam.getId())) {
            throw new IllegalStateException("Đề thi nguồn không thuộc cùng bài kiểm tra");
        }
        examTimeQuotaGuardService.requireWithinPlan(
            exam.getSchoolId(),
            sourcePaper.getTimeDurationSeconds(),
            "Mã đề sao chép " + sourcePaper.getCode()
        );

        var now = Instant.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            sourcePaper.getBlueprintVersionId(),
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var sourceSections = examPaperSectionRepository.findByPaperId(sourcePaper.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
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
                .sorted(Comparator.comparingInt(item -> item.getOrder()))
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
                        releaseModeFor(exam),
                        currentUserId
                    );
                }
            }
        }

        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamPaperDtoMapper.toDto(examPaperRepository.findById(paper.getId()).orElse(paper));
    }
}
