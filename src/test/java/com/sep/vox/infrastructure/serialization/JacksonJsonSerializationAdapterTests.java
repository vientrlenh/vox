package com.sep.vox.infrastructure.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class JacksonJsonSerializationAdapterTests {

    private JacksonJsonSerializationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JacksonJsonSerializationAdapter(new ObjectMapper());
    }

    @Test
    void toJson_should_serialize_map_and_list_values() {
        var json = adapter.toJson(Map.of("headers", List.of("code", "name")));

        assertThat(json).contains("headers");
        assertThat(json).contains("code");
        assertThat(json).contains("name");
    }

    @Test
    void toStringMap_should_parse_values_as_strings_and_preserve_nulls() {
        var result = adapter.toStringMap("{\"code\":\"A01\",\"count\":3,\"empty\":null}");

        assertThat(result).containsEntry("code", "A01");
        assertThat(result).containsEntry("count", "3");
        assertThat(result).containsEntry("empty", null);
    }

    @Test
    void toStringList_should_parse_values_as_strings_and_preserve_nulls() {
        var result = adapter.toStringList("[\"code\",3,null]");

        assertThat(result).containsExactly("code", "3", null);
    }

    @Test
    void toStringMapList_should_parse_map_entries_as_strings() {
        var result = adapter.toStringMapList("[{\"field\":\"code\",\"line\":1}]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("field", "code");
        assertThat(result.get(0)).containsEntry("line", "1");
    }

    @Test
    void parse_methods_should_return_empty_collections_for_null_or_blank_json() {
        assertThat(adapter.toStringMap(null)).isEmpty();
        assertThat(adapter.toStringMap(" ")).isEmpty();
        assertThat(adapter.toStringList(null)).isEmpty();
        assertThat(adapter.toStringList(" ")).isEmpty();
        assertThat(adapter.toStringMapList(null)).isEmpty();
        assertThat(adapter.toStringMapList(" ")).isEmpty();
    }

    @Test
    void parse_methods_should_throw_when_json_is_invalid() {
        assertThrows(IllegalStateException.class, () -> adapter.toStringMap("{"));
        assertThrows(IllegalStateException.class, () -> adapter.toStringList("{"));
        assertThrows(IllegalStateException.class, () -> adapter.toStringMapList("{"));
    }
}
