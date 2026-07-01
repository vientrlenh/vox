package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.exam.CreateClassTestResponse;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
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
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateClassTestResponse execute(CreateClassTestCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .noneMatch(role -> "TEACHER".equals(role.roleCode()))) {
            throw new ForbiddenException("Quyen truy cap bi tu choi");
        }

        var schoolClass = schoolClassRepository.findById(command.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay lop hoc"));

        var membership = schoolClassUserRepository.findByUserIdAndSchoolClassId(currentUserId, schoolClass.getId())
            .orElseThrow(() -> new ForbiddenException("Quyen truy cap bi tu choi"));
        if (!membership.isActive()) {
            throw new ForbiddenException("Quyen truy cap bi tu choi");
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
        List<Question> questions = new ArrayList<>();
        for (var questionId : command.questionIds()) {
            var question = questionRepository
                .findAccessibleById(questionId, currentUserId, schoolClass.getSchoolId(), false, false)
                .orElseThrow(() -> new ForbiddenException("Khong co quyen dung cau hoi " + questionId));
            validateCanUseQuestion(question, currentUserId);
            validateQuestionUnlocked(question);
            questions.add(question);
        }

        var blueprint = createBlueprint(schoolClass.getSchoolId(), schoolClass.getLanguageId(), command.name(), currentUserId, now);
        var version = createVersion(blueprint, now, currentUserId);
        var section = createSection(version, command.name(), now, currentUserId);
        var slots = createSlots(section, version, questions, now, currentUserId);

        var exam = createExam(blueprint, version, schoolClass, command, currentUserId, now);
        var paper = createPaper(exam, version.getId(), now, currentUserId);
        var paperSection = createPaperSection(paper, section, now, currentUserId);
        createPaperItems(paperSection, slots, exam.getId(), currentUserId);

        examMemberRepository.save(new ExamMember(exam.getId(), currentUserId, ExamMemberRole.CHAIR, now, currentUserId));
        var candidateCount = assignCandidates(exam, paper, schoolClass.getId(), currentUserId, now);
        return new CreateClassTestResponse(ExamDtoMapper.toDto(exam), paper.getId(), candidateCount);
    }

    private CreateClassTestResponse executeWithExistingBlueprint(
            CreateClassTestCommand command,
            SchoolClass schoolClass,
            UUID currentUserId,
            OffsetDateTime now) {
        var blueprint = examBlueprintRepository.findById(command.existingBlueprintId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay blueprint"));
        if (!blueprint.getSchoolId().equals(schoolClass.getSchoolId())) {
            throw new IllegalStateException("Blueprint khong thuoc truong cua giao vien");
        }

        var version = examBlueprintVersionRepository.findById(command.existingBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalStateException("Version khong thuoc blueprint da chon");
        }
        if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Chi duoc dung version da PUBLISHED");
        }

        var sections = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted(Comparator.comparingInt(ExamBlueprintSection::getOrder))
            .toList();
        if (sections.isEmpty()) {
            throw new IllegalStateException("Blueprint version khong co section nao");
        }

        var exam = createExam(blueprint, version, schoolClass, command, currentUserId, now);
        var paper = createPaper(exam, version.getId(), now, currentUserId);

        for (var section : sections) {
            var slots = examBlueprintSlotRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(ExamBlueprintSlot::getOrder))
                .toList();
            validateReusableSlots(slots);

            var paperSection = createPaperSection(paper, section, now, currentUserId);
            createPaperItems(paperSection, slots, exam.getId(), currentUserId);
        }

        examMemberRepository.save(new ExamMember(exam.getId(), currentUserId, ExamMemberRole.CHAIR, now, currentUserId));
        var candidateCount = assignCandidates(exam, paper, schoolClass.getId(), currentUserId, now);
        return new CreateClassTestResponse(ExamDtoMapper.toDto(exam), paper.getId(), candidateCount);
    }

    private CreateClassTestCommand normalize(CreateClassTestCommand input) {
        return new CreateClassTestCommand(
            input.schoolClassId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.openAt(),
            input.closeAt(),
            input.questionIds() == null ? List.of() : input.questionIds(),
            input.existingBlueprintId(),
            input.existingBlueprintVersionId()
        );
    }

    private void validateInputMode(CreateClassTestCommand command) {
        boolean hasQuestions = !command.questionIds().isEmpty();
        boolean hasExistingBlueprint = command.existingBlueprintId() != null || command.existingBlueprintVersionId() != null;

        if (!hasQuestions && !hasExistingBlueprint) {
            throw new IllegalStateException("Phai cung cap questionIds hoac existing blueprint");
        }
        if (hasQuestions && hasExistingBlueprint) {
            throw new IllegalStateException("Chi duoc chon mot cach tao bai kiem tra");
        }
        if (!hasQuestions && (command.existingBlueprintId() == null || command.existingBlueprintVersionId() == null)) {
            throw new IllegalStateException("Phai cung cap day du existingBlueprintId va existingBlueprintVersionId");
        }
    }

    private ExamBlueprint createBlueprint(UUID schoolId, UUID languageId, String name, UUID currentUserId, OffsetDateTime now) {
        var code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return examBlueprintRepository.save(new ExamBlueprint(
            schoolId,
            languageId,
            null,
            code,
            "Class test blueprint - " + name,
            null,
            true,
            now,
            now,
            currentUserId,
            currentUserId
        ));
    }

    private ExamBlueprintVersion createVersion(ExamBlueprint blueprint, OffsetDateTime now, UUID currentUserId) {
        return examBlueprintVersionRepository.save(new ExamBlueprintVersion(
            blueprint.getId(),
            1,
            blueprint.getCode() + "-V1",
            null,
            ExamBlueprintVersionStatus.PUBLISHED,
            null,
            now,
            null,
            now,
            now,
            currentUserId,
            currentUserId
        ));
    }

    private ExamBlueprintSection createSection(ExamBlueprintVersion version, String name, OffsetDateTime now, UUID currentUserId) {
        return examBlueprintSectionRepository.save(new ExamBlueprintSection(
            version.getId(),
            1,
            name,
            null,
            null,
            BigDecimal.ONE,
            now,
            now,
            currentUserId,
            currentUserId
        ));
    }

    private List<ExamBlueprintSlot> createSlots(
            ExamBlueprintSection section,
            ExamBlueprintVersion version,
            List<Question> questions,
            OffsetDateTime now,
            UUID currentUserId) {
        var slots = new ArrayList<ExamBlueprintSlot>();
        for (int i = 0; i < questions.size(); i++) {
            var slot = examBlueprintSlotRepository.save(new ExamBlueprintSlot(
                section.getId(),
                version.getId(),
                i + 1,
                BigDecimal.ONE,
                null,
                null,
                ExamBlueprintSlotType.FIXED,
                questions.get(i).getId(),
                null,
                now,
                now,
                currentUserId,
                currentUserId
            ));
            slots.add(slot);
        }
        return slots;
    }

    private Exam createExam(
            ExamBlueprint blueprint,
            ExamBlueprintVersion version,
            SchoolClass schoolClass,
            CreateClassTestCommand command,
            UUID currentUserId,
            OffsetDateTime now) {
        var code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        var openAt = parseDateTime(command.openAt());
        var closeAt = parseDateTime(command.closeAt());
        var status = openAt != null ? ExamStatus.SCHEDULED : ExamStatus.IN_PROGRESS;
        return examRepository.save(new Exam(
            blueprint.getId(),
            version.getId(),
            code,
            command.name(),
            command.description(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            ExamKind.CLASS_TEST,
            status,
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
            examPaperItemRepository.save(new ExamPaperItem(
                slot.getId(),
                paperSection.getId(),
                paperSection.getPaperId(),
                slot.getFixedQuestionId(),
                slot.getOrder(),
                slot.getWeight()
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                slot.getFixedQuestionId(),
                examId,
                ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE,
                currentUserId
            );
        }
    }

    private void validateReusableSlots(List<ExamBlueprintSlot> slots) {
        for (var slot : slots) {
            if (slot.getSlotType() == ExamBlueprintSlotType.SELECTION) {
                throw new IllegalStateException("Blueprint co slot SELECTION, khong dung truc tiep cho class test");
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED phai co fixedQuestionId");
            }
            var question = questionRepository.findById(slot.getFixedQuestionId())
                .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi co dinh trong blueprint"));
            validateQuestionUnlocked(question);
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

    private void validateQuestionUnlocked(Question question) {
        if (question.isLocked()) {
            throw new IllegalStateException("Cau hoi " + question.getCode() + " dang bi khoa boi ky thi khac");
        }
    }

    private int assignCandidates(Exam exam, ExamPaper paper, UUID schoolClassId, UUID currentUserId, OffsetDateTime now) {
        var roster = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 0, MAX_CLASS_ROSTER_SIZE).content();
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
            throw new IllegalStateException("Thoi gian mo bai phai nho hon thoi gian dong bai");
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }
}
