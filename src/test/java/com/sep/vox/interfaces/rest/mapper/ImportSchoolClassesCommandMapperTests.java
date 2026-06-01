package com.sep.vox.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImportSchoolClassesCommandMapperTests {

    @Test
    void from_file_should_parse_csv_with_headers_and_quoted_values() {
        var file = csv("""
            \uFEFFlanguageCode,schoolGradeCode,targetSchoolLevelCode,targetSchoolLevelVersion,code,name,description
            ENG,G10,A1,1,ENG_10_A,"English, 10A",Optional
            """);

        var command = ImportSchoolClassesCommandMapper.fromFile(file);

        assertThat(command.rows()).hasSize(1);
        var row = command.rows().getFirst();
        assertThat(row.rowNumber()).isEqualTo(2);
        assertThat(row.languageCode()).isEqualTo("ENG");
        assertThat(row.schoolGradeCode()).isEqualTo("G10");
        assertThat(row.targetSchoolLevelCode()).isEqualTo("A1");
        assertThat(row.targetSchoolLevelVersion()).isEqualTo("1");
        assertThat(row.code()).isEqualTo("ENG_10_A");
        assertThat(row.name()).isEqualTo("English, 10A");
        assertThat(row.description()).isEqualTo("Optional");
    }

    @Test
    void from_file_should_parse_xlsx_first_sheet() throws Exception {
        var file = xlsx();

        var command = ImportSchoolClassesCommandMapper.fromFile(file);

        assertThat(command.rows()).hasSize(1);
        var row = command.rows().getFirst();
        assertThat(row.rowNumber()).isEqualTo(2);
        assertThat(row.languageCode()).isEqualTo("ENG");
        assertThat(row.schoolGradeCode()).isEqualTo("G10");
        assertThat(row.targetSchoolLevelCode()).isEqualTo("A1");
        assertThat(row.targetSchoolLevelVersion()).isEqualTo("1");
        assertThat(row.code()).isEqualTo("ENG_10_A");
        assertThat(row.name()).isEqualTo("English 10A");
        assertThat(row.description()).isEqualTo("Optional");
    }

    @Test
    void from_file_should_skip_blank_rows() {
        var file = csv("""
            languageCode,schoolGradeCode,targetSchoolLevelCode,targetSchoolLevelVersion,code,name,description
            ENG,G10,A1,1,ENG_10_A,English 10A,Optional
            ,,,,,,
            ENG,G10,A1,1,ENG_10_B,English 10B,
            """);

        var command = ImportSchoolClassesCommandMapper.fromFile(file);

        assertThat(command.rows()).hasSize(2);
        assertThat(command.rows())
            .extracting(row -> row.code())
            .containsExactly("ENG_10_A", "ENG_10_B");
    }

    @Test
    void from_file_should_throw_when_required_header_is_missing() {
        var file = csv("""
            languageCode,schoolGradeCode,targetSchoolLevelCode,targetSchoolLevelVersion,name
            ENG,G10,A1,1,English 10A
            """);

        assertThrows(IllegalArgumentException.class, () -> ImportSchoolClassesCommandMapper.fromFile(file));
    }

    @Test
    void from_file_should_throw_when_extension_is_not_supported() {
        var file = new MockMultipartFile("file", "classes.txt", "text/plain", "test".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> ImportSchoolClassesCommandMapper.fromFile(file));
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "classes.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile xlsx() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("classes");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("languageCode");
            header.createCell(1).setCellValue("schoolGradeCode");
            header.createCell(2).setCellValue("targetSchoolLevelCode");
            header.createCell(3).setCellValue("targetSchoolLevelVersion");
            header.createCell(4).setCellValue("code");
            header.createCell(5).setCellValue("name");
            header.createCell(6).setCellValue("description");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("ENG");
            row.createCell(1).setCellValue("G10");
            row.createCell(2).setCellValue("A1");
            row.createCell(3).setCellValue(1);
            row.createCell(4).setCellValue("ENG_10_A");
            row.createCell(5).setCellValue("English 10A");
            row.createCell(6).setCellValue("Optional");

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile(
                "file",
                "classes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
            );
        }
    }
}
