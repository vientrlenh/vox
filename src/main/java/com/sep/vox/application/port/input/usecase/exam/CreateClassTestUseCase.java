package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.exam.CreateClassTestResponse;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

@Service
public class CreateClassTestUseCase implements IUseCase<CreateClassTestCommand, CreateClassTestResponse> {

    private static final int MAX_CLASS_ROSTER_SIZE = 2000;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UpdateExamStatusUseCase updateExamStatusUseCase;
    private final UserContextPort userContextPort;

    public CreateClassTestUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamMemberRepository examMemberRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UpdateExamStatusUseCase updateExamStatusUseCase,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examMemberRepository = examMemberRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.updateExamStatusUseCase = updateExamStatusUseCase;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateClassTestResponse execute(CreateClassTestCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .noneMatch(role -> "TEACHER".equals(role.roleCode()))) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var schoolClass = schoolClassRepository.findById(command.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        var membership = schoolClassUserRepository.findByUserIdAndSchoolClassId(currentUserId, schoolClass.getId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        if (!membership.isActive()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        validateInputMode(command);
        validateOpenClose(command.openAt(), command.closeAt());

        var now = OffsetDateTime.now();
        if (command.existingBlueprintId() != null && command.existingBlueprintVersionId() != null) {
            return executeWithExistingBlueprint(command, schoolClass, currentUserId, now);
        }
        return executeWithNewBlueprint(command, schoolClass, currentUserId, now);
    }

    private CreateClassTestResponse executeWithNewBlueprint(
            CreateClassTestCommand command,
            SchoolClass schoolClass,
            UUID currentUserId,
            OffsetDateTime now) {
        // Chế độ "câu hỏi trực tiếp": không tạo blueprint ẩn nào — thao tác thẳng trên ExamPaperSection/ExamPaperItem
        // để bài trên lớp thực sự tự do, không phụ thuộc lớp blueprint khi không dùng blueprint dùng chung.
        var exam = createExam(null, null, schoolClass, command, currentUserId, now);
        var paper = createPaper(exam, null, now, currentUserId);

        for (int i = 0; i < command.sections().size(); i++) {
            var sectionCommand = command.sections().get(i);
            List<Question> questions = new ArrayList<>();
            for (var questionId : sectionCommand.questionIds()) {
                var question = questionRepository
                    .findAccessibleById(questionId, currentUserId, schoolClass.getSchoolId(), false, false)
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền sử dụng câu hỏi " + questionId));
                validateCanUseQuestion(question, currentUserId);
                validateQuestionUnlocked(question, currentUserId);
                questions.add(question);
            }

            var paperSection = examPaperSectionRepository.save(new ExamPaperSection(
                paper.getId(),
                i + 1,
                sectionCommand.title(),
                sectionCommand.instruction(),
                null,
                now,
                now,
                currentUserId,
                currentUserId
            ));
            createPaperItemsDirect(paperSection, questions, exam.getId(), currentUserId);
        }

        examMemberRepository.save(new ExamMember(exam.getId(), currentUserId, ExamMemberRole.CHAIR, now, currentUserId));
        var candidateCount = assignCandidates(exam, paper, schoolClass.getId(), currentUserId, now);
        var examDto = scheduleAndMaybeStart(exam, command);
        return new CreateClassTestResponse(examDto, paper.getId(), candidateCount);
    }

    private CreateClassTestResponse executeWithExistingBlueprint(
            CreateClassTestCommand command,
            SchoolClass schoolClass,
            UUID currentUserId,
            OffsetDateTime now) {
        var blueprint = examBlueprintRepository.findById(command.existingBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(schoolClass.getSchoolId())) {
            throw new IllegalStateException("Blueprint không thuộc trường của giáo viên");
        }

        var version = examBlueprintVersionRepository.findById(command.existingBlueprintVersionId())
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

        var exam = createExam(blueprint.getId(), version.getId(), schoolClass, command, currentUserId, now);
        var paper = createPaper(exam, version.getId(), now, currentUserId);

        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();
            validateReusableSlots(slots, currentUserId);

            var paperSection = createPaperSection(paper, section, now, currentUserId);
            createPaperItems(paperSection, slots, exam.getId(), currentUserId);
        }

        examMemberRepository.save(new ExamMember(exam.getId(), currentUserId, ExamMemberRole.CHAIR, now, currentUserId));
        var candidateCount = assignCandidates(exam, paper, schoolClass.getId(), currentUserId, now);
        var examDto = scheduleAndMaybeStart(exam, command);
        return new CreateClassTestResponse(examDto, paper.getId(), candidateCount);
    }

    // Exam luôn được tạo ở DRAFT (xem createExam) rồi chuyển tiếp qua đúng pipeline SCHEDULE/START
    // của UpdateExamStatusUseCase để validatePlanLimits (giới hạn học sinh/thời lượng/token GRADING
    // theo subscription) thực sự chạy, thay vì set thẳng SCHEDULED/IN_PROGRESS lúc tạo như trước.
    private ExamDto scheduleAndMaybeStart(Exam exam, CreateClassTestCommand command) {
        var scheduled = updateExamStatusUseCase.execute(new UpdateExamStatusCommand(exam.getId(), "SCHEDULE", null));
        if (command.openAt() != null) {
            return scheduled;
        }
        return updateExamStatusUseCase.execute(new UpdateExamStatusCommand(exam.getId(), "START", null));
    }

    private CreateClassTestCommand normalize(CreateClassTestCommand input) {
        return new CreateClassTestCommand(
            input.schoolClassId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.openAt(),
            input.closeAt(),
            input.sections() == null ? List.of() : input.sections().stream()
                .map(section -> new ClassTestSectionCommand(
                    StringNormalization.trimAndCollapseSpaces(section.title()),
                    StringNormalization.trimAndCollapseSpaces(section.instruction()),
                    section.questionIds() == null ? List.of() : section.questionIds()
                ))
                .toList(),
            input.existingBlueprintId(),
            input.existingBlueprintVersionId(),
            input.maxAttempt(),
            input.examTimeDurationSecond(),
            input.resultDecisionMethod()
        );
    }

    private void validateInputMode(CreateClassTestCommand command) {
        boolean hasQuestions = !command.sections().isEmpty();
        boolean hasExistingBlueprint = command.existingBlueprintId() != null || command.existingBlueprintVersionId() != null;

        if (!hasQuestions && !hasExistingBlueprint) {
            throw new IllegalStateException("Phải cung cấp sections hoặc existing blueprint");
        }
        if (hasQuestions && hasExistingBlueprint) {
            throw new IllegalStateException("Chỉ được chọn một cách tạo bài kiểm tra");
        }
        if (!hasQuestions && (command.existingBlueprintId() == null || command.existingBlueprintVersionId() == null)) {
            throw new IllegalStateException("Phải cung cấp đầy đủ existingBlueprintId và existingBlueprintVersionId");
        }
        if (hasQuestions) {
            var seenQuestionIds = new java.util.HashSet<UUID>();
            for (var section : command.sections()) {
                if (section.questionIds() == null || section.questionIds().isEmpty()) {
                    throw new IllegalStateException("Mỗi section phải có ít nhất 1 câu hỏi");
                }
                for (var questionId : section.questionIds()) {
                    if (!seenQuestionIds.add(questionId)) {
                        throw new IllegalStateException("Một câu hỏi không thể xuất hiện nhiều lần trong cùng 1 bài kiểm tra");
                    }
                }
            }
        }
    }

    private void createPaperItemsDirect(
            ExamPaperSection paperSection,
            List<Question> questions,
            UUID examId,
            UUID currentUserId) {
        var weights = distributeEqualWeights(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            examPaperItemRepository.save(new ExamPaperItem(
                null,
                paperSection.getId(),
                paperSection.getPaperId(),
                questions.get(i).getId(),
                i + 1,
                weights.get(i)
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questions.get(i).getId(),
                examId,
                ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE,
                currentUserId
            );
        }
    }

    private List<BigDecimal> distributeEqualWeights(int count) {
        var weights = new ArrayList<BigDecimal>();
        var perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        var runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }

    private Exam createExam(
            UUID blueprintId,
            UUID blueprintVersionId,
            SchoolClass schoolClass,
            CreateClassTestCommand command,
            UUID currentUserId,
            OffsetDateTime now) {
        var code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        var openAt = parseDateTime(command.openAt());
        var closeAt = parseDateTime(command.closeAt());
        Integer requestedMaxAttempt = command.maxAttempt();
        int maxAttempt = requestedMaxAttempt == null ? 1 : requestedMaxAttempt;
        // Luôn tạo ở DRAFT — scheduleAndMaybeStart() sẽ chuyển tiếp qua SCHEDULE (và START nếu start ngay)
        // ngay sau khi exam được setup xong, để validatePlanLimits có cơ hội chạy trước khi vào SCHEDULED/IN_PROGRESS.
        return examRepository.save(new Exam(
            blueprintId,
            blueprintVersionId,
            code,
            command.name(),
            command.description(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            ExamKind.CLASS_TEST,
            ExamDeliveryMode.STUDENT_DEVICE,
            ExamStatus.DRAFT,
            maxAttempt,
            command.examTimeDurationSecond(),
            command.resultDecisionMethod() == null ? ResultDecisionMethod.HIGHEST : command.resultDecisionMethod(),
            null, 
            null, 
            openAt,
            closeAt,
            null,
            now,
            now,
            currentUserId,
            currentUserId
        ));
    }

    private ExamPaper createPaper(Exam exam, UUID blueprintVersionId, OffsetDateTime now, UUID currentUserId) {
        return examPaperRepository.save(new ExamPaper(
            exam.getId(),
            blueprintVersionId,
            exam.getCode() + "-P1",
            1,
            ExamPaperStatus.LOCKED,
            now,
            now,
            currentUserId,
            currentUserId
        ));
    }

    private ExamPaperSection createPaperSection(ExamPaper paper, ExamBlueprintSection section, OffsetDateTime now, UUID currentUserId) {
        return examPaperSectionRepository.save(new ExamPaperSection(
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
    }

    private void createPaperItems(
            ExamPaperSection paperSection,
            List<ExamBlueprintSlot> slots,
            UUID examId,
            UUID currentUserId) {
        for (var slot : slots) {
            var questionId = slot.getSlotType() == ExamBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
            examPaperItemRepository.save(new ExamPaperItem(
                slot.getId(),
                paperSection.getId(),
                paperSection.getPaperId(),
                questionId,
                slot.getOrder(),
                slot.getWeight()
            ));
            if (questionId != null) {
                examQuestionSecureLockService.lockQuestionForExam(
                    questionId,
                    examId,
                    ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE,
                    currentUserId
                );
            }
        }
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateVersionWeights(List<ExamBlueprintSection> sections, Map<UUID, List<ExamBlueprintSlot>> slotsBySectionId) {
        var sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể tạo bài kiểm tra");
        }
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ, không thể tạo bài kiểm tra");
            }
        }
    }

    private void validateReusableSlots(List<ExamBlueprintSlot> slots, UUID currentUserId) {
        for (var slot : slots) {
            if (slot.getSlotType() == ExamBlueprintSlotType.SELECTION) {
                continue;
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalStateException("Ô câu hỏi cố định trong blueprint không có câu hỏi nào được gán");
            }
            var question = questionRepository.findById(slot.getFixedQuestionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong blueprint"));
            validateQuestionUnlocked(question, currentUserId);
        }
    }

    private void validateCanUseQuestion(Question question, UUID currentUserId) {
        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("Quyền READ_ONLY không được phép dùng câu hỏi trong bài kiểm tra");
            }
        }
    }

    private void validateQuestionUnlocked(Question question, UUID currentUserId) {
        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        if (question.isLocked() && !isOwner) {
            throw new IllegalStateException("Câu hỏi " + question.getCode() + " đang bị khóa bởi kỳ thi khác");
        }
    }

    private int assignCandidates(Exam exam, ExamPaper paper, UUID schoolClassId, UUID currentUserId, OffsetDateTime now) {
        var roster = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 1, MAX_CLASS_ROSTER_SIZE).content();
        var candidates = new ArrayList<ExamCandidate>();
        for (var classUser : roster) {
            if (!classUser.isActive()) {
                continue;
            }
            var isStudent = userRoleQueryRepository.findByUserIdWithRoleInfo(classUser.getUserId()).stream()
                .anyMatch(role -> "STUDENT".equals(role.roleCode()));
            if (!isStudent) {
                continue;
            }
            candidates.add(new ExamCandidate(
                exam.getId(),
                classUser.getUserId(),
                paper.getId(),
                null,
                ExamCandidateStatus.ASSIGNED,
                now,
                now,
                currentUserId,
                currentUserId
            ));
        }
        return examCandidateRepository.saveAll(candidates).size();
    }

    private void validateOpenClose(String openAt, String closeAt) {
        if (openAt == null || closeAt == null) {
            return;
        }
        if (!OffsetDateTime.parse(openAt).isBefore(OffsetDateTime.parse(closeAt))) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
