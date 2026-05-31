package com.sep.vox.infrastructure.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class JsonImportParserTests {

    @Test
    void parse_should_return_rows_from_json_array() {
        var json = "[{\"email\":\"student@school.edu.vn\",\"phone\":\"0987654321\"},{\"email\":\"teacher@school.edu.vn\"}]";
        var parser = new JsonImportParser(new ObjectMapper());

        var rows = parser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(1);
        assertThat(rows.get(0).jsonValues().get("email")).isEqualTo("student@school.edu.vn");
        assertThat(rows.get(1).rowNumber()).isEqualTo(2);
        assertThat(rows.get(1).jsonValues().get("email")).isEqualTo("teacher@school.edu.vn");
    }
}
