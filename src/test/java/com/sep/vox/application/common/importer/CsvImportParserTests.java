package com.sep.vox.application.common.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class CsvImportParserTests {

    @Test
    void parse_should_return_rows_with_headers_and_values() {
        var csv = "Email,Phone,Full Name\nstudent@school.edu.vn,0987654321,Nguyen Van A\n";
        var parser = new CsvImportParser();

        var rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.rowNumber()).isEqualTo(2);
        assertThat(row.columns().get("Email")).isEqualTo("student@school.edu.vn");
        assertThat(row.columns().get("Phone")).isEqualTo("0987654321");
        assertThat(row.columns().get("Full Name")).isEqualTo("Nguyen Van A");
        assertThat(row.values()).containsExactly("student@school.edu.vn", "0987654321", "Nguyen Van A");
    }
}
