package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.interestdimension.UpsertInterestDimensionCommand;
import com.sep.vox.application.port.input.usecase.interestdimension.ManageInterestDimensionUseCase;
import com.sep.vox.application.response.input.interestdimension.InterestDimensionResponses.InterestDimensionResponse;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends the interest dimension catalog query through the real schema and asserts that the audit timestamps are
 * served as the {@code DateTime} scalar.
 */
@GraphQLControllerTest(GraphQLInterestDimensionController.class)
class InterestDimensionGraphQLTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ManageInterestDimensionUseCase manageInterestDimensionUseCase;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void interest_dimensions_should_return_created_and_updated_at_as_date_time() {
        when(manageInterestDimensionUseCase.findAll()).thenReturn(List.of(
            new InterestDimensionResponse("MUSIC", "Music", null, true, true, 1,
                Instant.parse("2026-07-28T02:30:00Z"), null)));

        graphQlTester.document("{ interestDimensions { code createdAt updatedAt } }")
            .execute()
            .errors().verify()
            .path("interestDimensions[0].code").entity(String.class).isEqualTo("MUSIC")
            .path("interestDimensions[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("interestDimensions[0].updatedAt").valueIsNull();
    }

    private void sendUpdate(String inputFields) {
        graphQlTester.document("mutation { updateInterestDimension(input: { code: \"MUSIC\"%s }) { code } }".formatted(inputFields))
            .execute().errors().verify();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_should_distinguish_omitted_null_and_value_for_the_description() {
        when(manageInterestDimensionUseCase.update(any())).thenReturn(
            new InterestDimensionResponse("MUSIC", "Music", null, true, true, 1, null, null));

        sendUpdate("");
        sendUpdate(", description: null");
        sendUpdate(", description: \"Fresh\"");

        verify(manageInterestDimensionUseCase).update(new UpsertInterestDimensionCommand("MUSIC", null, null, false, null, null, null));
        verify(manageInterestDimensionUseCase).update(new UpsertInterestDimensionCommand("MUSIC", null, null, true, null, null, null));
        verify(manageInterestDimensionUseCase).update(new UpsertInterestDimensionCommand("MUSIC", null, "Fresh", true, null, null, null));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void create_should_deliver_the_same_command_shape() {
        when(manageInterestDimensionUseCase.create(any())).thenReturn(
            new InterestDimensionResponse("MUSIC", "Music", null, true, true, 1, null, null));

        graphQlTester.document("mutation { createInterestDimension(input: { code: \"MUSIC\", label: \"Music\" }) { code } }")
            .execute().errors().verify();

        verify(manageInterestDimensionUseCase).create(new UpsertInterestDimensionCommand("MUSIC", "Music", null, false, null, null, null));
    }
}
