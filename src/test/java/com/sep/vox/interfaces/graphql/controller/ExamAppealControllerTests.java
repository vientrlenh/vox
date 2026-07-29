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

import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.query.ViewMyAppealTasksQuery;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAppealTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealTasksUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyExamAppealsUseCase;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealTaskDetailInfo;
import com.sep.vox.domain.common.PageResult;

public class ExamAppealControllerTests {

    private ViewExamAppealsUseCase viewExamAppealsUseCase;
    private ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    private ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    private ViewMyAppealTasksUseCase viewMyAppealTasksUseCase;
    private ViewAppealTaskDetailUseCase viewAppealTaskDetailUseCase;
    private ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private ViewMyExamAppealsUseCase viewMyExamAppealsUseCase;
    private ViewMyExamAppealDetailUseCase viewMyExamAppealDetailUseCase;
    private ExamAppealController controller;

    @BeforeEach
    void setUp() {
        viewExamAppealsUseCase = mock(ViewExamAppealsUseCase.class);
        viewExamAppealStatsUseCase = mock(ViewExamAppealStatsUseCase.class);
        viewExamAppealDetailUseCase = mock(ViewExamAppealDetailUseCase.class);
        viewMyAppealTasksUseCase = mock(ViewMyAppealTasksUseCase.class);
        viewAppealTaskDetailUseCase = mock(ViewAppealTaskDetailUseCase.class);
        viewAssignableReviewersUseCase = mock(ViewAssignableReviewersUseCase.class);
        viewMyExamAppealsUseCase = mock(ViewMyExamAppealsUseCase.class);
        viewMyExamAppealDetailUseCase = mock(ViewMyExamAppealDetailUseCase.class);
        controller = new ExamAppealController(
            viewExamAppealsUseCase,
            viewExamAppealStatsUseCase,
            viewExamAppealDetailUseCase,
            viewMyAppealTasksUseCase,
            viewAppealTaskDetailUseCase,
            viewAssignableReviewersUseCase,
            viewMyExamAppealsUseCase,
            viewMyExamAppealDetailUseCase
        );
    }

    @Test
    void should_default_paging_to_zero_based_first_page() {
        when(viewExamAppealsUseCase.execute(any()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        controller.appeals(null, null, null, null);

        var captor = ArgumentCaptor.forClass(SearchExamAppealsQuery.class);
        org.mockito.Mockito.verify(viewExamAppealsUseCase).execute(captor.capture());
        assertThat(captor.getValue().page()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }

    @Test
    void should_pass_filters_through_to_use_case() {
        when(viewExamAppealsUseCase.execute(any()))
            .thenReturn(new PageResult<>(List.of(), 1, 5, 0, 0));

        controller.appeals("PENDING", "Nam", 1, 5);

        var captor = ArgumentCaptor.forClass(SearchExamAppealsQuery.class);
        org.mockito.Mockito.verify(viewExamAppealsUseCase).execute(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("PENDING");
        assertThat(captor.getValue().keyword()).isEqualTo("Nam");
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    @Test
    void should_return_stats_from_use_case() {
        when(viewExamAppealStatsUseCase.execute()).thenReturn(new AppealStatsInfo(3, 2, 5, 1));

        var stats = controller.appealStats();

        assertThat(stats.pending()).isEqualTo(3);
        assertThat(stats.processing()).isEqualTo(2);
    }

    @Test
    void should_resolve_my_tasks_for_the_authenticated_reviewer() {
        when(viewMyAppealTasksUseCase.execute(any()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        controller.myAppealTasks("ASSIGNED", 0, 20);

        var captor = ArgumentCaptor.forClass(ViewMyAppealTasksQuery.class);
        org.mockito.Mockito.verify(viewMyAppealTasksUseCase).execute(captor.capture());
        // reviewerId lấy từ token trong use case, không phải tham số GraphQL.
        assertThat(captor.getValue().status()).isEqualTo("ASSIGNED");
    }

    @Test
    void should_not_expose_other_reviewers_reports_in_task_detail() {
        var appealId = UUID.randomUUID();
        var detail = new AppealTaskDetailInfo(appealId, List.of(), List.of(), List.of());
        when(viewAppealTaskDetailUseCase.execute(appealId)).thenReturn(detail);

        var result = controller.appealTaskDetail(appealId);

        // AppealTaskDetailInfo cố tình không có trường reviewers[]: chấm mù.
        assertThat(result.appealId()).isEqualTo(appealId);
        assertThat(result.myReport()).isEmpty();
    }

    @Test
    void should_return_reviewers_with_current_load() {
        var reviewerId = UUID.randomUUID();
        when(viewAssignableReviewersUseCase.execute("Lan"))
            .thenReturn(List.of(new AppealReviewerLiteInfo(reviewerId, "Nguyễn Thị Lan", 3)));

        var result = controller.appealReviewers("Lan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).load()).isEqualTo(3);
    }
}
