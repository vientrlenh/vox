package com.sep.vox.application.port.input.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

/**
 * Validates the examples JSON of a rubric criterion and rewrites it in the stored shape {@code {"values": [...]}}.
 */
@Service
@RequiredArgsConstructor
public class RubricCriterionExamplesNormalizer {

    private static final String INVALID_FORMAT_MESSAGE = "The JSON format of the Examples section is invalid.";

    private final JsonSerializationPort jsonSerializationPort;

    /**
     * Returns {@code examplesJson} unchanged when it is null or blank.
     *
     * @throws IllegalArgumentException with the validation message of {@link RubricCriterionExamples} when the list is
     *     empty or holds a null; with a generic format message for any other malformed input
     */
    public String normalize(String examplesJson) {
        if (examplesJson == null || examplesJson.isBlank()) {
            return examplesJson;
        }
        try {
            List<RubricCriterionExample> parsedExamples =
                jsonSerializationPort.fromJsonList(examplesJson, RubricCriterionExample.class);
            return jsonSerializationPort.toJson(new RubricCriterionExamples(parsedExamples));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE);
        }
    }
}
