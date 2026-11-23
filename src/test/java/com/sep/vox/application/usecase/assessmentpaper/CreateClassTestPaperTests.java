package com.sep.vox.application.usecase.assessmentpaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessment.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.assessment.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.assessmentpaper.CreateAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.assessmentpaper.CreateAssessmentPaperUseCase;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * Giáo viên soạn nhiều mã đề cho một bài kiểm tra trên lớp ngay ở trang chi tiết — cách "soạn câu
 * hỏi trực tiếp" ({@code source = questions}) trước đây chỉ tồn tại trong lúc tạo bài.
 */
class CreateClassTestPaperTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private CreateAssessmentPaperUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperSectionRepository = mock(AssessmentPaperSectionRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentQuestionSecureLockService = mock(AssessmentQuestionSecureLockService.class);
        assessmentBlueprintVersionRepository = mock(AssessmentBlueprintVersionRepository.class);
        assessmentBlueprintSectionRepository = mock(AssessmentBlueprintSectionRepository.class);
        assessmentBlueprintSlotRepository = mock(AssessmentBlueprintSlotRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new CreateAssessmentPaperUseCase(
            assessmentRepository,
            new AssessmentPaperAuthoringAccessService(
                assessmentMemberRepository, mock(SchoolUserRepository.class), mock(UserRoleQueryRepository.class)),
            assessmentBlueprintVersionRepository,
            assessmentBlueprintSectionRepository,
            assessmentBlueprintSlotRepository,
            assessmentPaperRepository,
            assessmentPaperSectionRepository,
            assessmentPaperItemRepository,
            questionRepository,
            mock(QuestionAssetRepository.class),
            mock(QuestionCollaboratorRepository.class),
            assessmentQuestionSecureLockService,
            mock(AssessmentTimeQuotaGuardService.class),
            mock(RecalculateAssessmentTimeDurationService.class),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(classTest(null)));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, TEACHER_ID)).thenReturn(Optional.of(
            AssessmentMember.builder()
                    .assessmentId(ASSESSMENT_ID)
                    .userId(TEACHER_ID)
                    .role(AssessmentMemberRole.CHAIR)
                    .grantedAt(Instant.now())
                    .grantedBy(TEACHER_ID)
                    .build()));
        when(questionRepository.findAccessibleById(any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question()));
        when(assessmentPaperRepository.nextVariant(ASSESSMENT_ID)).thenReturn(1);
        when(assessmentPaperRepository.save(any(AssessmentPaper.class))).thenAnswer(inv -> {
            AssessmentPaper paper = inv.getArgument(0);
            paper.setId(UUID.randomUUID());
            return paper;
        });
        when(assessmentPaperSectionRepository.save(any(AssessmentPaperSection.class))).thenAnswer(inv -> {
            AssessmentPaperSection section = inv.getArgument(0);
            section.setId(UUID.randomUUID());
            return section;
        });
        when(assessmentPaperItemRepository.save(any(AssessmentPaperItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assessmentPaperRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void should_create_a_paper_from_directly_authored_questions() {
        useCase.execute(questionsCommand());

        ArgumentCaptor<AssessmentPaperItem> captor = ArgumentCaptor.forClass(AssessmentPaperItem.class);
        verify(assessmentPaperItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(captor.getValue().getBlueprintSlotId()).isNull();
    }

    /**
     * The second paper variant must take the next number and must not overwrite "-P1" as the old
     * creation flow did.
     */
    @Test
    void should_number_the_next_paper_variant() {
        when(assessmentPaperRepository.nextVariant(ASSESSMENT_ID)).thenReturn(2);

        useCase.execute(questionsCommand());

        ArgumentCaptor<AssessmentPaper> captor = ArgumentCaptor.forClass(AssessmentPaper.class);
        verify(assessmentPaperRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).endsWith("-P2");
    }

    /** Câu hỏi của bài trên lớp tự mở khoá khi đóng bài, không chờ người quản lý mở tay. */
    @Test
    void should_lock_questions_with_auto_release_after_close() {
        useCase.execute(questionsCommand());

        verify(assessmentQuestionSecureLockService).lockQuestionForAssessment(
            eq(QUESTION_ID), eq(ASSESSMENT_ID), eq(AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE), eq(TEACHER_ID));
    }

    @Test
    void should_reject_direct_questions_for_centralized_assessment() {
        Assessment centralized = new Assessment();
        centralized.setId(ASSESSMENT_ID);
        centralized.setKind(AssessmentKind.CENTRALIZED);
        centralized.setSchoolId(SCHOOL_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(centralized));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, TEACHER_ID)).thenReturn(Optional.of(
            AssessmentMember.builder()
                    .assessmentId(ASSESSMENT_ID)
                    .userId(TEACHER_ID)
                    .role(AssessmentMemberRole.AUTHOR)
                    .grantedAt(Instant.now())
                    .grantedBy(TEACHER_ID)
                    .build()));

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ bài kiểm tra trên lớp mới được soạn câu hỏi trực tiếp");
    }

    /** Bài đã gắn blueprint dùng chung thì mọi mã đề phải khớp blueprint. */
    @Test
    void should_reject_direct_questions_when_blueprint_is_attached() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(classTest(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không thể soạn câu hỏi trực tiếp");
    }

    @Test
    void should_reject_the_same_question_twice_in_one_paper() {
        CreateAssessmentPaperCommand command = new CreateAssessmentPaperCommand(ASSESSMENT_ID, "questions", null, List.of(
            section("Phần 1", QUESTION_ID),
            section("Phần 2", QUESTION_ID)
        ));

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không thể xuất hiện nhiều lần");
    }

    @Test
    void should_reject_when_caller_is_not_the_class_test_chair() {
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, TEACHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    @Test
    void should_reject_empty_sections() {
        CreateAssessmentPaperCommand command = new CreateAssessmentPaperCommand(ASSESSMENT_ID, "questions", null, List.of());

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phải có ít nhất một phần trong đề");
    }

    /**
     * A deliberate contract for the "pick randomly" slot: it produces an EMPTY item but still
     * attaches {@code blueprintSlotId}, so the author has a place to assign a question and
     * UpdateAssessmentPaperItemUseCase recognizes it as a SELECTION slot (editable) rather than FIXED
     * (locked).
     */
    @Test
    void should_materialize_selection_slots_as_empty_items_bound_to_the_blueprint_slot() {
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Assessment assessment = classTest(UUID.randomUUID());
        assessment.setBlueprintVersionId(versionId);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        AssessmentBlueprintVersion version = new AssessmentBlueprintVersion();
        version.setId(versionId);
        version.setCode("BP-V1");
        version.setTotalTimeLimitSeconds(0);
        when(assessmentBlueprintVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        AssessmentBlueprintSection section = new AssessmentBlueprintSection();
        section.setId(sectionId);
        section.setBlueprintVersionId(versionId);
        section.setOrder(1);
        section.setTitle("Phần 1");
        section.setSectionWeight(BigDecimal.ONE);
        when(assessmentBlueprintSectionRepository.findByBlueprintVersionId(versionId)).thenReturn(List.of(section));

        AssessmentBlueprintSlot slot = new AssessmentBlueprintSlot();
        slot.setId(slotId);
        slot.setSectionId(sectionId);
        slot.setBlueprintVersionId(versionId);
        slot.setOrder(1);
        slot.setWeight(BigDecimal.ONE);
        slot.setSlotType(AssessmentBlueprintSlotType.SELECTION);
        when(assessmentBlueprintSlotRepository.findByBlueprintVersionId(versionId)).thenReturn(List.of(slot));

        useCase.execute(new CreateAssessmentPaperCommand(ASSESSMENT_ID, "blueprint", null, null));

        ArgumentCaptor<AssessmentPaperItem> captor = ArgumentCaptor.forClass(AssessmentPaperItem.class);
        verify(assessmentPaperItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isNull();
        assertThat(captor.getValue().getBlueprintSlotId()).isEqualTo(slotId);
    }

    private CreateAssessmentPaperCommand questionsCommand() {
        return new CreateAssessmentPaperCommand(ASSESSMENT_ID, "questions", null, List.of(section("Phần 1", QUESTION_ID)));
    }

    private ClassTestSectionCommand section(String title, UUID questionId) {
        return new ClassTestSectionCommand(
            title,
            null,
            BigDecimal.ONE,
            List.of(new ClassTestQuestionCommand(questionId, BigDecimal.ONE))
        );
    }

    private Assessment classTest(UUID blueprintId) {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setSchoolId(SCHOOL_ID);
        assessment.setCode("CT-ABC123");
        assessment.setBlueprintId(blueprintId);
        return assessment;
    }

    private Question question() {
        Question question = new Question();
        question.setId(QUESTION_ID);
        question.setCreatedBy(TEACHER_ID.toString());
        question.setSharing(QuestionSharing.SCHOOL_SHARED);
        question.setPreparationTimeSeconds(30);
        question.setMaxResponseSeconds(60);
        return question;
    }
}
