package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Ba nhóm người xem với ba lý do khác nhau. Cổng {@code @PreAuthorize} của controller
 * cho cả TEACHER vào, nên nếu use case không có nhánh cho họ thì giáo viên LUÔN nhận
 * {@code Forbidden} — cửa mở nhưng bên trong khoá (review BE-14).
 */
class ViewResultStatusHistoryUseCaseTests {

    private AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private UserContextPort userContextPort;
    private ViewResultStatusHistoryUseCase useCase;

    private final UUID candidateResultId = UUID.randomUUID();
    private AssessmentCandidateResult result;
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentResultAuditQueryRepository = mock(AssessmentResultAuditQueryRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewResultStatusHistoryUseCase(
            assessmentResultAuditQueryRepository,
            assessmentCandidateResultRepository,
            assessmentCandidateRepository,
            assessmentGradingAssignmentRepository,
            assessmentRepository,
            assessmentGradingAccessService,
            userContextPort);

        result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setAssessmentId(assessmentId);
        result.setStatus(AssessmentCandidateResultStatus.RELEASED);
        when(assessmentCandidateResultRepository.findById(candidateResultId)).thenReturn(Optional.of(result));

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        when(assessmentResultAuditQueryRepository.findHistory(candidateResultId)).thenReturn(List.of(
            new ResultStatusHistoryInfo(UUID.randomUUID(), candidateResultId,
                "PENDING_REVIEW", "RELEASED", null, null,
                "TEACHER_INITIAL", teacherId, "Cô Lan", "AI chấm đúng", Instant.now())));
    }

    private void loggedInAs(UUID userId) {
        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(userId);
    }

    /**
     * "Từng được giao", không phải "đang cầm": phân công đã COMPLETED cũng tính. Mock trả
     * {@code false} mặc định nên các ca phủ định không cần khai gì thêm.
     */
    private void givenEverAssignedTo(UUID assignedTeacherId) {
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, assignedTeacherId))
            .thenReturn(true);
    }

    private void schoolAdminIsRefused() {
        doThrow(new ForbiddenException("BẢO MẬT")).when(assessmentGradingAccessService)
            .authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_let_the_owning_student_see_their_own_timeline() {
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // Chủ bài không phải đi qua nhánh phân quyền admin.
        verify(assessmentGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    /**
     * Dòng thời gian mang scoreBefore/scoreAfter — với chính chủ nó chính là điểm, nên nó
     * chịu đúng luật công bố như màn kết quả. Trả rỗng chứ không ném lỗi: đây là khối phụ
     * trợ, ném lỗi sẽ làm hỏng cả trang chỉ vì một mục bên lề.
     */
    @Test
    void should_hide_timeline_from_the_owning_student_until_the_result_is_concluded() {
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).isEmpty();
    }

    @Test
    void should_still_show_the_timeline_to_the_assigned_teacher_while_pending_review() {
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    @Test
    void should_let_the_teacher_currently_holding_the_paper_see_it() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        // Lịch sử điểm chính là ngữ cảnh giáo viên cần khi chấm phúc khảo.
        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    /**
     * Once the score is submitted the assignment closes -- but the teacher's queue still lists that attempt
     * on the "Graded" tab, and the Score History button there must be clickable. The same rule as
     * listening to the recording ({@code AssessmentRecordingAccessService}).
     */
    @Test
    void should_let_a_teacher_who_was_ever_assigned_see_it_after_the_assignment_closed() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    @Test
    void should_refuse_a_teacher_who_was_never_assigned_to_this_paper() {
        loggedInAs(teacherId);
        // Bài này có người khác cầm; người đăng nhập chưa từng được giao.
        givenEverAssignedTo(UUID.randomUUID());
        schoolAdminIsRefused();

        assertThatThrownBy(() -> useCase.execute(candidateResultId))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * Khoá lại chủ đích: quyền đọc suy từ "đã từng được giao", KHÔNG từ dòng phân công
     * đang mở — quay lại lối cũ là giáo viên vừa nộp điểm xong mất quyền xem ngay.
     */
    @Test
    void should_not_look_at_the_open_assignment_row_to_authorize_a_teacher() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        useCase.execute(candidateResultId);

        verify(assessmentGradingAssignmentRepository, never()).findOpenByCandidateResultId(any());
    }

    @Test
    void should_let_a_school_admin_of_the_same_school_see_it() {
        UUID adminId = UUID.randomUUID();
        loggedInAs(adminId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // The scope of the check is the ASSESSMENT'S SCHOOL, not the logged-in user's school.
        verify(assessmentGradingAccessService).authorizeSchoolAdmin(schoolId, adminId);
    }

    @Test
    void should_let_a_system_admin_see_it_without_a_school_check() {
        loggedInAs(UUID.randomUUID());
        when(userContextPort.isSystemAdmin()).thenReturn(true);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        verify(assessmentGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_fail_when_the_result_does_not_exist() {
        loggedInAs(studentId);
        UUID unknownId = UUID.randomUUID();
        when(assessmentCandidateResultRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(unknownId)).isInstanceOf(NotFoundException.class);
    }
}
