package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.practicesession.EndPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.StartPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewMyPracticeHistoryUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewMyPracticeSessionDetailUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewStudentPracticeSessionDetailUseCase;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends practice session queries through the real schema and asserts that the session timestamps are served as the
 * {@code DateTime} scalar. {@code origin} and {@code status} stay plain strings because no Java enum exists for them.
 */
@GraphQLControllerTest(GraphQLPracticeSessionController.class)
class PracticeSessionGraphQLTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private StartPracticeSessionUseCase startPracticeSessionUseCase;
    @MockitoBean
    private EndPracticeSessionUseCase endPracticeSessionUseCase;
    @MockitoBean
    private ViewMyPracticeHistoryUseCase viewMyPracticeHistoryUseCase;
    @MockitoBean
    private ViewMyPracticeSessionDetailUseCase viewMyPracticeSessionDetailUseCase;
    @MockitoBean
    private ViewStudentPracticeSessionDetailUseCase viewStudentPracticeSessionDetailUseCase;

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_practice_history_should_return_started_and_ended_at_as_date_time() {
        when(viewMyPracticeHistoryUseCase.execute(any())).thenReturn(List.of(
            new PracticeSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Travel", "BROWSE",
                "COMPLETED", null, 82.5, 120, Instant.parse("2026-07-28T02:30:00Z"),
                Instant.parse("2026-07-28T02:45:00Z"), 0),
            new PracticeSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Food", "BROWSE",
                "IN_PROGRESS", null, null, 0, Instant.parse("2026-07-29T00:00:00Z"), null, 0)));

        graphQlTester.document("{ myPracticeHistory { status startedAt endedAt } }")
            .execute()
            .errors().verify()
            .path("myPracticeHistory[0].startedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("myPracticeHistory[0].endedAt").entity(String.class).isEqualTo("2026-07-28T02:45:00Z")
            .path("myPracticeHistory[1].endedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_practice_session_detail_should_return_started_at_as_date_time() {
        when(viewMyPracticeSessionDetailUseCase.execute(any())).thenReturn(new TeacherPracticeSessionDetail(
            UUID.randomUUID(), "Travel", Instant.parse("2026-07-28T02:30:00Z"), 900, 3, 82.5, List.of(), true, 0, 0,
            null, List.of(), 0.0, 100.0));

        graphQlTester.document("{ myPracticeSessionDetail(sessionId: \"%s\") { startedAt } }"
                .formatted(UUID.randomUUID()))
            .execute()
            .errors().verify()
            .path("myPracticeSessionDetail.startedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }
}
