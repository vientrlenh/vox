package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.output.ColumnDropdown;
import com.sep.vox.application.response.output.SpreadsheetCell;
import com.sep.vox.application.response.output.SpreadsheetDocument;
import com.sep.vox.application.response.output.SpreadsheetOptions;

/** Reads the produced .xlsx back with POI: the bytes of a zip say nothing about what the user sees. */
class PoiSpreadsheetWriterServiceTests {

    private static final SpreadsheetOptions GUARDED_OPTIONS = new SpreadsheetOptions(true, true, true, 60, true);

    private final PoiSpreadsheetWriterService writer = new PoiSpreadsheetWriterService();

    private Sheet write(SpreadsheetOptions options, List<ColumnDropdown> dropdowns, List<List<SpreadsheetCell>> rows) {
        SpreadsheetDocument document = new SpreadsheetDocument("Sheet name", List.of("First", "Second"), rows, dropdowns, options);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(writer.write(document)))) {
            return workbook.getSheetAt(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the produced workbook back", exception);
        }
    }

    private Sheet writeOneRow(SpreadsheetOptions options, SpreadsheetCell first, SpreadsheetCell second) {
        return write(options, List.of(), List.of(List.of(first, second)));
    }

    @Test
    void should_write_the_sheet_name_and_the_header_texts_in_order() {
        Sheet sheet = write(SpreadsheetOptions.plain(), List.of(), List.of());

        assertThat(sheet.getSheetName()).isEqualTo("Sheet name");
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("First");
        assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Second");
    }

    @Test
    void should_write_bold_grey_headers_only_when_emphasized() {
        Cell emphasized = write(GUARDED_OPTIONS, List.of(), List.of()).getRow(0).getCell(0);
        Cell plain = write(SpreadsheetOptions.plain(), List.of(), List.of()).getRow(0).getCell(0);

        assertThat(emphasized.getCellStyle().getFillPattern().name()).isEqualTo("SOLID_FOREGROUND");
        assertThat(((org.apache.poi.xssf.usermodel.XSSFCellStyle) emphasized.getCellStyle()).getFont().getBold()).isTrue();
        assertThat(plain.getCellStyle().getFillPattern().name()).isEqualTo("NO_FILL");
    }

    @Test
    void should_write_a_number_as_a_numeric_cell() {
        Sheet sheet = writeOneRow(SpreadsheetOptions.plain(),
            SpreadsheetCell.number(new BigDecimal("7.50")), SpreadsheetCell.text("x"));

        assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(7.5);
    }

    @Test
    void should_write_a_date_time_as_a_date_cell_with_the_display_pattern() {
        Sheet sheet = writeOneRow(SpreadsheetOptions.plain(),
            SpreadsheetCell.dateTime(LocalDateTime.of(2026, 7, 12, 8, 0), "HH:mm dd/MM/yyyy"), SpreadsheetCell.text("x"));

        Cell cell = sheet.getRow(1).getCell(0);
        assertThat(cell.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 7, 12, 8, 0));
        assertThat(cell.getCellStyle().getDataFormatString()).isEqualTo("HH:mm dd/MM/yyyy");
    }

    @Test
    void should_write_a_missing_number_and_a_missing_date_time_as_blank_cells() {
        Sheet sheet = writeOneRow(SpreadsheetOptions.plain(),
            SpreadsheetCell.number(null), SpreadsheetCell.dateTime(null, "HH:mm dd/MM/yyyy"));

        assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.BLANK);
        assertThat(sheet.getRow(1).getCell(1).getCellType()).isEqualTo(CellType.BLANK);
    }

    @Test
    void should_prefix_every_formula_trigger_when_the_guard_is_on() {
        for (String trigger : List.of("=", "+", "-", "@", "\t", "\r")) {
            Sheet sheet = writeOneRow(GUARDED_OPTIONS, SpreadsheetCell.text(trigger + "cmd"), SpreadsheetCell.blank());

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("'" + trigger + "cmd");
        }
    }

    @Test
    void should_leave_ordinary_and_empty_text_alone_when_the_guard_is_on() {
        Sheet sheet = writeOneRow(GUARDED_OPTIONS, SpreadsheetCell.text("Nguyễn Văn A"), SpreadsheetCell.text(""));

        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Nguyễn Văn A");
        assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();
    }

    @Test
    void should_not_touch_formula_like_text_when_the_guard_is_off() {
        Sheet sheet = writeOneRow(SpreadsheetOptions.plain(), SpreadsheetCell.text("=SUM(A1)"), SpreadsheetCell.blank());

        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("=SUM(A1)");
    }

    @Test
    void should_freeze_the_header_row_and_set_the_auto_filter_only_when_asked() {
        Sheet with = writeOneRow(GUARDED_OPTIONS, SpreadsheetCell.text("a"), SpreadsheetCell.text("b"));
        Sheet without = writeOneRow(SpreadsheetOptions.plain(), SpreadsheetCell.text("a"), SpreadsheetCell.text("b"));

        assertThat(with.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        assertThat(((XSSFSheet) with).getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:B2");
        assertThat(without.getPaneInformation()).isNull();
        assertThat(((XSSFSheet) without).getCTWorksheet().isSetAutoFilter()).isFalse();
    }

    @Test
    void should_cover_one_data_row_in_the_auto_filter_of_a_header_only_sheet() {
        Sheet sheet = write(GUARDED_OPTIONS, List.of(), List.of());

        assertThat(((XSSFSheet) sheet).getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:B2");
    }

    @Test
    void should_cap_the_column_width_only_when_a_cap_is_given() {
        SpreadsheetCell longText = SpreadsheetCell.text("N".repeat(300));

        Sheet capped = writeOneRow(GUARDED_OPTIONS, longText, SpreadsheetCell.text("b"));
        Sheet uncapped = writeOneRow(SpreadsheetOptions.plain(), longText, SpreadsheetCell.text("b"));

        assertThat(capped.getColumnWidth(0)).isEqualTo(60 * 256);
        assertThat(uncapped.getColumnWidth(0)).isGreaterThan(60 * 256);
    }

    @Test
    void should_add_a_dropdown_over_the_requested_rows_and_column() {
        Sheet sheet = write(SpreadsheetOptions.plain(),
            List.of(new ColumnDropdown(1, 1, 1000, List.of("ONE", "TWO"))), List.of());

        DataValidation validation = sheet.getDataValidations().getFirst();
        assertThat(sheet.getDataValidations()).hasSize(1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getFirstColumn()).isEqualTo(1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getFirstRow()).isEqualTo(1);
        assertThat(validation.getRegions().getCellRangeAddress(0).getLastRow()).isEqualTo(1000);
        assertThat(validation.getValidationConstraint().getExplicitListValues()).containsExactly("ONE", "TWO");
        assertThat(validation.getShowErrorBox()).isTrue();
    }
}
