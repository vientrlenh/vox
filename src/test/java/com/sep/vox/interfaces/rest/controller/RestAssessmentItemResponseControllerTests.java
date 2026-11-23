package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.usecase.assessmentitemresponse.UpdateAssessmentItemResponseTurnUseCase;
import com.sep.vox.domain.model.assessment.TurnType;
import java.time.Instant;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.support.RestControllerTest;

/**
 * Pins the JSON text that REST clients receive for the temporal and turn type fields of the
 * {@link AssessmentItemResponseTurnResponse} returned by {@link RestAssessmentItemResponseController}. The GraphQL typed-scalars
 * plan changes those field types; the JSON written here must stay the same.
 */
@RestControllerTest(RestAssessmentItemResponseController.class)
class RestAssessmentItemResponseControllerTests {

    private static final String ANSWERED_AT = "2026-08-01T01:20:00Z";
    private static final String CREATED_AT = "2026-08-01T01:21:05Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private UpdateAssessmentItemResponseTurnUseCase updateAssessmentItemResponseTurnUseCase;

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateTurn_should_keep_turn_json_text() {
        when(updateAssessmentItemResponseTurnUseCase.execute(any())).thenReturn(new AssessmentItemResponseTurnResponse(
            UUID.randomUUID(), UUID.randomUUID(), 1, TurnType.FOLLOWUP, "Prompt", null, "Transcript", 12, 3, Instant.parse(ANSWERED_AT), Instant.parse(CREATED_AT)));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-item-responses/{answerId}/turns/{turnOrder}", UUID.randomUUID(), 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnType\": \"FOLLOWUP\", \"answeredAt\": \"2026-08-01T01:20:00Z\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "turnType", "FOLLOWUP",
                "answeredAt", ANSWERED_AT,
                "createdAt", CREATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateTurn_should_answer_400_listing_turn_types_for_unknown_value() {
        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-item-responses/{answerId}/turns/{turnOrder}", UUID.randomUUID(), 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnType\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.turnType").asString()
            .contains("MAIN", "FOLLOWUP");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateTurn_should_answer_400_naming_answered_at_for_unparsable_date() {
        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-item-responses/{answerId}/turns/{turnOrder}", UUID.randomUUID(), 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"answeredAt\": \"2026-13-45\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.answeredAt").isNotNull();
    }
}
