package com.sep.vox.application.usecase.examgrading;

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
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.ExamResultAuditQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Ba nhóm người xem với ba lý do khác nhau. Cổng {@code @PreAuthorize} của controller
 * cho cả TEACHER vào, nên nếu use case không có nhánh cho họ thì giáo viên LUÔN nhận
 * {@code Forbidden} — cửa mở nhưng bên trong khoá (review BE-14).
 */
class ViewResultStatusHistoryUseCaseTests {

    private ExamResultAuditQueryRepository examResultAuditQueryRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamRepository examRepository;
    private ExamGradingAccessService examGradingAccessService;
    private UserContextPort userContextPort;
    private ViewResultStatusHistoryUseCase useCase;

    private final UUID candidateResultId = UUID.randomUUID();
    private ExamCandidateResult result;
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examResultAuditQueryRepository = mock(ExamResultAuditQueryRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examRepository = mock(ExamRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewResultStatusHistoryUseCase(
            examResultAuditQueryRepository,
            examCandidateResultRepository,
            examCandidateRepository,
            examGradingAssignmentRepository,
            examRepository,
            examGradingAccessService,
            userContextPort);

        result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setExamId(examId);
        result.setStatus(ExamCandidateResultStatus.RELEASED);
        when(examCandidateResultRepository.findById(candidateResultId)).thenReturn(Optional.of(result));

        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        when(examResultAuditQueryRepository.findHistory(candidateResultId)).thenReturn(List.of(
            new ResultStatusHistoryInfo(UUID.randomUUID(), candidateResultId,
                "PENDING_REVIEW", "RELEASED", null, null,
                "TEACHER_INITIAL", teacherId, "Cô Lan", "AI chấm đúng", Instant.now())));
    }

    private void loggedInAs(UUID userId) {
        when(examGradingAccessService.requireActiveUserId()).thenReturn(userId);
    }

    /**
     * "Từng được giao", không phải "đang cầm": phân công đã COMPLETED cũng tính. Mock trả
     * {@code false} mặc định nên các ca phủ định không cần khai gì thêm.
     */
    private void givenEverAssignedTo(UUID assignedTeacherId) {
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, assignedTeacherId))
            .thenReturn(true);
    }

    private void schoolAdminIsRefused() {
        doThrow(new ForbiddenException("BẢO MẬT")).when(examGradingAccessService)
            .authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_let_the_owning_student_see_their_own_timeline() {
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // Chủ bài không phải đi qua nhánh phân quyền admin.
        verify(examGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    /**
     * Dòng thời gian mang scoreBefore/scoreAfter — với chính chủ nó chính là điểm, nên nó
     * chịu đúng luật công bố như màn kết quả. Trả rỗng chứ không ném lỗi: đây là khối phụ
     * trợ, ném lỗi sẽ làm hỏng cả trang chỉ vì một mục bên lề.
     */
    @Test
    void should_hide_timeline_from_the_owning_student_until_the_result_is_concluded() {
        result.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).isEmpty();
    }

    @Test
    void should_still_show_the_timeline_to_the_assigned_teacher_while_pending_review() {
        result.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);
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
     * Nộp điểm xong là phân công đóng lại — nhưng hàng đợi của giáo viên vẫn liệt kê bài
     * đó ở tab "Đã chấm xong", và nút Lịch sử điểm ở đấy phải bấm được. Cùng luật với
     * nghe lại bản ghi ({@code ExamRecordingAccessService}).
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

        verify(examGradingAssignmentRepository, never()).findOpenByCandidateResultId(any());
    }

    @Test
    void should_let_a_school_admin_of_the_same_school_see_it() {
        var adminId = UUID.randomUUID();
        loggedInAs(adminId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // Phạm vi kiểm là TRƯỜNG CỦA BÀI THI, không phải trường của người đăng nhập.
        verify(examGradingAccessService).authorizeSchoolAdmin(schoolId, adminId);
    }

    @Test
    void should_let_a_system_admin_see_it_without_a_school_check() {
        loggedInAs(UUID.randomUUID());
        when(userContextPort.isSystemAdmin()).thenReturn(true);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        verify(examGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_fail_when_the_result_does_not_exist() {
        loggedInAs(studentId);
        var unknownId = UUID.randomUUID();
        when(examCandidateResultRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(unknownId)).isInstanceOf(NotFoundException.class);
    }
}
