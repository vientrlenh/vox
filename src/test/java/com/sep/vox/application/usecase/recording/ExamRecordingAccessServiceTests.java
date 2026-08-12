package com.sep.vox.application.usecase.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.ExamRecordingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Bốn tầng quyền xem bản ghi, trong đó tầng "người được phân công chấm" là tầng dễ quên nhất:
 * vòng hậu kiểm và phúc khảo cố tình giao bài cho giáo viên KHÔNG dính tới ca thi, nên nếu quyền
 * chỉ đi theo vai coi thi thì người chấm mở màn chấm ra là mất luôn video để nghe.
 */
class ExamRecordingAccessServiceTests {

    private ExamSessionRepository examSessionRepository;
    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private UserContextPort userContextPort;
    private ExamRecordingAccessService accessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examSessionRepository = mock(ExamSessionRepository.class);
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        userContextPort = mock(UserContextPort.class);
        accessService = new ExamRecordingAccessService(
            examSessionRepository, examRepository, examCandidateRepository, examMemberRepository,
            examScheduleProctorRepository, examCandidateResultRepository,
            examGradingAssignmentRepository, userContextPort);

        // Mặc định: giáo viên cùng trường, KHÔNG chủ tịch, KHÔNG giám thị, KHÔNG được phân công.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userContextPort.isSchoolAdmin()).thenReturn(false);
        when(userContextPort.isTeacher()).thenReturn(true);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session()));
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId))
            .thenReturn(false);
        when(examCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(candidateResult()));
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(false);
    }

    @Test
    void should_allow_assigned_grader_who_is_not_proctor() {
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        var session = accessService.requireCanViewRecordings(sessionId);

        assertThat(session.getId()).isEqualTo(sessionId);
    }

    /**
     * Không hỏi tới bảng giám thị khi đã có dòng phân công: người chấm là ca phổ biến nhất của
     * màn này, để nó rơi xuống tận nhánh cuối là mỗi lần mở bài thêm hai query thừa.
     */
    @Test
    void should_not_query_proctor_table_when_caller_is_assigned_grader() {
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        accessService.requireCanViewRecordings(sessionId);

        verify(examScheduleProctorRepository, never()).existsByScheduleIdAndTeacherId(scheduleId, userId);
    }

    /**
     * Chấm xong vẫn phải xem lại được: bài bị phúc khảo thì chính người chấm vòng trước là người
     * bị hỏi lại "vì sao chấm thế", mà lúc đó dòng phân công của họ đã COMPLETED.
     */
    @Test
    void should_allow_grader_of_a_completed_assignment() {
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_school_admin() {
        when(userContextPort.isSchoolAdmin()).thenReturn(true);
        when(userContextPort.isTeacher()).thenReturn(false);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_exam_chair() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_schedule_proctor() {
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId))
            .thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_reject_teacher_without_assignment_and_without_proctor_role() {
        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * Bài chưa có {@code ExamCandidateResult} (chưa chấm AI xong) thì không có gì để đối chiếu
     * phân công — phải rơi xuống nhánh giám thị chứ không được cho qua.
     */
    @Test
    void should_reject_when_session_has_no_candidate_result() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_teacher_from_another_school() {
        when(userContextPort.getCurrentSchoolId()).thenReturn(UUID.randomUUID());
        when(examGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_user_without_school() {
        when(userContextPort.getCurrentSchoolId()).thenReturn(null);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    /** Học sinh không xem lại được video buổi thi của chính mình qua đường này. */
    @Test
    void should_reject_non_teacher_non_admin_role() {
        when(userContextPort.isTeacher()).thenReturn(false);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    private ExamSession session() {
        var session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setCandidateId(candidateId);
        return session;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private ExamCandidate candidate() {
        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setExamId(examId);
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private ExamCandidateResult candidateResult() {
        var result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setExamId(examId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        return result;
    }
}
