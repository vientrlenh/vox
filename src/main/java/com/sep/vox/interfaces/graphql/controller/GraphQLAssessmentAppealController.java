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
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewAssessmentAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewAssessmentAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewAssessmentAppealsByAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewAssessmentAppealsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ViewMyAppealsUseCase;
import com.sep.vox.application.projection.dto.AppealDetailInfo;
import com.sep.vox.application.projection.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.projection.dto.AppealStatsInfo;
import com.sep.vox.application.projection.dto.AppealSummaryInfo;
import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.shared.PageResult;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;

/**
 * Chỉ còn góc nhìn của school admin.
 *
 * <p>{@code myAppealTasks} / {@code appealTaskDetail} đã bị gỡ: vòng phúc khảo giờ là
 * một dòng phân công như ba vòng kia, nên giáo viên xem ở {@code myGradingTasks} và
 * chấm ở {@code gradingTaskDetail} — đúng mục tiêu "một hàng đợi duy nhất".
 */
@Controller
@RequiredArgsConstructor 
public class GraphQLAssessmentAppealController {

    private final ViewAssessmentAppealsUseCase viewAssessmentAppealsUseCase;
    private final ViewAssessmentAppealStatsUseCase viewAssessmentAppealStatsUseCase;
    private final ViewAssessmentAppealDetailUseCase viewAssessmentAppealDetailUseCase;
    private final ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private final ViewMyAppealsUseCase viewMyAppealsUseCase;
    private final ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    private final ViewAssessmentAppealsByAssessmentUseCase viewAssessmentAppealsByAssessmentUseCase;


    @QueryMapping(name = "appeals")
    @PreAuthorize("hasRole('SCHOOL_MEMBER')")
    public PageResult<AppealSummaryInfo> appeals(
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewAssessmentAppealsUseCase.execute(new SearchAssessmentAppealsQuery(
            status, keyword, pageRequest));
    }

    @QueryMapping(name = "assessmentAppeals")
    @PreAuthorize("hasAnyRole('SCHOOL_MEMBER')")
    public PageResult<AppealSummaryInfo> assessmentAppeals(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewAssessmentAppealsByAssessmentUseCase.execute(new SearchAppealsByAssessmentQuery(
            assessmentId, status, keyword, pageRequest));
    }

    @QueryMapping(name = "appealStats")
    @PreAuthorize("hasRole('SCHOOL_MEMBER')")
    public AppealStatsInfo appealStats() {
        return viewAssessmentAppealStatsUseCase.execute();
    }

    @QueryMapping(name = "appeal")
    @PreAuthorize("hasAnyRole('SCHOOL_MEMBER')")
    public AppealDetailInfo appeal(@Argument(name = "id") UUID id) {
        return viewAssessmentAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "myAppeals")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResult<AppealSummaryInfo> myAppeals(
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewMyAppealsUseCase.execute(new SearchAssessmentAppealsQuery(
            status, null, pageRequest));
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

    @SchemaMapping(typeName = "AppealReviewerLite", field = "user")
    public CompletableFuture<UserDto> appealReviewerUser(
            AppealReviewerLiteInfo source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.id());
    }
}