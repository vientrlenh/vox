package com.sep.vox.application.usecase.exampaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.sep.vox.application.port.input.command.UpdateExamPaperItemCommand;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperItemUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

/**
 * Ô SELECTION ("chọn ngẫu nhiên") trong blueprint chỉ mô tả tiêu chí, người ra đề mới chọn câu cụ
 * thể. Guard cũ chặn theo cả mã đề ({@code kind != CLASS_TEST || blueprintId != null}) nên mọi kỳ
 * thi tập trung đều không gán được câu vào ô SELECTION — mã đề vĩnh viễn còn ô trống, không
 * SUBMIT/LOCK được và cả luồng thi đứng. Guard giờ xét theo từng ô: chỉ FIXED mới bị khoá.
 */
class AssignSelectionSlotQuestionTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private QuestionRepository questionRepository;
    private ExamQuestionSecureLockService examQuestionSecureLockService;
    private ExamPaper paper;
    private UpdateExamPaperItemUseCase useCase;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examBlueprintSlotRepository = mock(ExamBlueprintSlotRepository.class);
        questionRepository = mock(QuestionRepository.class);
        examQuestionSecureLockService = mock(ExamQuestionSecureLockService.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new UpdateExamPaperItemUseCase(
            examRepository,
            examPaperRepository,
            examPaperItemRepository,
            examMemberRepository,
            examBlueprintSlotRepository,
            questionRepository,
            mock(QuestionCollaboratorRepository.class),
            schoolUserRepository,
            examQuestionSecureLockService,
            mock(ExamTimeQuotaGuardService.class),
            mock(RecalculateExamTimeDurationService.class),
            userContextPort
        );

        paper = paper(ExamPaperStatus.DRAFT);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(examPaperItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(SLOT_ID)));
        when(examPaperItemRepository.findByPaperId(PAPER_ID)).thenReturn(List.of());
        when(examPaperItemRepository.save(any(ExamPaperItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(centralizedExam()));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, TEACHER_ID, ExamMemberRole.AUTHOR))
            .thenReturn(true);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, TEACHER_ID, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examBlueprintSlotRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(slot(ExamBlueprintSlotType.SELECTION, null)));
        when(questionRepository.findAccessibleById(eq(QUESTION_ID), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question(QuestionStatus.PUBLISHED, QuestionType.READ_ALOUD, TOPIC_ID)));
        when(schoolUserRepository.findByUserId(TEACHER_ID)).thenReturn(Optional.empty());
    }

    /** Đây là ca hồi quy chính của bug: trước đây luôn ném "gắn với blueprint, không thể sửa". */
    @Test
    void should_assign_a_question_to_a_selection_slot_of_a_centralized_exam() {
        useCase.execute(command());

        var captor = ArgumentCaptor.forClass(ExamPaperItem.class);
        verify(examPaperItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isEqualTo(QUESTION_ID);
    }

    @Test
    void should_reject_changing_the_question_of_a_fixed_slot() {
        when(examBlueprintSlotRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(slot(ExamBlueprintSlotType.FIXED, null)));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cố định theo blueprint");
    }

    @Test
    void should_assign_a_question_to_a_selection_slot_of_a_class_test_bound_to_a_blueprint() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTestExam(UUID.randomUUID())));

        useCase.execute(command());

        verify(examPaperItemRepository).save(any(ExamPaperItem.class));
    }

    /**
     * Câu FIXED bị archived sau khi blueprint publish thì CreateExamPaperUseCase để item không gắn
     * slot, cốt để CHAIR tự chọn câu khác — item đó phải gán được và không cần hỏi tới slot repo.
     */
    @Test
    void should_assign_a_question_to_an_item_without_a_blueprint_slot() {
        when(examPaperItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(null)));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTestExam(UUID.randomUUID())));

        useCase.execute(command());

        verify(examPaperItemRepository).save(any(ExamPaperItem.class));
        verify(examBlueprintSlotRepository, never()).findById(any(UUID.class));
    }

    /** Slot bị xoá khi version còn DRAFT: fail-closed sẽ làm mã đề kẹt vĩnh viễn, nên cho gán. */
    @Test
    void should_assign_a_question_when_the_blueprint_slot_row_no_longer_exists() {
        when(examBlueprintSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        useCase.execute(command());

        verify(examPaperItemRepository).save(any(ExamPaperItem.class));
    }

    @Test
    void should_reject_a_non_published_question_for_a_blueprint_slot() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTestExam(UUID.randomUUID())));
        when(questionRepository.findAccessibleById(eq(QUESTION_ID), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question(QuestionStatus.DRAFT, QuestionType.READ_ALOUD, TOPIC_ID)));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Chỉ được gán câu hỏi đã PUBLISHED");
    }

    @Test
    void should_reject_a_question_whose_type_does_not_match_the_slot_criteria() {
        when(examBlueprintSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(
            slot(ExamBlueprintSlotType.SELECTION, new QuestionSelectionSpec(QuestionType.READ_ALOUD, null, null, null, null))));
        when(questionRepository.findAccessibleById(eq(QUESTION_ID), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question(QuestionStatus.PUBLISHED, QuestionType.OPINION, TOPIC_ID)));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không khớp tiêu chí");
    }

    @Test
    void should_reject_a_question_outside_the_topic_required_by_the_slot() {
        when(examBlueprintSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(
            slot(ExamBlueprintSlotType.SELECTION, new QuestionSelectionSpec(null, null, null, null, TOPIC_ID))));
        when(questionRepository.findAccessibleById(eq(QUESTION_ID), any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question(QuestionStatus.PUBLISHED, QuestionType.READ_ALOUD, UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không thuộc chủ đề");
    }

    @Test
    void should_accept_a_question_matching_every_supported_slot_criterion() {
        when(examBlueprintSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(
            slot(ExamBlueprintSlotType.SELECTION,
                new QuestionSelectionSpec(QuestionType.READ_ALOUD, null, null, null, TOPIC_ID))));

        useCase.execute(command());

        verify(examPaperItemRepository).save(any(ExamPaperItem.class));
    }

    @Test
    void should_lock_the_assigned_question_into_the_exam_secure_pool() {
        useCase.execute(command());

        verify(examQuestionSecureLockService).lockQuestionForExam(
            eq(QUESTION_ID), eq(EXAM_ID), eq(ExamSecurePoolReleaseMode.MANUAL), eq(TEACHER_ID));
    }

    /** Câu hỏi của bài trên lớp tự mở khoá khi đóng bài — trước đây chỗ này hardcode MANUAL. */
    @Test
    void should_lock_class_test_questions_with_auto_release_after_close() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTestExam(UUID.randomUUID())));

        useCase.execute(command());

        verify(examQuestionSecureLockService).lockQuestionForExam(
            eq(QUESTION_ID), eq(EXAM_ID), eq(ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE), eq(TEACHER_ID));
    }

    @Test
    void should_send_an_approved_paper_back_to_in_review_after_assignment() {
        paper.setStatus(ExamPaperStatus.APPROVED);

        useCase.execute(command());

        var captor = ArgumentCaptor.forClass(ExamPaper.class);
        verify(examPaperRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExamPaperStatus.IN_REVIEW);
    }

    private UpdateExamPaperItemCommand command() {
        return new UpdateExamPaperItemCommand(PAPER_ID, ITEM_ID, QUESTION_ID);
    }

    private ExamPaperItem item(UUID blueprintSlotId) {
        return new ExamPaperItem(ITEM_ID, blueprintSlotId, UUID.randomUUID(), PAPER_ID, null, 1, BigDecimal.ONE);
    }

    private ExamPaper paper(ExamPaperStatus status) {
        var examPaper = new ExamPaper();
        examPaper.setId(PAPER_ID);
        examPaper.setExamId(EXAM_ID);
        examPaper.setCode("EX-ABC123-P1");
        examPaper.setStatus(status);
        return examPaper;
    }

    private ExamBlueprintSlot slot(ExamBlueprintSlotType slotType, QuestionSelectionSpec spec) {
        var blueprintSlot = new ExamBlueprintSlot();
        blueprintSlot.setId(SLOT_ID);
        blueprintSlot.setSlotType(slotType);
        blueprintSlot.setSelectionSpec(spec);
        return blueprintSlot;
    }

    private Exam centralizedExam() {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(ExamKind.CENTRALIZED);
        exam.setSchoolId(SCHOOL_ID);
        exam.setCode("EX-ABC123");
        exam.setStatus(ExamStatus.DRAFT);
        exam.setBlueprintId(UUID.randomUUID());
        return exam;
    }

    private Exam classTestExam(UUID blueprintId) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setSchoolId(SCHOOL_ID);
        exam.setCode("CT-ABC123");
        exam.setStatus(ExamStatus.DRAFT);
        exam.setBlueprintId(blueprintId);
        return exam;
    }

    private Question question(QuestionStatus status, QuestionType type, UUID topicId) {
        var question = new Question();
        question.setId(QUESTION_ID);
        question.setCode("Q-001");
        question.setCreatedBy(TEACHER_ID);
        question.setSharing(QuestionSharing.SCHOOL_SHARED);
        question.setStatus(status);
        question.setType(type);
        question.setQuestionTopicId(topicId);
        question.setPreparationTimeSeconds(30);
        question.setMaxResponseSeconds(60);
        question.setCreatedAt(Instant.now());
        return question;
    }
}
