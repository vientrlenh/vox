package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
}
