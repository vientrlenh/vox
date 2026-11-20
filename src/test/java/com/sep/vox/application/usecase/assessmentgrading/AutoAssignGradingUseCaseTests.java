package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

public class AutoAssignGradingUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AutoAssignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID anh = UUID.randomUUID();
    private final UUID binh = UUID.randomUUID();
    private final UUID chi = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        // The selector and the balancer are pure services, so the REAL ones are used: the
        // load-balancing rule is exactly what the tests below check, and mocking them would leave
        // nothing to check.
        useCase = new AutoAssignGradingUseCase(
            assessmentGradingAssignmentRepository, assessmentCandidateResultRepository, assessmentGradingQueryRepository,
            assessmentGradingAccessService, new GradingSampleSelector(new java.util.Random(42)),
            new RoundRobinLoadBalancer(),
            mock(GradingAssignmentNotificationService.class));

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(anh, binh, chi));
        when(assessmentGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AssessmentGradingAssignment> assignments = (List<AssessmentGradingAssignment>) invocation.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    private void givenUnassignedResults(int count) {
        List<UUID> ids = new ArrayList<UUID>();
        for (int index = 0; index < count; index++) {
            ids.add(UUID.randomUUID());
        }
        when(assessmentGradingQueryRepository.findAssignableResultIds(eq(schoolId), eq(assessmentId), eq(null), anyCollection()))
            .thenReturn(ids);
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of());
    }

    private AutoAssignGradingCommand command(UUID... teacherIds) {
        return new AutoAssignGradingCommand(assessmentId, null, GradingRoundType.INITIAL.name(),
            GradingSampleSelectionMode.ALL.name(), null, null, null, List.of(teacherIds));
    }

    @SuppressWarnings("unchecked")
    private List<AssessmentGradingAssignment> captureSaved() {
        ArgumentCaptor<List<AssessmentGradingAssignment>> captor = (ArgumentCaptor<List<AssessmentGradingAssignment>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(assessmentGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Map<UUID, Long> countByTeacher(List<AssessmentGradingAssignment> assignments) {
        Map<UUID, Long> counts = new java.util.HashMap<UUID, Long>();
        assignments.forEach(assignment ->
            counts.merge(
                assignment.getTeacherId(), 
                1L, 
                (existingCount, increment) -> existingCount == null ? increment : existingCount + increment));
        return counts;
    }

    @Test
    void should_split_evenly_when_the_count_divides() {
        givenUnassignedResults(6);

        useCase.execute(command(anh, binh, chi));

        Map<UUID, Long> counts = countByTeacher(captureSaved());
        assertThat(counts).containsOnlyKeys(anh, binh, chi);
        assertThat(counts.values()).containsExactly(2L, 2L, 2L);
    }

    @Test
    void should_give_the_remainder_to_the_first_teachers_in_order() {
        givenUnassignedResults(7);

        useCase.execute(command(anh, binh, chi));

        // 7 submissions / 3 people: ties go to whoever comes first, so the remainder falls to Anh.
        Map<UUID, Long> counts = countByTeacher(captureSaved());
        assertThat(counts.get(anh)).isEqualTo(3L);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(2L);
    }

    @Test
    void should_start_from_current_load_instead_of_zero() {
        givenUnassignedResults(3);
        // Anh is holding 5 submissions, Binh 0, Chi 0 -> Anh should not receive more in this round.
        when(assessmentGradingQueryRepository.assignedLoadByTeacherIds(anyCollection()))
            .thenReturn(Map.of(anh, 5L, binh, 0L, chi, 0L));

        useCase.execute(command(anh, binh, chi));

        Map<UUID, Long> counts = countByTeacher(captureSaved());
        assertThat(counts).doesNotContainKey(anh);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(1L);
    }

    @Test
    void should_mark_every_created_assignment_as_assigned_by_the_admin() {
        givenUnassignedResults(2);

        useCase.execute(command(anh, binh));

        assertThat(captureSaved()).allSatisfy(assignment -> {
            assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.ASSIGNED);
            assertThat(assignment.getAssignedBy()).isEqualTo(adminId);
        });
    }

    @Test
    void should_do_nothing_when_every_result_is_already_assigned() {
        givenUnassignedResults(0);

        assertThat(useCase.execute(command(anh, binh))).isEmpty();
        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_when_neither_assessment_nor_schedule_is_given() {
        assertThatThrownBy(() ->
            useCase.execute(new AutoAssignGradingCommand(null, null, GradingRoundType.INITIAL.name(),
                GradingSampleSelectionMode.ALL.name(), null, null, null, List.of(anh))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_an_empty_teacher_group() {
        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất một giáo viên");
    }

    @Test
    void should_reject_duplicate_teachers() {
        assertThatThrownBy(() -> useCase.execute(command(anh, anh)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("trùng giáo viên");
    }

    @Test
    void should_reject_a_grader_who_is_not_a_teacher_of_the_same_school() {
        // Query lọc chỉ trả anh — binh không thuộc trường nên bị loại.
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(anh));

        assertThatThrownBy(() -> useCase.execute(command(anh, binh)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_only_take_a_percentage_of_the_pool_when_sampling_randomly() {
        givenUnassignedResults(10);

        useCase.execute(new AutoAssignGradingCommand(assessmentId, null, GradingRoundType.SPOT_CHECK.name(),
            GradingSampleSelectionMode.RANDOM_PERCENT.name(), 30, null, null, List.of(anh, binh)));

        // Hậu kiểm không ai soi 100% bài; 30% của 10 là 3.
        assertThat(captureSaved()).hasSize(3);
    }

    @Test
    void should_reject_a_random_sample_without_a_percentage() {
        givenUnassignedResults(10);

        assertThatThrownBy(() -> useCase.execute(new AutoAssignGradingCommand(
            assessmentId, null, GradingRoundType.SPOT_CHECK.name(),
            GradingSampleSelectionMode.RANDOM_PERCENT.name(), null, null, null, List.of(anh))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1 - 100");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_stamp_the_round_type_on_every_assignment() {
        givenUnassignedResults(2);

        useCase.execute(new AutoAssignGradingCommand(assessmentId, null, GradingRoundType.SPOT_CHECK.name(),
            null, null, null, null, List.of(anh)));

        assertThat(captureSaved()).allSatisfy(assignment ->
            assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.SPOT_CHECK));
    }
}
