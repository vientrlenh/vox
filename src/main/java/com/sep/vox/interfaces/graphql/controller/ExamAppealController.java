package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.query.ViewMyAppealTasksQuery;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAppealTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealTasksUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.dto.AppealTaskDetailInfo;
import com.sep.vox.application.query.dto.AppealTaskInfo;
import com.sep.vox.domain.common.PageResult;

@Controller("graphqlExamAppealController")
public class ExamAppealController {

    private final ViewExamAppealsUseCase viewExamAppealsUseCase;
    private final ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    private final ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    private final ViewMyAppealTasksUseCase viewMyAppealTasksUseCase;
    private final ViewAppealTaskDetailUseCase viewAppealTaskDetailUseCase;
    private final ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;

    public ExamAppealController(
            ViewExamAppealsUseCase viewExamAppealsUseCase,
            ViewExamAppealStatsUseCase viewExamAppealStatsUseCase,
            ViewExamAppealDetailUseCase viewExamAppealDetailUseCase,
            ViewMyAppealTasksUseCase viewMyAppealTasksUseCase,
            ViewAppealTaskDetailUseCase viewAppealTaskDetailUseCase,
            ViewAssignableReviewersUseCase viewAssignableReviewersUseCase) {
        this.viewExamAppealsUseCase = viewExamAppealsUseCase;
        this.viewExamAppealStatsUseCase = viewExamAppealStatsUseCase;
        this.viewExamAppealDetailUseCase = viewExamAppealDetailUseCase;
        this.viewMyAppealTasksUseCase = viewMyAppealTasksUseCase;
        this.viewAppealTaskDetailUseCase = viewAppealTaskDetailUseCase;
        this.viewAssignableReviewersUseCase = viewAssignableReviewersUseCase;
    }

    @QueryMapping(name = "appeals")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AppealSummaryInfo> appeals(
            @Argument("status") String status,
            @Argument("keyword") String keyword,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return viewExamAppealsUseCase.execute(new SearchExamAppealsQuery(
            status, keyword, page == null ? 0 : page, size == null ? 20 : size));
    }

    @QueryMapping(name = "appealStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AppealStatsInfo appealStats() {
        return viewExamAppealStatsUseCase.execute();
    }

    @QueryMapping(name = "appeal")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AppealDetailInfo appeal(@Argument("id") UUID id) {
        return viewExamAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "myAppealTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<AppealTaskInfo> myAppealTasks(
            @Argument("status") String status,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return viewMyAppealTasksUseCase.execute(new ViewMyAppealTasksQuery(
            status, page == null ? 0 : page, size == null ? 20 : size));
    }

    @QueryMapping(name = "appealTaskDetail")
    @PreAuthorize("hasRole('TEACHER')")
    public AppealTaskDetailInfo appealTaskDetail(@Argument("appealId") UUID appealId) {
        return viewAppealTaskDetailUseCase.execute(appealId);
    }

    @QueryMapping(name = "appealReviewers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<AppealReviewerLiteInfo> appealReviewers(@Argument("keyword") String keyword) {
        return viewAssignableReviewersUseCase.execute(keyword);
    }
}
