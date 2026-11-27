package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.assessment.ViewMyAssessmentsQuery;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.dto.StudentAssessmentRowInfo;
import com.sep.vox.application.query.repository.AssessmentCandidateAttemptsQueryRepository;
import com.sep.vox.application.query.repository.StudentAssessmentQueryRepository;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

/**
 * What remains in Java after filtering/sorting/paging moved down to SQL: building the response and deciding whether the student
 * may enter the assessment room.
 *
 * <p>The rules about scope and order (hide DRAFT assessments, hide unpublished sessions, newest first, paging
 * from 1) now live in the JPQL so they are checked in
 * {@code JpaStudentAssessmentQueryRepositoryTests} against the real DB -- mocking the repository here and then
 * asserting them again would only test the mock itself.
 */
class ViewMyAssessmentsUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private StudentAssessmentQueryRepository studentAssessmentQueryRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentCandidateAttemptsQueryRepository attemptsQueryRepository;
    private ViewMyAssessmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        studentAssessmentQueryRepository = mock(StudentAssessmentQueryRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        attemptsQueryRepository = mock(AssessmentCandidateAttemptsQueryRepository.class);
        AssessmentPaperRepository assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new ViewMyAssessmentsUseCase(
            studentAssessmentQueryRepository,
            assessmentPaperRepository,
            assessmentScheduleRepository,
            attemptsQueryRepository,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(assessmentPaperRepository.findByIdIn(anyCollection())).thenReturn(List.of());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection())).thenReturn(List.of());
        // A session that is still valid -- dedicated session checks override it when needed.
        when(assessmentScheduleRepository.findByIdAndInSchedule(any(), any()))
            .thenReturn(Optional.of(new AssessmentSchedule()));
    }

    @Test
    void should_pass_the_paging_envelope_through_untouched() {
        givenRows(new PageResult<>(List.of(row().build()), 2, 20, 41, 3));

        PageResult<StudentAssessmentSummaryResponse> result = useCase.execute(new ViewMyAssessmentsQuery(null, null, 2, 20, true));

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void should_forward_the_one_based_page_to_the_query_repository() {
        givenRows(new PageResult<>(List.of(), 1, 20, 0, 0));

        useCase.execute(new ViewMyAssessmentsQuery(null, null, 1, 20, true));

        org.mockito.Mockito.verify(studentAssessmentQueryRepository)
            .findMyAssessments(eq(STUDENT_ID), eq(null), eq(null), eq(true), eq(1), eq(20), any());
    }

    @Test
    void should_let_an_attended_student_of_a_running_assessment_enter() {
        givenRows(row().candidateStatus(AssessmentCandidateStatus.ATTENDED).assessmentStatus(AssessmentStatus.IN_PROGRESS).build());

        StudentAssessmentSummaryResponse response = firstResponse();

        assertThat(response.canEnter()).isTrue();
        assertThat(response.entryMessage()).isNull();
    }

    @Test
    void should_block_a_student_who_was_forced_to_stop() {
        givenRows(row()
            .candidateStatus(AssessmentCandidateStatus.ATTENDED)
            .assessmentStatus(AssessmentStatus.IN_PROGRESS)
            .blockedAt(NOW)
            .build());

        assertThat(firstResponse().canEnter()).isFalse();
        assertThat(firstResponse().entryMessage()).contains("force-ended");
    }

    @Test
    void should_block_a_student_who_was_not_marked_present() {
        givenRows(row().candidateStatus(AssessmentCandidateStatus.ASSIGNED).assessmentStatus(AssessmentStatus.IN_PROGRESS).build());

        assertThat(firstResponse().entryMessage()).contains("marked present");
    }

    @Test
    void should_block_when_the_assessment_is_not_running_yet() {
        givenRows(row().candidateStatus(AssessmentCandidateStatus.ATTENDED).assessmentStatus(AssessmentStatus.SCHEDULED).build());

        assertThat(firstResponse().entryMessage()).contains("not open for entry yet");
    }

    @Test
    void should_block_when_the_student_has_no_assigned_paper() {
        givenRows(row()
            .candidateStatus(AssessmentCandidateStatus.ATTENDED)
            .assessmentStatus(AssessmentStatus.IN_PROGRESS)
            .assignedPaperId(null)
            .build());

        assertThat(firstResponse().entryMessage()).contains("have not been assigned an assessment paper");
    }

    /**
     * An unfinished session (IN_PROGRESS/INTERRUPTED) is one that can be re-entered, not an attempt
     * already used up. Counting it wrongly would tell the student "no attempts left" on the list
     * screen, before they get to re-enter the unfinished session.
     */
    @Test
    void should_not_count_an_unfinished_session_as_a_used_attempt() {
        UUID candidateId = UUID.randomUUID();
        givenRows(row().candidateId(candidateId).candidateStatus(AssessmentCandidateStatus.ATTENDED)
            .assessmentStatus(AssessmentStatus.IN_PROGRESS).maxAttempt(1).build());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection()))
            .thenReturn(List.of(attempt(candidateId, AssessmentSessionStatus.IN_PROGRESS)));

        StudentAssessmentSummaryResponse response = firstResponse();

        assertThat(response.attemptsUsed()).isZero();
        assertThat(response.canEnter()).isTrue();
    }

    @Test
    void should_block_once_every_attempt_is_used_up() {
        UUID candidateId = UUID.randomUUID();
        givenRows(row().candidateId(candidateId).candidateStatus(AssessmentCandidateStatus.ATTENDED)
            .assessmentStatus(AssessmentStatus.IN_PROGRESS).maxAttempt(1).build());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection()))
            .thenReturn(List.of(attempt(candidateId, AssessmentSessionStatus.SUBMITTED)));

        StudentAssessmentSummaryResponse response = firstResponse();

        assertThat(response.attemptsUsed()).isEqualTo(1);
        assertThat(response.entryMessage()).contains("used up your attempts");
    }

    /** Without a paper the duration is derived from the length of the assessment schedule. */
    @Test
    void should_derive_the_duration_from_the_schedule_window() {
        givenRows(row().scheduleStartDate(NOW).scheduleEndDate(NOW.plusSeconds(2700)).build());

        assertThat(firstResponse().duration()).isEqualTo(45);
    }

    @Test
    void should_expose_the_kind_as_a_readable_subject() {
        givenRows(row().assessmentKind("CLASS_TEST").build());

        assertThat(firstResponse().subject()).isEqualTo("CLASS TEST");
    }

    // ---- fixtures ----------------------------------------------------------

    private void givenRows(StudentAssessmentRowInfo... rows) {
        givenRows(new PageResult<>(List.of(rows), 1, 20, rows.length, 1));
    }

    private void givenRows(PageResult<StudentAssessmentRowInfo> page) {
        when(studentAssessmentQueryRepository.findMyAssessments(
            any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any())).thenReturn(page);
    }

    private com.sep.vox.application.response.input.assessment.StudentAssessmentSummaryResponse firstResponse() {
        return useCase.execute(new ViewMyAssessmentsQuery(null, null, 1, 20, true)).content().get(0);
    }

    private static AssessmentAttemptSummary attempt(UUID candidateId, AssessmentSessionStatus status) {
        return new AssessmentAttemptSummary(
            candidateId, UUID.randomUUID(), AssessmentCandidateStatus.ATTENDED, UUID.randomUUID(),
            NOW, null, status, false, null, BigDecimal.ZERO,
            null, null, null, null, null, AssessmentCandidateResultStatus.PENDING_REVIEW, null);
    }

    private static RowBuilder row() {
        return new RowBuilder();
    }

    /** The "normal" row: marked present, assessment open, assessment schedule in progress, paper assigned. */
    private static final class RowBuilder {
        private UUID candidateId = UUID.randomUUID();
        private AssessmentCandidateStatus candidateStatus = AssessmentCandidateStatus.ATTENDED;
        private Instant blockedAt;
        private UUID assignedPaperId = UUID.randomUUID();
        private String assessmentKind = "CENTRALIZED";
        private AssessmentStatus assessmentStatus = AssessmentStatus.IN_PROGRESS;
        private Integer maxAttempt = 3;
        private Instant scheduleStartDate = NOW.minusSeconds(600);
        private Instant scheduleEndDate = NOW.plusSeconds(600);

        RowBuilder candidateId(UUID value) {
            this.candidateId = value;
            return this;
        }

        RowBuilder candidateStatus(AssessmentCandidateStatus value) {
            this.candidateStatus = value;
            return this;
        }

        RowBuilder blockedAt(Instant value) {
            this.blockedAt = value;
            return this;
        }

        RowBuilder assignedPaperId(UUID value) {
            this.assignedPaperId = value;
            return this;
        }

        RowBuilder assessmentKind(String value) {
            this.assessmentKind = value;
            return this;
        }

        RowBuilder assessmentStatus(AssessmentStatus value) {
            this.assessmentStatus = value;
            return this;
        }

        RowBuilder maxAttempt(Integer value) {
            this.maxAttempt = value;
            return this;
        }

        RowBuilder scheduleStartDate(Instant value) {
            this.scheduleStartDate = value;
            return this;
        }

        RowBuilder scheduleEndDate(Instant value) {
            this.scheduleEndDate = value;
            return this;
        }

        StudentAssessmentRowInfo build() {
            return new StudentAssessmentRowInfo(
                candidateId, candidateStatus, blockedAt, assignedPaperId,
                UUID.randomUUID(), "Kỳ thi", "Mô tả", assessmentKind, assessmentStatus, true, maxAttempt,
                UUID.randomUUID(), scheduleStartDate, scheduleEndDate,
                scheduleStartDate, "in_progress");
        }
    }
}
