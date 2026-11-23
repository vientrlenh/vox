package com.sep.vox.application.usecase.assessmentcandidate;

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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.BulkDeleteAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkDeleteAssessmentCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá hàng loạt thí sinh khỏi kỳ thi: all-or-nothing.
 *
 * <p>Xoá một phần rồi báo lỗi chung chung sẽ khiến người dùng không biết ai đã bị xoá và ai chưa,
 * mà đây là thao tác không hoàn tác được.
 */
public class BulkDeleteAssessmentCandidatesUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private BulkDeleteAssessmentCandidatesUseCase useCase;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID candidateA = UUID.randomUUID();
    private final UUID candidateB = UUID.randomUUID();

    private Assessment assessment;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new BulkDeleteAssessmentCandidatesUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentSessionRepository,
            assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);

        assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setStatus(AssessmentStatus.SCHEDULED);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(schoolUserRepository.findByUserId(adminId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(adminId)
                .startDate(null)
                .endDate(null)
                .build()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), adminId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));

        when(assessmentCandidateRepository.findByIdInAndAssessmentId(anyCollection(), any()))
            .thenReturn(List.of(candidate(candidateA), candidate(candidateB)));
        when(assessmentSessionRepository.findByCandidateIdIn(anyCollection())).thenReturn(List.of());
    }

    private AssessmentCandidate candidate(UUID id) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(id);
        candidate.setAssessmentId(assessmentId);
        candidate.setStudentId(UUID.randomUUID());
        return candidate;
    }

    private void delete(UUID... ids) {
        useCase.execute(new BulkDeleteAssessmentCandidatesCommand(assessmentId, List.of(ids)));
    }

    @Test
    void should_delete_the_whole_group_in_one_call() {
        delete(candidateA, candidateB);

        verify(assessmentCandidateRepository).deleteByIdIn(List.of(candidateA, candidateB));
    }

    @Test
    void should_do_nothing_for_an_empty_list() {
        useCase.execute(new BulkDeleteAssessmentCandidatesCommand(assessmentId, List.of()));

        verify(assessmentCandidateRepository, never()).deleteByIdIn(any());
    }

    /** Thiếu dòng = có id không tồn tại hoặc thuộc kỳ thi khác — hỏng cả lượt, không xoá một phần. */
    @Test
    void should_refuse_when_an_id_does_not_belong_to_the_assessment() {
        when(assessmentCandidateRepository.findByIdInAndAssessmentId(anyCollection(), any()))
            .thenReturn(List.of(candidate(candidateA)));

        assertThatThrownBy(() -> delete(candidateA, candidateB)).isInstanceOf(ResourceNotFoundException.class);

        verify(assessmentCandidateRepository, never()).deleteByIdIn(any());
    }

    /**
     * If one person has already entered the assessment, the whole batch is rejected -- the rest is not
     * deleted.
     */
    @Test
    void should_refuse_the_whole_batch_when_any_candidate_already_sat_the_assessment() {
        AssessmentSession session = new AssessmentSession();
        session.setCandidateId(candidateB);
        when(assessmentSessionRepository.findByCandidateIdIn(anyCollection())).thenReturn(List.of(session));

        assertThatThrownBy(() -> delete(candidateA, candidateB))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã có bài thi");

        verify(assessmentCandidateRepository, never()).deleteByIdIn(any());
    }

    /** Once an assessment has started, every edit of the list is locked (AssessmentEditingGuard). */
    @Test
    void should_refuse_once_the_assessment_has_started() {
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);

        assertThatThrownBy(() -> delete(candidateA)).isInstanceOf(IllegalArgumentException.class);

        verify(assessmentCandidateRepository, never()).deleteByIdIn(any());
    }

    @Test
    void should_refuse_a_caller_who_is_neither_school_admin_nor_chair() {
        UUID outsider = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(outsider);
        when(schoolUserRepository.findByUserId(outsider))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(UUID.randomUUID())
                .userId(outsider)
                .startDate(null)
                .endDate(null)
                .build()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(outsider)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), outsider, UUID.randomUUID(), null, "TEACHER", "Teacher")));

        assertThatThrownBy(() -> delete(candidateA)).isInstanceOf(ForbiddenException.class);

        verify(assessmentCandidateRepository, never()).deleteByIdIn(any());
    }
}
