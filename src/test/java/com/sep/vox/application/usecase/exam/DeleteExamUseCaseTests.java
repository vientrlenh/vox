package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.DeleteExamCommand;
import com.sep.vox.application.port.input.usecase.exam.DeleteExamUseCase;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá kỳ thi phải dọn hết dữ liệu con: không có FK/cascade nào ở DB làm hộ, nên bỏ sót một bảng
 * là để lại ca thi/thí sinh mồ côi trỏ vào kỳ thi đã biến mất — đúng lỗi khiến trang lịch thi của
 * học sinh hiện một ca thi không còn kỳ thi nào đứng sau.
 *
 * <p>Chỉ kỳ thi còn DRAFT mới được xoá cứng; từ lúc đã lên lịch trở đi lịch đã công bố cho học
 * sinh nên chỉ được chuyển CANCELLED.
 */
class DeleteExamUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamPaperSectionRepository examPaperSectionRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamQuestionSecureLockService examQuestionSecureLockService;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteExamUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID deletedScheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperSectionRepository = mock(ExamPaperSectionRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examQuestionSecureLockService = mock(ExamQuestionSecureLockService.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new DeleteExamUseCase(
            examRepository,
            examScheduleRepository,
            examScheduleProctorRepository,
            examCandidateRepository,
            examPaperRepository,
            examPaperSectionRepository,
            examPaperItemRepository,
            examMemberRepository,
            examQuestionSecureLockService,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort);

        // Giáo viên chủ bài kiểm tra trên lớp — nhánh cho phép xoá.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.DRAFT)));
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(paper()));
        when(examScheduleRepository.findAllIdsByExamId(examId)).thenReturn(List.of(scheduleId));
    }

    @Test
    void should_hard_delete_draft_exam_with_all_its_children() {
        var response = useCase.execute(new DeleteExamCommand(examId));

        assertThat(response.deleted()).isTrue();
        assertThat(response.cancelledInstead()).isFalse();
        verify(examPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        verify(examPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        verify(examPaperRepository).deleteByExamId(examId);
        verify(examScheduleProctorRepository).deleteByScheduleIdIn(List.of(scheduleId));
        verify(examCandidateRepository).deleteByExamId(examId);
        verify(examScheduleRepository).deleteByExamId(examId);
        verify(examMemberRepository).deleteByExamId(examId);
        verify(examRepository).deleteById(examId);
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_delete_children_before_parent() {
        useCase.execute(new DeleteExamCommand(examId));

        // Con trước cha: không có FK nào chặn, sai thứ tự là để lại dòng mồ côi.
        var order = inOrder(
            examPaperItemRepository, examPaperSectionRepository, examPaperRepository,
            examScheduleProctorRepository, examCandidateRepository, examScheduleRepository,
            examMemberRepository, examRepository);
        order.verify(examPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(examPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(examPaperRepository).deleteByExamId(examId);
        order.verify(examScheduleProctorRepository).deleteByScheduleIdIn(List.of(scheduleId));
        order.verify(examCandidateRepository).deleteByExamId(examId);
        order.verify(examScheduleRepository).deleteByExamId(examId);
        order.verify(examMemberRepository).deleteByExamId(examId);
        order.verify(examRepository).deleteById(examId);
    }

    @Test
    void should_unlock_secure_pool_questions_before_deleting_draft_exam() {
        useCase.execute(new DeleteExamCommand(examId));

        // Câu hỏi bị khoá trỏ vào exam_secure_pools; xoá pool mà không mở khoá thì câu hỏi
        // nằm lại ngân hàng đề ở trạng thái khoá vĩnh viễn, không cách nào dùng lại.
        var order = inOrder(examQuestionSecureLockService, examRepository);
        order.verify(examQuestionSecureLockService).releaseAllForExam(examId, userId);
        order.verify(examRepository).deleteById(examId);
    }

    @Test
    void should_include_soft_deleted_schedules_when_cleaning_up_proctors() {
        // findAllIdsByExamId KHÔNG lọc DELETED: giám thị của ca đã xoá mềm cũng phải được dọn.
        when(examScheduleRepository.findAllIdsByExamId(examId))
            .thenReturn(List.of(scheduleId, deletedScheduleId));

        useCase.execute(new DeleteExamCommand(examId));

        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.captor();
        verify(examScheduleProctorRepository).deleteByScheduleIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(scheduleId, deletedScheduleId);
    }

    @Test
    void should_skip_paper_children_cleanup_when_exam_has_no_paper() {
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of());

        useCase.execute(new DeleteExamCommand(examId));

        verify(examPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(examPaperSectionRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(examRepository).deleteById(examId);
    }

    @Test
    void should_cancel_instead_of_delete_when_exam_is_scheduled() {
        assertCancelledInsteadOfDeleted(ExamStatus.SCHEDULED);
    }

    @Test
    void should_cancel_instead_of_delete_when_exam_is_in_progress() {
        assertCancelledInsteadOfDeleted(ExamStatus.IN_PROGRESS);
    }

    @Test
    void should_cancel_instead_of_delete_when_exam_is_closed() {
        assertCancelledInsteadOfDeleted(ExamStatus.CLOSED);
    }

    @Test
    void should_reject_delete_when_results_published() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.RESULTS_PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCommand(examId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("công bố kết quả");
        verify(examRepository, never()).deleteById(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void should_do_nothing_when_exam_already_cancelled() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.CANCELLED)));

        var response = useCase.execute(new DeleteExamCommand(examId));

        assertThat(response.deleted()).isFalse();
        assertThat(response.cancelledInstead()).isTrue();
        verify(examRepository, never()).deleteById(any());
        verify(examRepository, never()).save(any());
        verifyNoInteractions(examScheduleRepository, examCandidateRepository, examQuestionSecureLockService);
    }

    @Test
    void should_reject_when_user_is_neither_school_admin_nor_chair() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new DeleteExamCommand(examId)))
            .isInstanceOf(com.sep.vox.application.exception.ForbiddenException.class);
        verify(examRepository, never()).deleteById(any());
    }

    private void assertCancelledInsteadOfDeleted(ExamStatus status) {
        var exam = exam(status);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(new DeleteExamCommand(examId));

        assertThat(response.deleted()).isFalse();
        assertThat(response.cancelledInstead()).isTrue();
        assertThat(exam.getStatus()).isEqualTo(ExamStatus.CANCELLED);
        verify(examRepository).save(exam);
        verify(examRepository, never()).deleteById(any());
        verifyNoInteractions(examScheduleRepository, examCandidateRepository, examPaperRepository,
            examQuestionSecureLockService);
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(status);
        return exam;
    }

    private ExamPaper paper() {
        var paper = new ExamPaper();
        paper.setId(paperId);
        paper.setExamId(examId);
        return paper;
    }
}
