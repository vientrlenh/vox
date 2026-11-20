package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchAppealsByAssessmentQuery;
import com.sep.vox.application.port.input.query.SearchAssessmentAppealsQuery;
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsByAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewAssessmentAppealsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.interfaces.shared.PageArguments;

import graphql.schema.DataFetchingEnvironment;

/**
 * Only the school admin's point of view remains.
 *
 * <p>{@code myAppealTasks} / {@code appealTaskDetail} have been removed: the appeal round is now
 * an assignment row like the other three rounds, so the teacher views it in {@code myGradingTasks} and
 * grades it in {@code gradingTaskDetail} — exactly the "one single queue" goal.
 */
@Controller("graphqlAssessmentAppealController")
public class AssessmentAppealController {

    private final ViewAssessmentAppealsUseCase viewAssessmentAppealsUseCase;
    private final ViewAssessmentAppealStatsUseCase viewAssessmentAppealStatsUseCase;
    private final ViewAssessmentAppealDetailUseCase viewAssessmentAppealDetailUseCase;
    private final ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private final ViewMyAppealsUseCase viewMyAppealsUseCase;
    private final ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    private final ViewAssessmentAppealsByAssessmentUseCase viewAssessmentAppealsByAssessmentUseCase;

    public AssessmentAppealController(
            ViewAssessmentAppealsUseCase viewAssessmentAppealsUseCase,
            ViewAssessmentAppealStatsUseCase viewAssessmentAppealStatsUseCase,
            ViewAssessmentAppealDetailUseCase viewAssessmentAppealDetailUseCase,
            ViewAssignableReviewersUseCase viewAssignableReviewersUseCase,
            ViewMyAppealsUseCase viewMyAppealsUseCase,
            ViewMyAppealDetailUseCase viewMyAppealDetailUseCase,
            ViewAssessmentAppealsByAssessmentUseCase viewAssessmentAppealsByAssessmentUseCase) {
        this.viewAssessmentAppealsUseCase = viewAssessmentAppealsUseCase;
        this.viewAssessmentAppealStatsUseCase = viewAssessmentAppealStatsUseCase;
        this.viewAssessmentAppealDetailUseCase = viewAssessmentAppealDetailUseCase;
        this.viewAssignableReviewersUseCase = viewAssignableReviewersUseCase;
        this.viewMyAppealsUseCase = viewMyAppealsUseCase;
        this.viewMyAppealDetailUseCase = viewMyAppealDetailUseCase;
        this.viewAssessmentAppealsByAssessmentUseCase = viewAssessmentAppealsByAssessmentUseCase;
    }

    @QueryMapping(name = "appeals")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AppealSummaryInfo> appeals(
            @Argument(name = "status") AssessmentAppealStatus status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewAssessmentAppealsUseCase.execute(new SearchAssessmentAppealsQuery(
            status, keyword, page, size));
    }

    @QueryMapping(name = "assessmentAppeals")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<AppealSummaryInfo> assessmentAppeals(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") AssessmentAppealStatus status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewAssessmentAppealsByAssessmentUseCase.execute(new SearchAppealsByAssessmentQuery(
            assessmentId, status, keyword, page, size));
    }

    @QueryMapping(name = "appealStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AppealStatsInfo appealStats() {
        return viewAssessmentAppealStatsUseCase.execute();
    }

    @QueryMapping(name = "appeal")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public AppealDetailInfo appeal(@Argument(name = "id") UUID id) {
        return viewAssessmentAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "myAppeals")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResult<AppealSummaryInfo> myAppeals(
            @Argument(name = "status") AssessmentAppealStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewMyAppealsUseCase.execute(new SearchAssessmentAppealsQuery(
            status, null, page, size));
    }

    @QueryMapping(name = "myAppeal")
    @PreAuthorize("hasRole('STUDENT')")
    public AppealDetailInfo myAppeal(@Argument(name = "id") UUID id) {
        return viewMyAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "appealReviewers")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AppealReviewerLiteInfo> appealReviewers(
            @Argument(name = "appealId") UUID appealId,
            @Argument(name = "keyword") String keyword) {
        return viewAssignableReviewersUseCase.execute(new ViewAssignableReviewersQuery(appealId, keyword));
    }

    /**
     * The grader profile for the appeal-assignment picker, loaded through the DataLoader {@code userById}.
     *
     * <p>Only attached to {@code AppealReviewerLite} -- this type only appears in
     * {@code appealReviewers}, the school admin's query when choosing a grader. NOT attached to
     * {@code AppealSummary}/{@code AppealDetail}: those two types are shared with the student's {@code myAppeals},
     * and the grader must stay anonymous to the student (see the note in assessment-appeal.graphqls).
     * A photo reveals an identity more surely than a name.
     */
    @SchemaMapping(typeName = "AppealReviewerLite", field = "user")
    public CompletableFuture<UserDto> appealReviewerUser(
            AppealReviewerLiteInfo source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.id());
    }
}