package com.sep.vox.application.usecase.examsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.DeleteExamSessionCommand;
import com.sep.vox.application.port.input.usecase.examsession.DeleteExamSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá bài thi là xoá MỀM: dữ liệu bài làm phải còn nguyên để đối chiếu khi học sinh thắc mắc điểm,
 * và phải ghi lại xoá lúc nào, vì sao.
 *
 * <p>Bản trước xoá cứng cả cây dữ liệu qua hơn mười bảng; những test đó đã bỏ cùng với cascade.
 */
public class DeleteExamSessionUseCaseTests {

    private ExamSessionRepository examSessionRepository;
    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private DeleteExamSessionUseCase useCase;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    private Exam exam;

    @BeforeEach
    void setUp() {
        examSessionRepository = mock(ExamSessionRepository.class);
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        useCase = new DeleteExamSessionUseCase(
            examSessionRepository,
            examRepository,
            examMemberRepository,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort,
            examCandidateResultRepository,
            examResultAppealRepository
        );

        var session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CENTRALIZED);
        exam.setStatus(ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // SCHOOL_ADMIN cùng trường với bài thi — nhánh cho phép xoá.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(schoolUserRepository.findByUserId(adminId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, adminId, null, null)));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), adminId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));

        when(examSessionRepository.softDelete(eq(sessionId), any(), any())).thenReturn(1);
    }

    private void delete(String reason) {
        useCase.execute(new DeleteExamSessionCommand(sessionId, reason));
    }

    @Test
    void should_soft_delete_the_session_with_its_reason() {
        delete("Vào phòng thi lỗi, phải thi lại");

        var reason = ArgumentCaptor.forClass(String.class);
        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), reason.capture());
        assertThat(reason.getValue()).isEqualTo("Vào phòng thi lỗi, phải thi lại");
    }

    /** Điểm phải biến mất khỏi bảng kết quả, hàng đợi chấm và phúc khảo cùng lúc với phiên thi. */
    @Test
    void should_soft_delete_the_candidate_result_together_with_the_session() {
        delete("Chấm lỗi");

        verify(examCandidateResultRepository).softDeleteBySessionId(eq(sessionId), any(Instant.class), eq("Chấm lỗi"));
    }

    @Test
    void should_reject_a_blank_reason() {
        assertThatThrownBy(() -> delete("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        verify(examSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_trim_the_reason_before_storing_it() {
        delete("  Thí sinh mất kết nối  ");

        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), eq("Thí sinh mất kết nối"));
    }

    /**
     * Kỳ thi đã đóng hoặc đã công bố kết quả thì điểm đã (hoặc sắp) đến tay học sinh — xoá lúc này
     * làm điểm biến mất khỏi bảng kết quả mà học sinh không hiểu vì sao.
     */
    @Test
    void should_refuse_when_the_exam_is_closed() {
        exam.setStatus(ExamStatus.CLOSED);

        assertThatThrownBy(() -> delete("Chấm lỗi"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã đóng");

        verify(examSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_refuse_when_results_are_already_published() {
        exam.setStatus(ExamStatus.RESULTS_PUBLISHED);

        assertThatThrownBy(() -> delete("Chấm lỗi")).isInstanceOf(IllegalStateException.class);

        verify(examSessionRepository, never()).softDelete(any(), any(), any());
    }

    /**
     * Xoá phiên hỏng NGAY TRONG lúc thi chính là lý do tính năng này tồn tại (vào phòng lỗi, chấm
     * lỗi), nên IN_PROGRESS phải vẫn xoá được.
     */
    @Test
    void should_allow_deleting_while_the_exam_is_in_progress() {
        exam.setStatus(ExamStatus.IN_PROGRESS);

        delete("Vào phòng thi lỗi");

        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    /** Bấm xoá hai lần không được ghi đè lý do và mốc thời gian của lần đầu. */
    @Test
    void should_not_touch_the_result_when_the_session_was_already_deleted() {
        when(examSessionRepository.softDelete(eq(sessionId), any(), any())).thenReturn(0);

        delete("Xoá lần hai");

        verify(examCandidateResultRepository, never()).softDeleteBySessionId(any(), any(), any());
    }

    @Test
    void should_refuse_a_teacher_who_is_not_chair() {
        givenTeacherCaller();
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, teacherId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> delete("Chấm lỗi")).isInstanceOf(ForbiddenException.class);
    }

    /**
     * Chủ tịch xoá được phiên hỏng của kỳ thi mình phụ trách, kể cả kỳ thi TẬP TRUNG. Vạch cũ chặn
     * ở đây tự mâu thuẫn: cùng người đó vốn đã xoá được cả thí sinh khỏi kỳ thi tập trung
     * (DeleteExamCandidateUseCase), tức gỡ được cả con người, mà lại không gỡ nổi một lượt thi hỏng.
     */
    @Test
    void should_allow_a_chair_on_a_centralized_exam() {
        givenTeacherCaller();
        exam.setKind(ExamKind.CENTRALIZED);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, teacherId, ExamMemberRole.CHAIR))
            .thenReturn(true);

        delete("Vào phòng thi lỗi");

        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    @Test
    void should_allow_a_chair_on_a_class_test() {
        givenTeacherCaller();
        exam.setKind(ExamKind.CLASS_TEST);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, teacherId, ExamMemberRole.CHAIR))
            .thenReturn(true);

        delete("Chấm lỗi");

        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    /**
     * Đơn phúc khảo đang mở là tranh chấp điểm học sinh đã chính thức nêu — xoá bài lúc này để đơn
     * treo trỏ vào một dòng đã ẩn.
     */
    @Test
    void should_refuse_when_the_result_has_an_open_appeal() {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result));
        when(examResultAppealRepository.existsOpenByCandidateResultId(result.getId())).thenReturn(true);

        assertThatThrownBy(() -> delete("Chấm lỗi"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("phúc khảo");

        verify(examSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_allow_when_the_result_has_no_open_appeal() {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result));
        when(examResultAppealRepository.existsOpenByCandidateResultId(result.getId())).thenReturn(false);

        delete("Chấm lỗi");

        verify(examSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    private void givenTeacherCaller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacherId);
        when(schoolUserRepository.findByUserId(teacherId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, teacherId, null, null)));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), teacherId, UUID.randomUUID(), null, "TEACHER", "Teacher")));
    }
}
