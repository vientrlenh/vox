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
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsByExamUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.domain.common.PageResult;

public class ExamAppealControllerTests {

    private ViewExamAppealsUseCase viewExamAppealsUseCase;
    private ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    private ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    private ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private ViewMyAppealsUseCase viewMyAppealsUseCase;
    private ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    private ExamAppealController controller;

    @BeforeEach
    void setUp() {
        viewExamAppealsUseCase = mock(ViewExamAppealsUseCase.class);
        viewExamAppealStatsUseCase = mock(ViewExamAppealStatsUseCase.class);
        viewExamAppealDetailUseCase = mock(ViewExamAppealDetailUseCase.class);
        viewAssignableReviewersUseCase = mock(ViewAssignableReviewersUseCase.class);
        viewMyAppealsUseCase = mock(ViewMyAppealsUseCase.class);
        viewMyAppealDetailUseCase = mock(ViewMyAppealDetailUseCase.class);
        controller = new ExamAppealController(
            viewExamAppealsUseCase,
            viewExamAppealStatsUseCase,
            viewExamAppealDetailUseCase,
            viewAssignableReviewersUseCase,
            viewMyAppealsUseCase,
            viewMyAppealDetailUseCase,
            mock(ViewExamAppealsByExamUseCase.class)
        );
    }

    @Test
    void should_pass_filters_through_to_use_case() {
        when(viewExamAppealsUseCase.execute(any()))
            .thenReturn(new PageResult<>(List.of(), 1, 5, 1, 0));

        controller.appeals("PENDING", "Nam", 1, 5);

        var captor = ArgumentCaptor.forClass(SearchExamAppealsQuery.class);
        org.mockito.Mockito.verify(viewExamAppealsUseCase).execute(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("PENDING");
        assertThat(captor.getValue().keyword()).isEqualTo("Nam");
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    @Test
    void should_return_stats_from_use_case() {
        when(viewExamAppealStatsUseCase.execute()).thenReturn(new AppealStatsInfo(3, 2, 5, 1, 4));

        var stats = controller.appealStats();

        assertThat(stats.pending()).isEqualTo(3);
        assertThat(stats.processing()).isEqualTo(2);
        assertThat(stats.withdrawn()).isEqualTo(4);
    }

    @Test
    void should_pass_appeal_id_when_listing_reviewers() {
        var appealId = UUID.randomUUID();
        when(viewAssignableReviewersUseCase.execute(any())).thenReturn(List.of());

        controller.appealReviewers(appealId, "Lan");

        // appealId là bắt buộc để tính cờ xung đột lợi ích — thiếu nó thì ai cũng
        // hiện conflicted = false và admin sẽ giao nhầm người đã từng chấm bài.
        var captor = ArgumentCaptor.forClass(ViewAssignableReviewersQuery.class);
        org.mockito.Mockito.verify(viewAssignableReviewersUseCase).execute(captor.capture());
        assertThat(captor.getValue().appealId()).isEqualTo(appealId);
        assertThat(captor.getValue().keyword()).isEqualTo("Lan");
    }

    @Test
    void should_return_reviewers_with_load_and_conflict_flag() {
        var reviewerId = UUID.randomUUID();
        when(viewAssignableReviewersUseCase.execute(any()))
            .thenReturn(List.of(new AppealReviewerLiteInfo(reviewerId, "Nguyễn Thị Lan", 3, true)));

        var result = controller.appealReviewers(UUID.randomUUID(), "Lan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).load()).isEqualTo(3);
        assertThat(result.get(0).conflicted()).isTrue();
    }
}