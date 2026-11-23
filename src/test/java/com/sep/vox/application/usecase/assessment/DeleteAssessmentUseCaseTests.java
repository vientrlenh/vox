package com.sep.vox.application.usecase.assessment;

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
import org.mockito.InOrder;

import com.sep.vox.application.port.input.command.assessment.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá kỳ thi phải dọn hết dữ liệu con: không có FK/cascade nào ở DB làm hộ, nên bỏ sót một bảng
 * là để lại ca thi/thí sinh mồ côi trỏ vào kỳ thi đã biến mất — đúng lỗi khiến trang lịch thi của
 * học sinh hiện một ca thi không còn kỳ thi nào đứng sau.
 *
 * <p>Chỉ kỳ thi còn DRAFT mới được xoá cứng; từ lúc đã lên lịch trở đi lịch đã công bố cho học
 * sinh nên chỉ được chuyển CANCELLED.
 */
class DeleteAssessmentUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private DeleteAssessmentUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID paperId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID deletedScheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperSectionRepository = mock(AssessmentPaperSectionRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentQuestionSecureLockService = mock(AssessmentQuestionSecureLockService.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new DeleteAssessmentUseCase(
            assessmentRepository,
            assessmentScheduleRepository,
            assessmentScheduleProctorRepository,
            assessmentCandidateRepository,
            assessmentPaperRepository,
            assessmentPaperSectionRepository,
            assessmentPaperItemRepository,
            assessmentMemberRepository,
            assessmentQuestionSecureLockService,
            // The real service (not mocked) on the same fake repo: the session-cancelling cascade is still checked here,
            // the full status mapping table lives in AssessmentScheduleClosureServiceTests.
            new com.sep.vox.application.port.input.service.AssessmentScheduleClosureService(
                assessmentScheduleRepository, mock(com.sep.vox.domain.repository.AssessmentSessionRepository.class)),
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort);

        // Giáo viên chủ bài kiểm tra trên lớp — nhánh cho phép xoá.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.DRAFT)));
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(paper()));
        when(assessmentScheduleRepository.findAllIdsByAssessmentId(assessmentId)).thenReturn(List.of(scheduleId));
    }

    @Test
    void should_hard_delete_draft_assessment_with_all_its_children() {
        DeleteAssessmentResponse response = useCase.execute(new DeleteAssessmentCommand(assessmentId));

        assertThat(response.deleted()).isTrue();
        assertThat(response.cancelledInstead()).isFalse();
        verify(assessmentPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        verify(assessmentPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        verify(assessmentPaperRepository).deleteByAssessmentId(assessmentId);
        verify(assessmentScheduleProctorRepository).deleteByScheduleIdIn(List.of(scheduleId));
        verify(assessmentCandidateRepository).deleteByAssessmentId(assessmentId);
        verify(assessmentScheduleRepository).deleteByAssessmentId(assessmentId);
        verify(assessmentMemberRepository).deleteByAssessmentId(assessmentId);
        verify(assessmentRepository).deleteById(assessmentId);
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_delete_children_before_parent() {
        useCase.execute(new DeleteAssessmentCommand(assessmentId));

        // Children before parents: no FK blocks it, and the wrong order leaves orphan rows behind.
        InOrder order = inOrder(
            assessmentPaperItemRepository, assessmentPaperSectionRepository, assessmentPaperRepository,
            assessmentScheduleProctorRepository, assessmentCandidateRepository, assessmentScheduleRepository,
            assessmentMemberRepository, assessmentRepository);
        order.verify(assessmentPaperItemRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(assessmentPaperSectionRepository).deleteByPaperIdIn(List.of(paperId));
        order.verify(assessmentPaperRepository).deleteByAssessmentId(assessmentId);
        order.verify(assessmentScheduleProctorRepository).deleteByScheduleIdIn(List.of(scheduleId));
        order.verify(assessmentCandidateRepository).deleteByAssessmentId(assessmentId);
        order.verify(assessmentScheduleRepository).deleteByAssessmentId(assessmentId);
        order.verify(assessmentMemberRepository).deleteByAssessmentId(assessmentId);
        order.verify(assessmentRepository).deleteById(assessmentId);
    }

    @Test
    void should_unlock_secure_pool_questions_before_deleting_draft_assessment() {
        useCase.execute(new DeleteAssessmentCommand(assessmentId));

        // Locked questions point into assessment_secure_pools; if the pool is deleted without unlocking,
        // the questions stay in the question bank locked forever and can never be reused.
        InOrder order = inOrder(assessmentQuestionSecureLockService, assessmentRepository);
        order.verify(assessmentQuestionSecureLockService).releaseAllForAssessment(assessmentId, userId);
        order.verify(assessmentRepository).deleteById(assessmentId);
    }

    @Test
    void should_include_soft_deleted_schedules_when_cleaning_up_proctors() {
        // findAllIdsByAssessmentId does NOT filter DELETED: the proctors of a soft-deleted session must be cleaned up too.
        when(assessmentScheduleRepository.findAllIdsByAssessmentId(assessmentId))
            .thenReturn(List.of(scheduleId, deletedScheduleId));

        useCase.execute(new DeleteAssessmentCommand(assessmentId));

        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.captor();
        verify(assessmentScheduleProctorRepository).deleteByScheduleIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(scheduleId, deletedScheduleId);
    }

    @Test
    void should_skip_paper_children_cleanup_when_assessment_has_no_paper() {
        when(assessmentPaperRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());

        useCase.execute(new DeleteAssessmentCommand(assessmentId));

        verify(assessmentPaperItemRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(assessmentPaperSectionRepository, never()).deleteByPaperIdIn(anyCollection());
        verify(assessmentRepository).deleteById(assessmentId);
    }

    @Test
    void should_cancel_instead_of_delete_when_assessment_is_scheduled() {
        assertCancelledInsteadOfDeleted(AssessmentStatus.SCHEDULED);
    }

    @Test
    void should_cancel_instead_of_delete_when_assessment_is_in_progress() {
        assertCancelledInsteadOfDeleted(AssessmentStatus.IN_PROGRESS);
    }

    @Test
    void should_cancel_instead_of_delete_when_assessment_is_closed() {
        assertCancelledInsteadOfDeleted(AssessmentStatus.CLOSED);
    }

    @Test
    void should_reject_delete_when_results_published() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.RESULTS_PUBLISHED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCommand(assessmentId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("công bố kết quả");
        verify(assessmentRepository, never()).deleteById(any());
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void should_do_nothing_when_assessment_already_cancelled() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment(AssessmentStatus.CANCELLED)));

        DeleteAssessmentResponse response = useCase.execute(new DeleteAssessmentCommand(assessmentId));

        assertThat(response.deleted()).isFalse();
        assertThat(response.cancelledInstead()).isTrue();
        verify(assessmentRepository, never()).deleteById(any());
        verify(assessmentRepository, never()).save(any());
        verifyNoInteractions(assessmentScheduleRepository, assessmentCandidateRepository, assessmentQuestionSecureLockService);
    }

    /**
     * A session that is COMPLETED/MOVED is in a final state: overwriting it with CANCELLED would
     * erase the trace of a session that was already held and skew the movedToScheduleId pointer of
     * the moved session.
     */
    @Test
    void should_not_cancel_completed_or_moved_schedules() {
        Assessment assessment = assessment(AssessmentStatus.SCHEDULED);
        AssessmentSchedule draft = schedule(AssessmentScheduleStatus.DRAFT);
        AssessmentSchedule completed = schedule(AssessmentScheduleStatus.COMPLETED);
        AssessmentSchedule moved = schedule(AssessmentScheduleStatus.MOVED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(draft, completed, moved));

        useCase.execute(new DeleteAssessmentCommand(assessmentId));

        assertThat(draft.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        assertThat(completed.getStatus()).isEqualTo(AssessmentScheduleStatus.COMPLETED);
        assertThat(moved.getStatus()).isEqualTo(AssessmentScheduleStatus.MOVED);
        verify(assessmentScheduleRepository).save(draft);
        verify(assessmentScheduleRepository, never()).save(completed);
        verify(assessmentScheduleRepository, never()).save(moved);
    }

    @Test
    void should_reject_when_user_is_neither_school_admin_nor_chair() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCommand(assessmentId)))
            .isInstanceOf(com.sep.vox.application.exception.ForbiddenException.class);
        verify(assessmentRepository, never()).deleteById(any());
    }

    /**
     * Regression guard for the permission boundary of the council chair: they run the whole
     * centralized-assessment workflow (edit information, change status, author papers, arrange
     * schedules), but deleting the assessment is still only for the school admin -- it is a destructive
     * action and is not part of that workflow.
     */
    @Test
    void should_reject_delete_of_a_centralized_assessment_by_its_chair() {
        Assessment assessment = assessment(AssessmentStatus.DRAFT);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> useCase.execute(new DeleteAssessmentCommand(assessmentId)))
            .isInstanceOf(com.sep.vox.application.exception.ForbiddenException.class);
        verify(assessmentRepository, never()).deleteById(any());
    }

    private void assertCancelledInsteadOfDeleted(AssessmentStatus status) {
        Assessment assessment = assessment(status);
        AssessmentSchedule schedule = schedule(AssessmentScheduleStatus.PUBLISHED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(schedule));

        DeleteAssessmentResponse response = useCase.execute(new DeleteAssessmentCommand(assessmentId));

        assertThat(response.deleted()).isFalse();
        assertThat(response.cancelledInstead()).isTrue();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.CANCELLED);
        // Cancelling an assessment but leaving its sessions PUBLISHED means the sessions still show in
        // the students' assessment calendar.
        assertThat(schedule.getStatus()).isEqualTo(AssessmentScheduleStatus.CANCELLED);
        verify(assessmentRepository).save(assessment);
        verify(assessmentRepository, never()).deleteById(any());
        verify(assessmentScheduleRepository, never()).deleteByAssessmentId(any());
        verifyNoInteractions(assessmentCandidateRepository, assessmentPaperRepository, assessmentQuestionSecureLockService);
    }

    private Assessment assessment(AssessmentStatus status) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(status);
        return assessment;
    }

    private AssessmentSchedule schedule(AssessmentScheduleStatus status) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(assessmentId);
        schedule.setStatus(status);
        return schedule;
    }

    private AssessmentPaper paper() {
        AssessmentPaper paper = new AssessmentPaper();
        paper.setId(paperId);
        paper.setAssessmentId(assessmentId);
        return paper;
    }
}
