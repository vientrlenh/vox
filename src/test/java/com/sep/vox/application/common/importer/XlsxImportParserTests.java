package com.sep.vox.application.common.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

public class XlsxImportParserTests {

    @Test
    void parse_should_return_rows_with_headers_and_values() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("Email");
        header.createCell(1).setCellValue("Phone");
        header.createCell(2).setCellValue("Full Name");

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue("student@school.edu.vn");
        row.createCell(1).setCellValue("0987654321");
        row.createCell(2).setCellValue("Nguyen Van A");

        var output = new ByteArrayOutputStream();
        workbook.write(output);
        workbook.close();

        var parser = new XlsxImportParser();
        var rows = parser.parse(new ByteArrayInputStream(output.toByteArray()));

        assertThat(rows).hasSize(1);
        var parsed = rows.get(0);
        assertThat(parsed.rowNumber()).isEqualTo(2);
        assertThat(parsed.columns().get("Email")).isEqualTo("student@school.edu.vn");
        assertThat(parsed.columns().get("Phone")).isEqualTo("0987654321");
        assertThat(parsed.columns().get("Full Name")).isEqualTo("Nguyen Van A");
        assertThat(parsed.values()).containsExactly("student@school.edu.vn", "0987654321", "Nguyen Van A");
    }
}
