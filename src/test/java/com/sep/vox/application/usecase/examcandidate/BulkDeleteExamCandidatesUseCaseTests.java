package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkDeleteExamCandidatesCommand;
import com.sep.vox.application.port.input.usecase.examcandidate.BulkDeleteExamCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá hàng loạt thí sinh khỏi kỳ thi: all-or-nothing.
 *
 * <p>Xoá một phần rồi báo lỗi chung chung sẽ khiến người dùng không biết ai đã bị xoá và ai chưa,
 * mà đây là thao tác không hoàn tác được.
 */
public class BulkDeleteExamCandidatesUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private BulkDeleteExamCandidatesUseCase useCase;

    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID candidateA = UUID.randomUUID();
    private final UUID candidateB = UUID.randomUUID();

    private Exam exam;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new BulkDeleteExamCandidatesUseCase(
            examRepository, examCandidateRepository, examSessionRepository,
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setStatus(ExamStatus.SCHEDULED);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(schoolUserRepository.findByUserId(adminId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, adminId, null, null)));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), adminId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));

        when(examCandidateRepository.findByIdInAndExamId(anyCollection(), any()))
            .thenReturn(List.of(candidate(candidateA), candidate(candidateB)));
        when(examSessionRepository.findByCandidateIdIn(anyCollection())).thenReturn(List.of());
    }

    private ExamCandidate candidate(UUID id) {
        var candidate = new ExamCandidate();
        candidate.setId(id);
        candidate.setExamId(examId);
        candidate.setStudentId(UUID.randomUUID());
        return candidate;
    }

    private void delete(UUID... ids) {
        useCase.execute(new BulkDeleteExamCandidatesCommand(examId, List.of(ids)));
    }

    @Test
    void should_delete_the_whole_group_in_one_call() {
        delete(candidateA, candidateB);

        verify(examCandidateRepository).deleteByIdIn(List.of(candidateA, candidateB));
    }

    @Test
    void should_do_nothing_for_an_empty_list() {
        useCase.execute(new BulkDeleteExamCandidatesCommand(examId, List.of()));

        verify(examCandidateRepository, never()).deleteByIdIn(any());
    }

    /** Thiếu dòng = có id không tồn tại hoặc thuộc kỳ thi khác — hỏng cả lượt, không xoá một phần. */
    @Test
    void should_refuse_when_an_id_does_not_belong_to_the_exam() {
        when(examCandidateRepository.findByIdInAndExamId(anyCollection(), any()))
            .thenReturn(List.of(candidate(candidateA)));

        assertThatThrownBy(() -> delete(candidateA, candidateB)).isInstanceOf(NotFoundException.class);

        verify(examCandidateRepository, never()).deleteByIdIn(any());
    }

    /** Một người đã vào thi là cả lô bị từ chối — không xoá phần còn lại. */
    @Test
    void should_refuse_the_whole_batch_when_any_candidate_already_sat_the_exam() {
        var session = new ExamSession();
        session.setCandidateId(candidateB);
        when(examSessionRepository.findByCandidateIdIn(anyCollection())).thenReturn(List.of(session));

        assertThatThrownBy(() -> delete(candidateA, candidateB))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã có bài thi");

        verify(examCandidateRepository, never()).deleteByIdIn(any());
    }

    /** Kỳ thi đã bắt đầu thì mọi thao tác sửa danh sách bị khoá (ExamEditingGuard). */
    @Test
    void should_refuse_once_the_exam_has_started() {
        exam.setStatus(ExamStatus.IN_PROGRESS);

        assertThatThrownBy(() -> delete(candidateA)).isInstanceOf(IllegalStateException.class);

        verify(examCandidateRepository, never()).deleteByIdIn(any());
    }

    @Test
    void should_refuse_a_caller_who_is_neither_school_admin_nor_chair() {
        var outsider = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(outsider);
        when(schoolUserRepository.findByUserId(outsider))
            .thenReturn(Optional.of(new SchoolUser(UUID.randomUUID(), outsider, null, null)));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(outsider)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), outsider, UUID.randomUUID(), null, "TEACHER", "Teacher")));

        assertThatThrownBy(() -> delete(candidateA)).isInstanceOf(ForbiddenException.class);

        verify(examCandidateRepository, never()).deleteByIdIn(any());
    }
}
