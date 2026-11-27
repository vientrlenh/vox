package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.support.FakeJsonSerializationPort;

/**
 * The expected strings below were captured from the previous Jackson 2 implementation of this block, so the stored
 * format must stay byte for byte the same.
 */
class RubricCriterionExamplesNormalizerTests {

    private static final String INVALID_FORMAT_MESSAGE = "The JSON format of the Examples section is invalid.";
    private static final String EMPTY_MESSAGE = "The criterion examples in the scoring framework must not be blank";
    private static final String NULL_ITEM_MESSAGE =
        "The criterion examples in the scoring framework must not contain blank values";

    private final RubricCriterionExamplesNormalizer normalizer =
        new RubricCriterionExamplesNormalizer(new FakeJsonSerializationPort());

    @Test
    void normalize_should_wrap_one_example_under_values() {
        String result = normalizer.normalize(
            "[{\"transcript\":\"Hello there\",\"explanation\":\"Good\",\"expectedScore\":7.5}]");

        assertThat(result).isEqualTo(
            "{\"values\":[{\"transcript\":\"Hello there\",\"explanation\":\"Good\",\"expectedScore\":7.5}]}");
    }

    @Test
    void normalize_should_reorder_properties_and_keep_the_scale_of_scores() {
        String result = normalizer.normalize("[ {\"expectedScore\":3,\"transcript\":\"a\",\"explanation\":\"b\"},"
            + " {\"transcript\":\"c\",\"explanation\":\"d\",\"expectedScore\":8.50} ]");

        assertThat(result).isEqualTo("{\"values\":[{\"transcript\":\"a\",\"explanation\":\"b\",\"expectedScore\":3},"
            + "{\"transcript\":\"c\",\"explanation\":\"d\",\"expectedScore\":8.50}]}");
    }

    @Test
    void normalize_should_keep_non_ascii_text_and_a_zero_score() {
        String result = normalizer.normalize(
            "[{\"transcript\":\"Xin chào\",\"explanation\":\"Tốt\",\"expectedScore\":0}]");

        assertThat(result).isEqualTo(
            "{\"values\":[{\"transcript\":\"Xin chào\",\"explanation\":\"Tốt\",\"expectedScore\":0}]}");
    }

    @Test
    void normalize_should_return_null_and_blank_input_unchanged() {
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("  ")).isEqualTo("  ");
    }

    @Test
    void normalize_should_pass_the_validation_message_through_for_an_empty_list_null_json_or_null_item() {
        assertThatThrownBy(() -> normalizer.normalize("[]"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(EMPTY_MESSAGE);
        assertThatThrownBy(() -> normalizer.normalize("null"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(EMPTY_MESSAGE);
        assertThatThrownBy(() -> normalizer.normalize("[null]"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(NULL_ITEM_MESSAGE);
    }

    @Test
    void normalize_should_answer_the_generic_message_for_every_other_failure() {
        String[] invalidInputs = {
            "[{\"transcript\":\"a\",\"explanation\":\"b\",\"expectedScore\":1,\"extra\":true}]",
            "[{\"transcript\":\"\",\"explanation\":\"b\",\"expectedScore\":1}]",
            "{\"values\":[]}",
            "not json",
            "[{\"transcript\":\"a\",\"explanation\":\"b\",\"expectedScore\":\"x\"}]"
        };
        for (String invalidInput : invalidInputs) {
            assertThatThrownBy(() -> normalizer.normalize(invalidInput))
                .as(invalidInput)
                .isInstanceOf(IllegalArgumentException.class).hasMessage(INVALID_FORMAT_MESSAGE);
        }
    }
}
