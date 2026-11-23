package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.assessmentappeal.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.query.assessmentappeal.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsByAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;

public class GraphQLAssessmentAppealControllerTests {

    private ViewAssessmentAppealsUseCase viewAssessmentAppealsUseCase;
    private ViewAssessmentAppealStatsUseCase viewAssessmentAppealStatsUseCase;
    private ViewAssessmentAppealDetailUseCase viewAssessmentAppealDetailUseCase;
    private ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private ViewMyAppealsUseCase viewMyAppealsUseCase;
    private ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    private GraphQLAssessmentAppealController controller;

    @BeforeEach
    void setUp() {
        viewAssessmentAppealsUseCase = mock(ViewAssessmentAppealsUseCase.class);
        viewAssessmentAppealStatsUseCase = mock(ViewAssessmentAppealStatsUseCase.class);
        viewAssessmentAppealDetailUseCase = mock(ViewAssessmentAppealDetailUseCase.class);
        viewAssignableReviewersUseCase = mock(ViewAssignableReviewersUseCase.class);
        viewMyAppealsUseCase = mock(ViewMyAppealsUseCase.class);
        viewMyAppealDetailUseCase = mock(ViewMyAppealDetailUseCase.class);
        controller = new GraphQLAssessmentAppealController(
            viewAssessmentAppealsUseCase,
            viewAssessmentAppealStatsUseCase,
            viewAssessmentAppealDetailUseCase,
            viewAssignableReviewersUseCase,
            viewMyAppealsUseCase,
            viewMyAppealDetailUseCase,
            mock(ViewAssessmentAppealsByAssessmentUseCase.class)
        );
    }

    @Test
    void should_pass_filters_through_to_use_case() {
        when(viewAssessmentAppealsUseCase.execute(any()))
            .thenReturn(new PageResult<>(List.of(), 1, 5, 1, 0));

        controller.appeals(AssessmentAppealStatus.PENDING, "Nam", 1, 5);

        ArgumentCaptor<SearchAssessmentAppealsQuery> captor = ArgumentCaptor.forClass(SearchAssessmentAppealsQuery.class);
        org.mockito.Mockito.verify(viewAssessmentAppealsUseCase).execute(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AssessmentAppealStatus.PENDING);
        assertThat(captor.getValue().keyword()).isEqualTo("Nam");
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    @Test
    void should_return_stats_from_use_case() {
        when(viewAssessmentAppealStatsUseCase.execute()).thenReturn(new AppealStatsInfo(3, 2, 5, 1, 4));

        AppealStatsInfo stats = controller.appealStats();

        assertThat(stats.pending()).isEqualTo(3);
        assertThat(stats.processing()).isEqualTo(2);
        assertThat(stats.withdrawn()).isEqualTo(4);
    }

    @Test
    void should_pass_appeal_id_when_listing_reviewers() {
        UUID appealId = UUID.randomUUID();
        when(viewAssignableReviewersUseCase.execute(any())).thenReturn(List.of());

        controller.appealReviewers(appealId, "Lan");

        // appealId is required to compute the conflict-of-interest flag -- without it everyone
        // shows conflicted = false and the admin will assign someone who has already graded the
        // submission.
        ArgumentCaptor<ViewAssignableReviewersQuery> captor = ArgumentCaptor.forClass(ViewAssignableReviewersQuery.class);
        org.mockito.Mockito.verify(viewAssignableReviewersUseCase).execute(captor.capture());
        assertThat(captor.getValue().appealId()).isEqualTo(appealId);
        assertThat(captor.getValue().keyword()).isEqualTo("Lan");
    }

    @Test
    void should_return_reviewers_with_load_and_conflict_flag() {
        UUID reviewerId = UUID.randomUUID();
        when(viewAssignableReviewersUseCase.execute(any()))
            .thenReturn(List.of(new AppealReviewerLiteInfo(reviewerId, "Nguyễn Thị Lan", 3, true)));

        List<AppealReviewerLiteInfo> result = controller.appealReviewers(UUID.randomUUID(), "Lan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).load()).isEqualTo(3);
        assertThat(result.get(0).conflicted()).isTrue();
    }
}