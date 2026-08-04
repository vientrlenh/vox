package com.sep.vox.application.usecase.exampaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exampaper.CreateExamPaperUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * Giáo viên soạn nhiều mã đề cho một bài kiểm tra trên lớp ngay ở trang chi tiết — cách "soạn câu
 * hỏi trực tiếp" ({@code source = questions}) trước đây chỉ tồn tại trong lúc tạo bài.
 */
class CreateClassTestPaperTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamPaperSectionRepository examPaperSectionRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamQuestionSecureLockService examQuestionSecureLockService;
    private ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private CreateExamPaperUseCase useCase;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperSectionRepository = mock(ExamPaperSectionRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examQuestionSecureLockService = mock(ExamQuestionSecureLockService.class);
        examBlueprintVersionRepository = mock(ExamBlueprintVersionRepository.class);
        examBlueprintSectionRepository = mock(ExamBlueprintSectionRepository.class);
        examBlueprintSlotRepository = mock(ExamBlueprintSlotRepository.class);
        var questionRepository = mock(QuestionRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new CreateExamPaperUseCase(
            examRepository,
            examMemberRepository,
            examBlueprintVersionRepository,
            examBlueprintSectionRepository,
            examBlueprintSlotRepository,
            examPaperRepository,
            examPaperSectionRepository,
            examPaperItemRepository,
            questionRepository,
            mock(QuestionCollaboratorRepository.class),
            examQuestionSecureLockService,
            mock(ExamTimeQuotaGuardService.class),
            mock(RecalculateExamTimeDurationService.class),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTest(null)));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, TEACHER_ID, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(questionRepository.findAccessibleById(any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question()));
        when(examPaperRepository.nextVariant(EXAM_ID)).thenReturn(1);
        when(examPaperRepository.save(any(ExamPaper.class))).thenAnswer(inv -> {
            ExamPaper paper = inv.getArgument(0);
            paper.setId(UUID.randomUUID());
            return paper;
        });
        when(examPaperSectionRepository.save(any(ExamPaperSection.class))).thenAnswer(inv -> {
            ExamPaperSection section = inv.getArgument(0);
            section.setId(UUID.randomUUID());
            return section;
        });
        when(examPaperItemRepository.save(any(ExamPaperItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examPaperRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void should_create_a_paper_from_directly_authored_questions() {
        useCase.execute(questionsCommand());

        var captor = ArgumentCaptor.forClass(ExamPaperItem.class);
        verify(examPaperItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(captor.getValue().getBlueprintSlotId()).isNull();
    }

    /** Mã đề thứ hai phải lấy số hiệu kế tiếp, không đè "-P1" như luồng tạo bài cũ. */
    @Test
    void should_number_the_next_paper_variant() {
        when(examPaperRepository.nextVariant(EXAM_ID)).thenReturn(2);

        useCase.execute(questionsCommand());

        var captor = ArgumentCaptor.forClass(ExamPaper.class);
        verify(examPaperRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).endsWith("-P2");
    }

    /** Câu hỏi của bài trên lớp tự mở khoá khi đóng bài, không chờ người quản lý mở tay. */
    @Test
    void should_lock_questions_with_auto_release_after_close() {
        useCase.execute(questionsCommand());

        verify(examQuestionSecureLockService).lockQuestionForExam(
            eq(QUESTION_ID), eq(EXAM_ID), eq(ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE), eq(TEACHER_ID));
    }

    @Test
    void should_reject_direct_questions_for_centralized_exam() {
        var centralized = new Exam();
        centralized.setId(EXAM_ID);
        centralized.setKind(ExamKind.CENTRALIZED);
        centralized.setSchoolId(SCHOOL_ID);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(centralized));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, TEACHER_ID, ExamMemberRole.AUTHOR))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ bài kiểm tra trên lớp mới được soạn câu hỏi trực tiếp");
    }

    /** Bài đã gắn blueprint dùng chung thì mọi mã đề phải khớp blueprint. */
    @Test
    void should_reject_direct_questions_when_blueprint_is_attached() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTest(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không thể soạn câu hỏi trực tiếp");
    }

    @Test
    void should_reject_the_same_question_twice_in_one_paper() {
        var command = new CreateExamPaperCommand(EXAM_ID, "questions", null, List.of(
            section("Phần 1", QUESTION_ID),
            section("Phần 2", QUESTION_ID)
        ));

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không thể xuất hiện nhiều lần");
    }

    @Test
    void should_reject_when_caller_is_not_the_class_test_chair() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, TEACHER_ID, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(questionsCommand()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    @Test
    void should_reject_empty_sections() {
        var command = new CreateExamPaperCommand(EXAM_ID, "questions", null, List.of());

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Phải có ít nhất một phần trong đề");
    }

    /**
     * Hợp đồng có chủ đích cho ô "chọn ngẫu nhiên": sinh ra item RỖNG nhưng vẫn gắn
     * {@code blueprintSlotId}, để người ra đề có chỗ gán câu và UpdateExamPaperItemUseCase nhận ra
     * đây là ô SELECTION (được sửa) chứ không phải FIXED (bị khoá).
     */
    @Test
    void should_materialize_selection_slots_as_empty_items_bound_to_the_blueprint_slot() {
        var versionId = UUID.randomUUID();
        var sectionId = UUID.randomUUID();
        var slotId = UUID.randomUUID();
        var exam = classTest(UUID.randomUUID());
        exam.setBlueprintVersionId(versionId);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));

        var version = new ExamBlueprintVersion();
        version.setId(versionId);
        version.setCode("BP-V1");
        version.setTotalTimeLimitSeconds(0);
        when(examBlueprintVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        var section = new ExamBlueprintSection();
        section.setId(sectionId);
        section.setBlueprintVersionId(versionId);
        section.setOrder(1);
        section.setTitle("Phần 1");
        section.setSectionWeight(BigDecimal.ONE);
        when(examBlueprintSectionRepository.findByBlueprintVersionId(versionId)).thenReturn(List.of(section));

        var slot = new ExamBlueprintSlot();
        slot.setId(slotId);
        slot.setSectionId(sectionId);
        slot.setBlueprintVersionId(versionId);
        slot.setOrder(1);
        slot.setWeight(BigDecimal.ONE);
        slot.setSlotType(ExamBlueprintSlotType.SELECTION);
        when(examBlueprintSlotRepository.findByBlueprintVersionId(versionId)).thenReturn(List.of(slot));

        useCase.execute(new CreateExamPaperCommand(EXAM_ID, "blueprint", null, null));

        var captor = ArgumentCaptor.forClass(ExamPaperItem.class);
        verify(examPaperItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isNull();
        assertThat(captor.getValue().getBlueprintSlotId()).isEqualTo(slotId);
    }

    private CreateExamPaperCommand questionsCommand() {
        return new CreateExamPaperCommand(EXAM_ID, "questions", null, List.of(section("Phần 1", QUESTION_ID)));
    }

    private ClassTestSectionCommand section(String title, UUID questionId) {
        return new ClassTestSectionCommand(
            title,
            null,
            BigDecimal.ONE,
            List.of(new ClassTestQuestionCommand(questionId, BigDecimal.ONE))
        );
    }

    private Exam classTest(UUID blueprintId) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setSchoolId(SCHOOL_ID);
        exam.setCode("CT-ABC123");
        exam.setBlueprintId(blueprintId);
        return exam;
    }

    private Question question() {
        var question = new Question();
        question.setId(QUESTION_ID);
        question.setCreatedBy(TEACHER_ID);
        question.setSharing(QuestionSharing.SCHOOL_SHARED);
        question.setPreparationTimeSeconds(30);
        question.setMaxResponseSeconds(60);
        return question;
    }
}
