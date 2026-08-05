package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

class ClassTestGradingAssignmentServiceTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ClassTestGradingAssignmentService service;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        service = new ClassTestGradingAssignmentService(
            examRepository, examMemberRepository, examCandidateResultRepository, examGradingAssignmentRepository);

        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(ExamKind.CLASS_TEST)));
        when(examMemberRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
            new ExamMember(EXAM_ID, CHAIR_ID, ExamMemberRole.CHAIR, Instant.now(), CHAIR_ID)
        ));
        when(examGradingAssignmentRepository.findOpenByCandidateResultId(any())).thenReturn(Optional.empty());
    }

    @Test
    void should_open_initial_assignment_for_class_test_chair() {
        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.PENDING_REVIEW));

        var captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository).save(captor.capture());
        var assignment = captor.getValue();
        assertThat(assignment.getCandidateResultId()).isEqualTo(RESULT_ID);
        assertThat(assignment.getTeacherId()).isEqualTo(CHAIR_ID);
        assertThat(assignment.getAssignedBy()).isEqualTo(CHAIR_ID);
        assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.ASSIGNED);
        // activeResultId là thứ dựng bất biến "một phân công mở / bài" ở DB.
        assertThat(assignment.getActiveResultId()).isEqualTo(RESULT_ID);
    }

    /** Mốc đo độ lệch AI ↔ người: lấy sau thì đã bị chính giáo viên sửa mất. */
    @Test
    void should_capture_score_before_at_the_moment_of_opening() {
        var result = result(ExamCandidateResultStatus.PENDING_REVIEW);
        result.setTotalScore(new BigDecimal("6.25"));

        service.ensureAssignmentForResult(result);

        var captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getScoreBefore()).isEqualByComparingTo("6.25");
    }

    /** Bài trên lớp không có hạn chấm hành chính — job nhắc hạn chỉ bắn cho dòng CÓ hạn. */
    @Test
    void should_open_without_deadline() {
        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.PENDING_REVIEW));

        var captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDeadlineAt()).isNull();
    }

    @Test
    void should_ignore_centralized_exam() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(ExamKind.CENTRALIZED)));

        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.PENDING_REVIEW));

        verify(examGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_result_that_is_not_pending_review() {
        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.RELEASED));

        verify(examGradingAssignmentRepository, never()).save(any());
    }

    /**
     * Hàng rào chống vi phạm unique index {@code uq_grading_assignment_active_result}:
     * {@code ClearInvalidResultUseCase} đã tự mở một vòng INITIAL mới rồi.
     */
    @Test
    void should_not_open_second_assignment_when_one_is_already_open() {
        when(examGradingAssignmentRepository.findOpenByCandidateResultId(RESULT_ID))
            .thenReturn(Optional.of(mock(ExamGradingAssignment.class)));

        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.PENDING_REVIEW));

        verify(examGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_exam_without_chair() {
        when(examMemberRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
            new ExamMember(EXAM_ID, UUID.randomUUID(), ExamMemberRole.AUTHOR, Instant.now(), CHAIR_ID)
        ));

        service.ensureAssignmentForResult(result(ExamCandidateResultStatus.PENDING_REVIEW));

        verify(examGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_sweep_only_pending_review_results_of_the_exam() {
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        var released = result(ExamCandidateResultStatus.RELEASED);
        released.setId(UUID.randomUUID());
        when(examCandidateResultRepository.findByExamId(EXAM_ID)).thenReturn(List.of(pending, released));
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyList())).thenReturn(List.of());
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        var opened = service.ensureAssignmentsForExam(EXAM_ID);

        assertThat(opened).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExamGradingAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(examGradingAssignmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
            .satisfies(assignment -> assertThat(assignment.getCandidateResultId()).isEqualTo(RESULT_ID));
    }

    /** Hai điểm móc chồng lên nhau là chuyện bình thường — quét bù không được nhân đôi. */
    @Test
    void should_skip_results_that_already_have_an_open_assignment_when_sweeping() {
        var pending = result(ExamCandidateResultStatus.PENDING_REVIEW);
        when(examCandidateResultRepository.findByExamId(EXAM_ID)).thenReturn(List.of(pending));
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyList())).thenReturn(List.of(
            ExamGradingAssignment.open(
                RESULT_ID, CHAIR_ID, GradingRoundType.INITIAL, null, null, Instant.now(), CHAIR_ID, null)
        ));

        var opened = service.ensureAssignmentsForExam(EXAM_ID);

        assertThat(opened).isZero();
        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_not_sweep_centralized_exam() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(ExamKind.CENTRALIZED)));

        assertThat(service.ensureAssignmentsForExam(EXAM_ID)).isZero();
        verify(examCandidateResultRepository, never()).findByExamId(any());
    }

    private Exam exam(ExamKind kind) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(kind);
        return exam;
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult();
        result.setId(RESULT_ID);
        result.setExamId(EXAM_ID);
        result.setStatus(status);
        return result;
    }
}
