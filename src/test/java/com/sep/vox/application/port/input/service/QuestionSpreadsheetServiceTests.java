package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.infrastructure.service.PoiSpreadsheetWriterService;

/**
 * Pins the workbook that {@link QuestionSpreadsheetService} writes today, before the POI code moves behind a port.
 * The file is read back with POI because an .xlsx is a zip: comparing bytes says nothing about what the user sees.
 */
@ExtendWith(MockitoExtension.class)
class QuestionSpreadsheetServiceTests {

    private static final List<String> IMPORT_HEADERS = List.of(
        "Mã câu hỏi", "Loại câu hỏi", "Nội dung câu hỏi", "Hướng dẫn", "Gợi ý", "Văn bản chuẩn bị",
        "Thời gian chuẩn bị", "Thời gian trả lời tối thiểu", "Thời gian trả lời tối đa", "Chia sẻ",
        "Nội dung mong đợi", "Ý chính", "Câu trả lời chấp nhận", "Ví dụ lạc đề", "Gợi ý chấm điểm",
        "Lỗi thường gặp");

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private QuestionTopicRepository questionTopicRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;
    private QuestionSpreadsheetService service;

    private final UUID callerId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID bankId = UUID.randomUUID();
    private final UUID topicId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestionSpreadsheetService(
            questionRepository, questionEvaluationGuideRepository, questionBankRepository, questionTopicRepository,
            userRepository, userContextPort, schoolUserRepository, userRoleQueryRepository,
            new PoiSpreadsheetWriterService());
    }

    private Sheet sheetOf(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            return workbook.getSheetAt(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the produced workbook back", exception);
        }
    }

    private List<String> cellTexts(Row row) {
        return IntStream.range(0, row.getLastCellNum())
            .mapToObj(index -> row.getCell(index).getStringCellValue())
            .toList();
    }

    private Sheet exportedSheet() {
        return sheetOf(service.exportQuestions(null, null, null, null, null, null, null, null));
    }

    private void givenSystemAdminCaller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(
            SchoolUser.builder().userId(callerId).schoolId(schoolId).build()));
    }

    private Question question(String questionText) {
        return Question.builder()
            .id(questionId).questionBankId(bankId).questionTopicId(topicId).code("Q-1")
            .questionText(questionText).instructionText("Instruction").promptText("Prompt")
            .preparationText("Preparation").type(QuestionType.READ_ALOUD)
            .preparationTimeSeconds(15).minResponseSeconds(30).maxResponseSeconds(60)
            .sharing(QuestionSharing.PRIVATE).status(QuestionStatus.DRAFT)
            .confidentiality(QuestionConfidentiality.OPEN)
            .createdAt(Instant.parse("2026-07-12T01:00:00Z")).createdBy(callerId.toString()).build();
    }

    private void givenOneAccessibleQuestion(String questionText) {
        givenSystemAdminCaller();
        when(questionRepository.findAccessible(any(), any(), eq(true), eq(false), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), eq(1), anyInt()))
            .thenReturn(new PageResult<>(List.of(question(questionText)), 1, 500, 1, 1));
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(
            QuestionBank.builder().code("BANK-1").build()));
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(
            QuestionTopic.builder().name("Weekend").build()));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(
            User.builder().fullName(new FullName("Nguyen Van A")).build()));
    }

    @Test
    void should_write_the_sixteen_template_headers_in_order() {
        Sheet sheet = sheetOf(service.downloadTemplate(null));

        assertThat(sheet.getSheetName()).isEqualTo("Questions");
        assertThat(cellTexts(sheet.getRow(0))).containsExactlyElementsOf(IMPORT_HEADERS);
    }

    @Test
    void should_write_three_sample_rows_with_the_requested_type_on_the_first() {
        Sheet sheet = sheetOf(service.downloadTemplate(QuestionType.OPINION));

        assertThat(sheet.getLastRowNum()).isEqualTo(3);
        assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("OPINION");
        assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("SHORT_ANSWER");
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Q-SAMPLE-001");
    }

    @Test
    void should_default_the_first_sample_row_to_read_aloud() {
        Sheet sheet = sheetOf(service.downloadTemplate(null));

        assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("READ_ALOUD");
    }

    @Test
    void should_write_every_template_cell_as_text_and_empty_values_as_empty_text() {
        Sheet sheet = sheetOf(service.downloadTemplate(null));

        assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEmpty();
        assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("15");
    }

    @Test
    void should_limit_the_type_column_to_the_question_types_over_rows_one_to_one_thousand() {
        Sheet sheet = sheetOf(service.downloadTemplate(null));

        DataValidation validation = validationOnColumn(sheet, 1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getFirstRow()).isEqualTo(1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getLastRow()).isEqualTo(1000);
        assertThat(validation.getValidationConstraint().getExplicitListValues())
            .containsExactly(Arrays.stream(QuestionType.values()).map(e -> e.name()).toArray(String[]::new));
        assertThat(validation.getShowErrorBox()).isTrue();
    }

    @Test
    void should_limit_the_sharing_column_to_the_sharing_values_over_rows_one_to_one_thousand() {
        Sheet sheet = sheetOf(service.downloadTemplate(null));

        DataValidation validation = validationOnColumn(sheet, 9);
        assertThat(validation.getRegions().getCellRangeAddress(0).getFirstRow()).isEqualTo(1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getLastRow()).isEqualTo(1000);
        assertThat(validation.getValidationConstraint().getExplicitListValues())
            .containsExactly(Arrays.stream(QuestionSharing.values()).map(e -> e.name()).toArray(String[]::new));
        assertThat(sheet.getDataValidations()).hasSize(2);
    }

    private DataValidation validationOnColumn(Sheet sheet, int column) {
        return sheet.getDataValidations().stream()
            .filter(validation -> validation.getRegions().getCellRangeAddress(0).getFirstColumn() == column)
            .findFirst()
            .orElseThrow();
    }

    @Test
    void should_export_the_import_headers_plus_five_extra_columns() {
        givenOneAccessibleQuestion("Read this.");
        when(questionEvaluationGuideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        Sheet sheet = exportedSheet();

        assertThat(cellTexts(sheet.getRow(0))).containsExactlyElementsOf(
            Stream.concat(IMPORT_HEADERS.stream(),
                Stream.of("Mã ngân hàng", "Chủ đề", "Trạng thái", "Người tạo", "Ngày tạo")).toList());
    }

    @Test
    void should_export_one_row_per_question_with_guide_bank_topic_and_author() {
        givenOneAccessibleQuestion("Read this.");
        when(questionEvaluationGuideRepository.findByQuestionId(questionId)).thenReturn(Optional.of(
            QuestionEvaluationGuide.builder().expectedContent("Expected").keyPoints("Points").build()));

        Sheet sheet = exportedSheet();

        assertThat(cellTexts(sheet.getRow(1))).containsExactly(
            "Q-1", "READ_ALOUD", "Read this.", "Instruction", "Prompt", "Preparation", "15", "30", "60", "PRIVATE",
            "Expected", "Points", "", "", "", "",
            "BANK-1", "Weekend", "DRAFT", "Nguyen Van A", "2026-07-12T01:00:00Z");
    }

    @Test
    void should_write_empty_guide_columns_when_the_question_has_no_guide() {
        givenOneAccessibleQuestion("Read this.");
        when(questionEvaluationGuideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        Sheet sheet = exportedSheet();

        assertThat(cellTexts(sheet.getRow(1)).subList(10, 16)).containsOnly("");
    }

    @Test
    void should_not_apply_a_formula_guard_to_exported_text_today() {
        givenOneAccessibleQuestion("=HYPERLINK(\"http://evil\")");
        when(questionEvaluationGuideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        Sheet sheet = exportedSheet();

        assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("=HYPERLINK(\"http://evil\")");
    }

    @Test
    void should_name_the_files() {
        assertThat(service.templateFileName()).isEqualTo("question-import-template.xlsx");
        assertThat(service.exportFileName()).matches("questions-export-\\d{14}\\.xlsx");
    }
}
