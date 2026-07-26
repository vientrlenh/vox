package com.sep.vox.application.usecase.examgrading;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.AssignGradingUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

public class AssignGradingUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamRepository examRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private AssignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID firstResultId = UUID.randomUUID();
    private final UUID secondResultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examRepository = mock(ExamRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new AssignGradingUseCase(
            examGradingAssignmentRepository,
            examCandidateResultRepository,
            examRepository,
            examGradingQueryRepository,
            examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, ExamCandidateResultStatus.PENDING_REVIEW),
            result(secondResultId, ExamCandidateResultStatus.PENDING_REVIEW)));
        when(examRepository.findByIdIn(anyCollection())).thenReturn(List.of(exam()));
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of());
        when(examGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of(teacherId));
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var assignments = (List<ExamGradingAssignment>) invocation.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    private ExamCandidateResult result(UUID candidateResultId, ExamCandidateResultStatus status) {
        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setExamId(examId);
        candidateResult.setStatus(status);
        return candidateResult;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private ExamGradingAssignment existingAssignment(UUID candidateResultId) {
        var assignment = ExamGradingAssignment.open(candidateResultId, UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, OffsetDateTime.now(), adminId, null);
        assignment.setId(UUID.randomUUID());
        return assignment;
    }

    private AssignGradingCommand command(UUID... candidateResultIds) {
        return command(GradingRoundType.INITIAL, candidateResultIds);
    }

    private AssignGradingCommand command(GradingRoundType roundType, UUID... candidateResultIds) {
        return new AssignGradingCommand(roundType.name(), null, List.of(candidateResultIds).stream()
            .map(id -> new AssignGradingCommand.AssignmentItem(id, teacherId))
            .toList());
    }

    @SuppressWarnings("unchecked")
    private List<ExamGradingAssignment> captureSaved() {
        var captor = (ArgumentCaptor<List<ExamGradingAssignment>>) (ArgumentCaptor<?>)
            ArgumentCaptor.forClass(List.class);
        verify(examGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_assign_every_selected_result_to_the_chosen_teacher() {
        var assignmentIds = useCase.execute(command(firstResultId, secondResultId));

        assertThat(assignmentIds).hasSize(2);
        var saved = captureSaved();
        assertThat(saved).extracting(ExamGradingAssignment::getCandidateResultId)
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
        verify(examCandidateResultRepository, times(1)).findByIdIn(anyCollection());
        verify(examRepository, times(1)).findByIdIn(anyCollection());
        verify(examGradingAssignmentRepository, times(1)).findOpenByCandidateResultIdIn(anyCollection());
        verify(examGradingQueryRepository, times(1)).findTeacherIdsInSchool(eq(schoolId), anyCollection());
        // Phân quyền chạy một lần cho trường duy nhất, không lặp theo bài.
        verify(examGradingAccessService, times(1)).authorizeSchoolAdmin(eq(schoolId), eq(adminId));
    }

    @Test
    void should_reject_when_a_result_already_has_an_assignment() {
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection()))
            .thenReturn(List.of(existingAssignment(secondResultId)));

        // Một bài, một giáo viên. Unique index là chốt cuối; đây là lỗi đọc được.
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, secondResultId)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("đang được một giáo viên chấm");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_duplicate_results_in_the_same_batch() {
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, firstResultId)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("trùng bài thi");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_a_result_whose_status_does_not_match_the_round() {
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, ExamCandidateResultStatus.PENDING_REVIEW),
            result(secondResultId, ExamCandidateResultStatus.RELEASED)));

        // Vòng INITIAL chỉ nhận bài PENDING_REVIEW; bài RELEASED thuộc vòng SPOT_CHECK.
        assertThatThrownBy(() -> useCase.execute(command(firstResultId, secondResultId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INITIAL");

        // Bài đầu hợp lệ vẫn không được ghi: validate hết rồi mới persist.
        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_assign_released_results_to_a_spot_check_round() {
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(firstResultId, ExamCandidateResultStatus.RELEASED)));

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

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_snapshot_the_current_score_when_assigning() {
        var released = result(firstResultId, ExamCandidateResultStatus.RELEASED);
        released.setTotalScore(new java.math.BigDecimal("6.50"));
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(released));

        useCase.execute(command(GradingRoundType.SPOT_CHECK, firstResultId));

        // scoreBefore chụp lúc GIAO: lấy sau thì chính giáo viên đã sửa mất mốc so lệch.
        assertThat(captureSaved()).singleElement()
            .satisfies(assignment ->
                assertThat(assignment.getScoreBefore()).isEqualByComparingTo("6.50"));
    }

    @Test
    void should_reject_a_grader_who_is_not_a_teacher_of_the_same_school() {
        var outsider = UUID.randomUUID();
        // Query lọc giáo viên trả tập rỗng cho người ngoài trường.
        when(examGradingQueryRepository.findTeacherIdsInSchool(eq(schoolId), anyCollection()))
            .thenReturn(Set.of());
        when(examCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(firstResultId, ExamCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(new AssignGradingCommand(
            GradingRoundType.INITIAL.name(), null, List.of(
                new AssignGradingCommand.AssignmentItem(firstResultId, outsider)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_when_caller_is_not_school_admin_of_the_exam() {
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examGradingAccessService).authorizeSchoolAdmin(eq(schoolId), eq(adminId));
        when(examCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(firstResultId, ExamCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(command(firstResultId)))
            .isInstanceOf(ForbiddenException.class);

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_an_empty_batch() {
        assertThatThrownBy(() -> useCase.execute(
            new AssignGradingCommand(GradingRoundType.INITIAL.name(), null, List.of())))
            .isInstanceOf(IllegalArgumentException.class);

        verify(examGradingAssignmentRepository, never()).saveAll(any());
    }
}
