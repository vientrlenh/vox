package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessmentgrading.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.application.response.input.assessmentgrading.ReclaimOverdueResponse;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * Thu hồi quá hạn là một trong ba đường đóng phân công KHÔNG đi qua
 * {@code GradingActionSupport.finish()}, nên nó phải tự nhả đơn phúc khảo (review BE-1)
 * — và chỉ nhả khi không có người thay (review BE-8 cho phần phạm vi quét).
 */
class ReclaimOverdueAssignmentsUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ReclaimOverdueAssignmentsUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    private AssessmentResultAppeal appeal;

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ReclaimOverdueAssignmentsUseCase(
            assessmentGradingAssignmentRepository,
            assessmentResultAppealRepository,
            assessmentGradingQueryRepository,
            assessmentGradingAccessService,
            new RoundRobinLoadBalancer());

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(assessmentGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.GRADING);
        when(assessmentResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
    }

    private AssessmentGradingAssignment overdue(GradingRoundType roundType, UUID linkedAppealId) {
        Instant now = Instant.now();
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, linkedAppealId, null,
            now.minus(3, ChronoUnit.DAYS), adminId, now.minus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        return assignment;
    }

    private void givenOverdue(AssessmentGradingAssignment... assignments) {
        when(assessmentGradingAssignmentRepository.findOverdueInSchool(any(), any(), any(), anyInt()))
            .thenReturn(List.of(assignments));
    }

    private ReclaimOverdueAssignmentsCommand reclaimAll(List<UUID> reassignTo) {
        return new ReclaimOverdueAssignmentsCommand(assessmentId, null, reassignTo, null);
    }

    @Test
    void should_release_the_appeal_back_to_approved_when_reclaiming_without_a_replacement() {
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));

        useCase.execute(reclaimAll(null));

        // Không nhả thì đơn đứng ở GRADING vĩnh viễn: giao lại đòi APPROVED, duyệt lại
        // đòi PENDING — không còn đường nào ngoài sửa tay dưới DB.
        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.APPROVED);
        verify(assessmentResultAppealRepository).save(appeal);
    }

    @Test
    void should_keep_the_appeal_in_grading_when_a_replacement_takes_over_immediately() {
        UUID replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(assessmentGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        useCase.execute(reclaimAll(List.of(replacement)));

        // The new row is opened right away with the same appealId in the same transaction, so
        // GRADING is still correct -- releasing and then pulling it back would only create a wrong
        // in-between state.
        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.GRADING);
        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_a_replacement_who_already_graded_the_paper_under_appeal() {
        UUID conflicted = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(conflicted)))
            .thenReturn(Set.of(conflicted));
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(conflicted));

        // The same COI rule that ReassignGradingUseCase already applies to changing the person row
        // by row: bulk reassignment must not be a back door around it.
        assertThatThrownBy(() -> useCase.execute(reclaimAll(List.of(conflicted))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã từng chấm");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_allow_a_clean_replacement_for_an_appeal_round() {
        UUID replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(UUID.randomUUID()));
        when(assessmentGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        assertThat(useCase.execute(reclaimAll(List.of(replacement))).reassignedAssignmentIds()).hasSize(1);
    }

    @Test
    void should_not_ask_about_conflicts_when_no_appeal_round_is_involved() {
        UUID replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.INITIAL, null));
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(assessmentGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        useCase.execute(reclaimAll(List.of(replacement)));

        // The other three rounds have no COI rule -- asking anyway would add a query to every
        // reclaim.
        verify(assessmentGradingQueryRepository, never()).findTeacherIdsWithHumanEvaluation(any());
    }

    @Test
    void should_leave_appeals_alone_for_the_other_three_rounds() {
        givenOverdue(overdue(GradingRoundType.INITIAL, null));

        useCase.execute(reclaimAll(null));

        verify(assessmentResultAppealRepository, never()).findById(any());
        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_still_close_the_reclaimed_assignment_as_declined() {
        AssessmentGradingAssignment assignment = overdue(GradingRoundType.APPEAL, appealId);
        givenOverdue(assignment);

        useCase.execute(reclaimAll(null));

        assertThat(assignment.isCompleted()).isTrue();
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_cap_one_batch_and_tell_the_admin_there_is_more_to_do() {
        AssessmentGradingAssignment[] tooMany = IntStream.range(0, 501)
            .mapToObj(ignored -> overdue(GradingRoundType.INITIAL, null))
            .toArray(AssessmentGradingAssignment[]::new);
        givenOverdue(tooMany);

        ReclaimOverdueResponse response = useCase.execute(reclaimAll(null));

        // A single click on "Reclaim all" must not take the whole school into one write
        // transaction; the hasMore flag is what tells the admin they need to click again instead of
        // assuming it is finished.
        assertThat(response.reclaimedAssignmentIds()).hasSize(500);
        assertThat(response.hasMore()).isTrue();
        // Fetching exactly ONE extra row is enough to know there is more -- no need for an
        // additional COUNT query.
        verify(assessmentGradingAssignmentRepository).findOverdueInSchool(any(), any(), any(), eq(501));
    }

    @Test
    void should_not_flag_more_work_when_the_batch_fits() {
        givenOverdue(overdue(GradingRoundType.INITIAL, null));

        assertThat(useCase.execute(reclaimAll(null)).hasMore()).isFalse();
    }

    @Test
    void should_scan_only_the_current_school_instead_of_the_whole_system() {
        givenOverdue(overdue(GradingRoundType.INITIAL, null));

        useCase.execute(reclaimAll(null));

        // Bản cũ gọi findOverdue toàn hệ thống rồi load() từng dòng để đọc schoolId —
        // 4N+1 query và chạm cả dữ liệu trường khác.
        verify(assessmentGradingAssignmentRepository).findOverdueInSchool(any(), any(), any(), anyInt());
        verify(assessmentGradingAssignmentRepository, never()).findOverdue(any());
        verify(assessmentGradingAccessService, never()).load(any());
    }
}
