package com.sep.vox.application.usecase.examgrading;

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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

public class AutoAssignGradingUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private AutoAssignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID anh = UUID.randomUUID();
    private final UUID binh = UUID.randomUUID();
    private final UUID chi = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new AutoAssignGradingUseCase(
            examGradingAssignmentRepository, examGradingQueryRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(examGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(anh, binh, chi));
        when(examGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var assignments = (List<ExamGradingAssignment>) invocation.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    private void givenUnassignedResults(int count) {
        var ids = new ArrayList<UUID>();
        for (var index = 0; index < count; index++) {
            ids.add(UUID.randomUUID());
        }
        when(examGradingQueryRepository.findUnassignedPendingReviewResultIds(schoolId, examId, null))
            .thenReturn(ids);
    }

    private AutoAssignGradingCommand command(UUID... teacherIds) {
        return new AutoAssignGradingCommand(examId, null, List.of(teacherIds));
    }

    @SuppressWarnings("unchecked")
    private List<ExamGradingAssignment> captureSaved() {
        var captor = (ArgumentCaptor<List<ExamGradingAssignment>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(examGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Map<UUID, Long> countByTeacher(List<ExamGradingAssignment> assignments) {
        var counts = new java.util.HashMap<UUID, Long>();
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

        var counts = countByTeacher(captureSaved());
        assertThat(counts).containsOnlyKeys(anh, binh, chi);
        assertThat(counts.values()).containsExactly(2L, 2L, 2L);
    }

    @Test
    void should_give_the_remainder_to_the_first_teachers_in_order() {
        givenUnassignedResults(7);

        useCase.execute(command(anh, binh, chi));

        // 7 bài / 3 người: hoà thì người đứng trước thắng, nên dư rơi vào Anh.
        var counts = countByTeacher(captureSaved());
        assertThat(counts.get(anh)).isEqualTo(3L);
        assertThat(counts.get(binh)).isEqualTo(2L);
        assertThat(counts.get(chi)).isEqualTo(2L);
    }

    @Test
    void should_start_from_current_load_instead_of_zero() {
        givenUnassignedResults(3);
        // Anh đang giữ 5 bài, Bình 0, Chi 0 -> vòng này Anh không nên nhận thêm.
        when(examGradingQueryRepository.assignedLoadByTeacherIds(anyCollection()))
            .thenReturn(Map.of(anh, 5L, binh, 0L, chi, 0L));

        useCase.execute(command(anh, binh, chi));

        var counts = countByTeacher(captureSaved());
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
        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_when_neither_exam_nor_schedule_is_given() {
        assertThatThrownBy(() ->
            useCase.execute(new AutoAssignGradingCommand(null, null, List.of(anh))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
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
        when(examGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(anh));

        assertThatThrownBy(() -> useCase.execute(command(anh, binh)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }
}
