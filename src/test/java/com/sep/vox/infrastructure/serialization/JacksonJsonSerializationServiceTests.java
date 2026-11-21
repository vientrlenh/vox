package com.sep.vox.infrastructure.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.infrastructure.service.JacksonJsonSerializationService;

import tools.jackson.databind.json.JsonMapper;


class JacksonJsonSerializationServiceTests {

    private record SecondsParams(int seconds) {
    }

    private JacksonJsonSerializationService service;

    @BeforeEach
    void setUp() {
        service = new JacksonJsonSerializationService(new JsonMapper());
    }

    @Test
    void toJson_should_serialize_map_and_list_values() {
        String json = service.toJson(Map.of("headers", List.of("code", "name")));

        assertThat(json).contains("headers");
        assertThat(json).contains("code");
        assertThat(json).contains("name");
    }

    @Test
    void toStringMap_should_parse_values_as_strings_and_preserve_nulls() {
        Map<String, String> result = service.toStringMap("{\"code\":\"A01\",\"count\":3,\"empty\":null}");

        assertThat(result).containsEntry("code", "A01");
        assertThat(result).containsEntry("count", "3");
        assertThat(result).containsEntry("empty", null);
    }

    @Test
    void toStringList_should_parse_values_as_strings_and_preserve_nulls() {
        List<String> result = service.toStringList("[\"code\",3,null]");

        assertThat(result).containsExactly("code", "3", null);
    }

    @Test
    void toStringMapList_should_parse_map_entries_as_strings() {
        List<Map<String, String>> result = service.toStringMapList("[{\"field\":\"code\",\"line\":1}]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("field", "code");
        assertThat(result.get(0)).containsEntry("line", "1");
    }

    @Test
    void parse_methods_should_return_empty_collections_for_null_or_blank_json() {
        assertThat(service.toStringMap(null)).isEmpty();
        assertThat(service.toStringMap(" ")).isEmpty();
        assertThat(service.toStringList(null)).isEmpty();
        assertThat(service.toStringList(" ")).isEmpty();
        assertThat(service.toStringMapList(null)).isEmpty();
        assertThat(service.toStringMapList(" ")).isEmpty();
    }

    @Test
    void parse_methods_should_throw_when_json_is_invalid() {
        assertThrows(IllegalStateException.class, () -> service.toStringMap("{"));
        assertThrows(IllegalStateException.class, () -> service.toStringList("{"));
        assertThrows(IllegalStateException.class, () -> service.toStringMapList("{"));
    }

    @Test
    void convertValue_should_bind_a_raw_map_to_a_record() {
        SecondsParams params = service.convertValue(Map.of("seconds", 3), SecondsParams.class);

        assertThat(params).isEqualTo(new SecondsParams(3));
    }

    @Test
    void convertValue_should_keep_the_binding_error_in_the_message_when_a_value_is_invalid() {
        Map<String, Object> raw = Map.of("seconds", "not a number");

        assertThatThrownBy(() -> service.convertValue(raw, SecondsParams.class))
            .hasMessageContaining("not a number");
    }

    @Test
    void convertToObjectMap_should_turn_a_record_into_a_raw_map() {
        Map<String, Object> map = service.convertToObjectMap(new SecondsParams(5));

        assertThat(map).containsExactly(Map.entry("seconds", 5));
    }

    @Test
    void toObjectMap_should_parse_a_json_object_into_a_raw_map() {
        Map<String, Object> map = service.toObjectMap("{\"seconds\": 7}");

        assertThat(map).containsExactly(Map.entry("seconds", 7));
    }

    @Test
    void toObjectMap_should_throw_when_the_json_is_not_an_object() {
        assertThrows(RuntimeException.class, () -> service.toObjectMap("[1, 2]"));
    }
}
