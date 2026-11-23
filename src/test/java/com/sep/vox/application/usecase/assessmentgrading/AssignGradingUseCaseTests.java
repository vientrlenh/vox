package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentgrading.AssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AssignGradingUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

public class AssignGradingUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID firstResultId = UUID.randomUUID();
    private final UUID secondResultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new AssignGradingUseCase(
            assessmentGradingAssignmentRepository,
            assessmentCandidateResultRepository,
            assessmentRepository,
            assessmentGradingQueryRepository,
            assessmentGradingAccessService,
            mock(GradingAssignmentNotificationService.class));

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, AssessmentCandidateResultStatus.PENDING_REVIEW),
            result(secondResultId, AssessmentCandidateResultStatus.PENDING_REVIEW)));
        when(assessmentRepository.findByIdIn(anyCollection())).thenReturn(List.of(assessment()));
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of());
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(teacherId));
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AssessmentGradingAssignment> assignments = (List<AssessmentGradingAssignment>) invocation.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    private AssessmentCandidateResult result(UUID candidateResultId, AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setAssessmentId(assessmentId);
        candidateResult.setStatus(status);
        return candidateResult;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private AssessmentGradingAssignment existingAssignment(UUID candidateResultId) {
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, Instant.now(), adminId, null);
        assignment.setId(UUID.randomUUID());
        return assignment;
    }

    private AssignGradingCommand command(UUID... candidateResultIds) {
        return command(GradingRoundType.INITIAL, candidateResultIds);
    }

    private AssignGradingCommand command(GradingRoundType roundType, UUID... candidateResultIds) {
        return new AssignGradingCommand(roundType, null, List.of(candidateResultIds).stream()
            .map(id -> new AssignGradingCommand.AssignmentItem(id, teacherId))
            .toList());
    }

    @SuppressWarnings("unchecked")
    private List<AssessmentGradingAssignment> captureSaved() {
        ArgumentCaptor<List<AssessmentGradingAssignment>> captor = (ArgumentCaptor<List<AssessmentGradingAssignment>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(assessmentGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_assign_every_selected_result_to_the_chosen_teacher() {
        List<UUID> assignmentIds = useCase.execute(command(firstResultId, secondResultId));

        assertThat(assignmentIds).hasSize(2);
        List<AssessmentGradingAssignment> saved = captureSaved();
        assertThat(saved).extracting(grading -> grading.getCandidateResultId())
            .containsExactlyInAnyOrder(firstResultId, secondResultId);
        assertThat(saved).allSatisfy(assignment -> {
            assertThat(assignment.getTeacherId()).isEqualTo(teacherId);
            assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.ASSIGNED);
            assertThat(assignment.getAssignedBy()).isEqualTo(adminId);
            assertThat(assignment.getCompletedAt()).isNull();
        });
    }

    @Test
    void should_batch_all_lookups_regardless_of_batch_size() {
        useCase.execute(command(firstResultId, secondResultId));

        // Không N+1: mỗi lookup đúng một lần cho cả lô, không nhân theo số bài.
        verify(assessmentCandidateResultRepository, times(1)).findByIdIn(anyCollection());
        verify(assessmentRepository, times(1)).findByIdIn(anyCollection());
        verify(assessmentGradingAssignmentRepository, times(1)).findOpenByCandidateResultIdIn(anyCollection());
        verify(assessmentGradingQueryRepository, times(1)).findTeacherIdsInSchool(eq(schoolId), anyCollection());
        // Phân quyền chạy một lần cho trường duy nhất, không lặp theo bài.
        verify(assessmentGradingAccessService, times(1)).authorizeSchoolAdmin(eq(schoolId), eq(adminId));
    }

    @Test
    void should_reject_when_a_result_already_has_an_assignment() {
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of(existingAssignment(secondResultId)));

        // Một bài, một giáo viên. Unique index là chốt cuối; đây là lỗi đọc được.
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, secondResultId)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("đang được một giáo viên chấm");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_duplicate_results_in_the_same_batch() {
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, firstResultId)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("trùng bài thi");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_a_result_whose_status_does_not_match_the_round() {
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, AssessmentCandidateResultStatus.PENDING_REVIEW),
            result(secondResultId, AssessmentCandidateResultStatus.RELEASED)));

        // Vòng INITIAL chỉ nhận bài PENDING_REVIEW; bài RELEASED thuộc vòng SPOT_CHECK.
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, secondResultId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("INITIAL");

        // Bài đầu hợp lệ vẫn không được ghi: validate hết rồi mới persist.
        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_assign_released_results_to_a_spot_check_round() {
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, AssessmentCandidateResultStatus.RELEASED)));

        useCase.execute(command(GradingRoundType.SPOT_CHECK, firstResultId));

        // Hậu kiểm là vòng dành cho bài ĐÃ công bố — bản cũ không giao được bài này.
        assertThat(captureSaved()).singleElement()
            .satisfies(assignment ->
                assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.SPOT_CHECK));
    }

    @Test
    void should_refuse_to_assign_the_appeal_round_here() {
        // Vòng phúc khảo gắn với một đơn cụ thể và có luật xung đột lợi ích riêng,
        // nên nó chỉ được giao từ màn đơn phúc khảo.
        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.APPEAL, firstResultId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phúc khảo");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_snapshot_the_current_score_when_assigning() {
        AssessmentCandidateResult released = result(firstResultId, AssessmentCandidateResultStatus.RELEASED);
        released.setTotalScore(new java.math.BigDecimal("6.50"));
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(released));

        useCase.execute(command(GradingRoundType.SPOT_CHECK, firstResultId));

        // scoreBefore is captured at ASSIGNMENT time: if taken later, the teacher has already
        // edited it and the deviation reference point is lost.
        assertThat(captureSaved()).singleElement()
            .satisfies(assignment ->
                assertThat(assignment.getScoreBefore()).isEqualByComparingTo("6.50"));
    }

    @Test
    void should_reject_a_grader_who_is_not_a_teacher_of_the_same_school() {
        UUID outsider = UUID.randomUUID();
        // The teacher-filtering query returns an empty set for someone outside the school.
        when(assessmentGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of());
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(firstResultId, AssessmentCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(new AssignGradingCommand(
            GradingRoundType.INITIAL, null, List.of(
                new AssignGradingCommand.AssignmentItem(firstResultId, outsider)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_when_caller_is_not_school_admin_of_the_assessment() {
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(assessmentGradingAccessService).authorizeSchoolAdmin(eq(schoolId), eq(adminId));
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(firstResultId, AssessmentCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(command(firstResultId)))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    /**
     * Bài kiểm tra trên lớp do giáo viên tạo bài tự chấm — nhà trường không phân công.
     * Không chặn thì admin gán giáo viên khác trong khi chủ bài đã được gán tự động,
     * hai bên tranh cùng một {@code active_result_id}.
     */
    @Test
    void should_reject_class_test_result() {
        org.mockito.Mockito.doThrow(new ForbiddenException(
                AssessmentGradingAccessService.CLASS_TEST_COORDINATION_REJECTION))
            .when(assessmentGradingAccessService).rejectClassTestCoordination(anyCollection());
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(firstResultId, AssessmentCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(command(firstResultId)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("nhà trường không phân công");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_an_empty_batch() {
        assertThatThrownBy(() -> useCase.execute(
            new AssignGradingCommand(GradingRoundType.INITIAL, null, List.of())))
            .isInstanceOf(IllegalArgumentException.class);

        verify(assessmentGradingAssignmentRepository, never()).saveAll(any());
    }
}
