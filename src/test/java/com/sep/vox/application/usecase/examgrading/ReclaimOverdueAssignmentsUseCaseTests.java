package com.sep.vox.application.usecase.examgrading;

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

import com.sep.vox.application.port.input.command.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.examgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Thu hồi quá hạn là một trong ba đường đóng phân công KHÔNG đi qua
 * {@code GradingActionSupport.finish()}, nên nó phải tự nhả đơn phúc khảo (review BE-1)
 * — và chỉ nhả khi không có người thay (review BE-8 cho phần phạm vi quét).
 */
class ReclaimOverdueAssignmentsUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ReclaimOverdueAssignmentsUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    private ExamResultAppeal appeal;

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ReclaimOverdueAssignmentsUseCase(
            examGradingAssignmentRepository,
            examResultAppealRepository,
            examGradingQueryRepository,
            examGradingAccessService,
            new RoundRobinLoadBalancer());

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(examGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.GRADING);
        when(examResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
    }

    private ExamGradingAssignment overdue(GradingRoundType roundType, UUID linkedAppealId) {
        var now = Instant.now();
        var assignment = ExamGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, linkedAppealId, null,
            now.minus(3, ChronoUnit.DAYS), adminId, now.minus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        return assignment;
    }

    private void givenOverdue(ExamGradingAssignment... assignments) {
        when(examGradingAssignmentRepository.findOverdueInSchool(any(), any(), any(), anyInt()))
            .thenReturn(List.of(assignments));
    }

    private ReclaimOverdueAssignmentsCommand reclaimAll(List<UUID> reassignTo) {
        return new ReclaimOverdueAssignmentsCommand(examId, null, reassignTo, null);
    }

    @Test
    void should_release_the_appeal_back_to_approved_when_reclaiming_without_a_replacement() {
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));

        useCase.execute(reclaimAll(null));

        // Không nhả thì đơn đứng ở GRADING vĩnh viễn: giao lại đòi APPROVED, duyệt lại
        // đòi PENDING — không còn đường nào ngoài sửa tay dưới DB.
        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.APPROVED);
        verify(examResultAppealRepository).save(appeal);
    }

    @Test
    void should_keep_the_appeal_in_grading_when_a_replacement_takes_over_immediately() {
        var replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(examGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(examGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        useCase.execute(reclaimAll(List.of(replacement)));

        // Dòng mới mở ngay với cùng appealId trong cùng transaction, nên GRADING vẫn
        // đúng — nhả rồi kéo lại chỉ tạo một khoảng trạng thái sai ở giữa.
        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.GRADING);
        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_a_replacement_who_already_graded_the_paper_under_appeal() {
        var conflicted = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(examGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(conflicted)))
            .thenReturn(Set.of(conflicted));
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(conflicted));

        // Cùng luật COI mà ReassignGradingUseCase đã áp cho đổi người từng dòng: giao
        // lại hàng loạt không được là cửa sau để lách nó.
        assertThatThrownBy(() -> useCase.execute(reclaimAll(List.of(conflicted))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã từng chấm");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_allow_a_clean_replacement_for_an_appeal_round() {
        var replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.APPEAL, appealId));
        when(examGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(UUID.randomUUID()));
        when(examGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        assertThat(useCase.execute(reclaimAll(List.of(replacement))).reassignedAssignmentIds()).hasSize(1);
    }

    @Test
    void should_not_ask_about_conflicts_when_no_appeal_round_is_involved() {
        var replacement = UUID.randomUUID();
        givenOverdue(overdue(GradingRoundType.INITIAL, null));
        when(examGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(replacement)))
            .thenReturn(Set.of(replacement));
        when(examGradingQueryRepository.assignedLoadByTeacherIds(anyCollection())).thenReturn(Map.of());

        useCase.execute(reclaimAll(List.of(replacement)));

        // Ba vòng còn lại không có luật COI — hỏi thừa là thêm query cho mọi lượt thu hồi.
        verify(examGradingQueryRepository, never()).findTeacherIdsWithHumanEvaluation(any());
    }

    @Test
    void should_leave_appeals_alone_for_the_other_three_rounds() {
        givenOverdue(overdue(GradingRoundType.INITIAL, null));

        useCase.execute(reclaimAll(null));

        verify(examResultAppealRepository, never()).findById(any());
        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_still_close_the_reclaimed_assignment_as_declined() {
        var assignment = overdue(GradingRoundType.APPEAL, appealId);
        givenOverdue(assignment);

        useCase.execute(reclaimAll(null));

        assertThat(assignment.isCompleted()).isTrue();
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_cap_one_batch_and_tell_the_admin_there_is_more_to_do() {
        var tooMany = IntStream.range(0, 501)
            .mapToObj(ignored -> overdue(GradingRoundType.INITIAL, null))
            .toArray(ExamGradingAssignment[]::new);
        givenOverdue(tooMany);

        var response = useCase.execute(reclaimAll(null));

        // Một cú bấm "Thu hồi toàn bộ" không được ôm cả trường vào một transaction ghi;
        // cờ hasMore là thứ cho admin biết cần bấm tiếp thay vì tưởng đã xong.
        assertThat(response.reclaimedAssignmentIds()).hasSize(500);
        assertThat(response.hasMore()).isTrue();
        // Lấy dư đúng MỘT dòng là đủ để biết còn nữa — không cần thêm câu COUNT.
        verify(examGradingAssignmentRepository).findOverdueInSchool(any(), any(), any(), eq(501));
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
        verify(examGradingAssignmentRepository).findOverdueInSchool(any(), any(), any(), anyInt());
        verify(examGradingAssignmentRepository, never()).findOverdue(any());
        verify(examGradingAccessService, never()).load(any());
    }
}
