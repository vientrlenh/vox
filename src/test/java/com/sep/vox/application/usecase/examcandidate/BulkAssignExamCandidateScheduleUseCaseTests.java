package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkAssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.examcandidate.BulkAssignExamCandidateScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class BulkAssignExamCandidateScheduleUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private BulkAssignExamCandidateScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID firstCandidateId = UUID.randomUUID();
    private final UUID secondCandidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new BulkAssignExamCandidateScheduleUseCase(
            examRepository, examCandidateRepository, examScheduleRepository,
            new ExamPaperAutoAssigner(mock(ExamPaperRepository.class), examCandidateRepository),
            new ExamScheduleManageAccessService(
                examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.DRAFT)));
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });
    }

    @Test
    void should_assign_multiple_candidates_to_schedule() {
        givenSchedule(ExamScheduleStatus.PUBLISHED);
        givenCandidates(candidate(firstCandidateId, null), candidate(secondCandidateId, null));

        var result = useCase.execute(command(scheduleId));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isEqualTo(scheduleId));
        // Ca chỉ bị khoá đúng một lần dù có bao nhiêu thí sinh.
        verify(examScheduleRepository).findByIdForUpdate(scheduleId);
    }

    @Test
    void should_unassign_multiple_candidates_when_schedule_id_is_null() {
        givenCandidates(candidate(firstCandidateId, scheduleId), candidate(secondCandidateId, scheduleId));

        var result = useCase.execute(command(null));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.scheduleId()).isNull());
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
    }

    /** Tra theo (id, examId) nên thiếu dòng = có id lạ; hỏng cả lượt thay vì xếp một phần. */
    @Test
    void should_reject_when_any_candidate_belongs_to_another_exam() {
        givenSchedule(ExamScheduleStatus.DRAFT);
        givenCandidates(candidate(firstCandidateId, null));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(NotFoundException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_schedule_is_not_draft_or_published() {
        givenSchedule(ExamScheduleStatus.COMPLETED);

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_reject_when_exam_already_started() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam(ExamStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> useCase.execute(command(scheduleId)))
            .isInstanceOf(IllegalStateException.class);
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    @Test
    void should_do_nothing_when_candidate_list_is_empty() {
        var result = useCase.execute(new BulkAssignExamCandidateScheduleCommand(examId, List.of(), scheduleId));

        assertThat(result).isEmpty();
        verify(examScheduleRepository, never()).findByIdForUpdate(any());
        verify(examCandidateRepository, never()).saveAll(anyCollection());
    }

    private BulkAssignExamCandidateScheduleCommand command(UUID targetScheduleId) {
        return new BulkAssignExamCandidateScheduleCommand(
            examId, List.of(firstCandidateId, secondCandidateId), targetScheduleId);
    }

    private void givenSchedule(ExamScheduleStatus status) {
        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setStatus(status);
        when(examScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
    }

    private void givenCandidates(ExamCandidate... candidates) {
        when(examCandidateRepository.findByIdInAndExamId(
            List.of(firstCandidateId, secondCandidateId), examId)).thenReturn(List.of(candidates));
    }

    private ExamCandidate candidate(UUID id, UUID currentScheduleId) {
        var candidate = new ExamCandidate();
        candidate.setId(id);
        candidate.setExamId(examId);
        candidate.setStudentId(UUID.randomUUID());
        candidate.setScheduleId(currentScheduleId);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }

    private Exam exam(ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(status);
        return exam;
    }
}
