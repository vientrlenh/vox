package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.service.TopicOfferBackfillService;
import com.sep.vox.application.port.input.usecase.learnerprofile.SetPracticeGoalUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitInterestQuizUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewInterestQuizItemsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewLearnerProfileUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewPracticeBandOptionsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewPracticeFrameworkOptionsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends the learner profile query through the real schema and asserts that {@code quizCompletedAt} is served as the
 * {@code DateTime} scalar and may be null.
 */
@GraphQlControllerTest(PracticeController.class)
class PracticeLearnerProfileGraphQlTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewLearnerProfileUseCase viewLearnerProfileUseCase;
    @MockitoBean
    private ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase;
    @MockitoBean
    private ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase;
    @MockitoBean
    private ViewPracticeFrameworkOptionsUseCase viewPracticeFrameworkOptionsUseCase;
    @MockitoBean
    private SubmitInterestQuizUseCase submitInterestQuizUseCase;
    @MockitoBean
    private SetPracticeGoalUseCase setPracticeGoalUseCase;
    @MockitoBean
    private TopicOfferBackfillService topicOfferBackfillService;
    @MockitoBean
    private UserContextPort userContextPort;
    @MockitoBean(name = "practiceGenerationExecutor")
    private AsyncTaskExecutor practiceGenerationExecutor;

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_learner_profile_should_return_quiz_completed_at_as_date_time() {
        when(viewLearnerProfileUseCase.execute(null)).thenReturn(
            new LearnerProfile("ABILITY_IMPROVEMENT", "B1", "B1", true, Instant.parse("2026-07-28T02:30:00Z")));

        graphQlTester.document("{ myLearnerProfile { goalType quizCompletedAt } }")
            .execute()
            .errors().verify()
            .path("myLearnerProfile.goalType").entity(String.class).isEqualTo("ABILITY_IMPROVEMENT")
            .path("myLearnerProfile.quizCompletedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_learner_profile_should_return_null_when_the_quiz_is_not_completed() {
        when(viewLearnerProfileUseCase.execute(null)).thenReturn(
            new LearnerProfile("ABILITY_IMPROVEMENT", null, null, true, null));

        graphQlTester.document("{ myLearnerProfile { quizCompletedAt } }")
            .execute()
            .errors().verify()
            .path("myLearnerProfile.quizCompletedAt").valueIsNull();
    }
}
