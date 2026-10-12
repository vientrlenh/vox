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

import com.sep.vox.application.port.input.query.SearchAppealsByExamQuery;
import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsByExamUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewMyAppealsUseCase;
import com.sep.vox.application.projection.dto.AppealDetailInfo;
import com.sep.vox.application.projection.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.projection.dto.AppealStatsInfo;
import com.sep.vox.application.projection.dto.AppealSummaryInfo;
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
public class GraphQLExamAppealController {

    private final ViewExamAppealsUseCase viewExamAppealsUseCase;
    private final ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    private final ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    private final ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;
    private final ViewMyAppealsUseCase viewMyAppealsUseCase;
    private final ViewMyAppealDetailUseCase viewMyAppealDetailUseCase;
    private final ViewExamAppealsByExamUseCase viewExamAppealsByExamUseCase;


    @QueryMapping(name = "appeals")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AppealSummaryInfo> appeals(
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewExamAppealsUseCase.execute(new SearchExamAppealsQuery(
            status, keyword, page, size));
    }

    @QueryMapping(name = "examAppeals")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<AppealSummaryInfo> examAppeals(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewExamAppealsByExamUseCase.execute(new SearchAppealsByExamQuery(
            examId, status, keyword, page, size));
    }

    @QueryMapping(name = "appealStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AppealStatsInfo appealStats() {
        return viewExamAppealStatsUseCase.execute();
    }

    @QueryMapping(name = "appeal")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public AppealDetailInfo appeal(@Argument(name = "id") UUID id) {
        return viewExamAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "myAppeals")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResult<AppealSummaryInfo> myAppeals(
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewMyAppealsUseCase.execute(new SearchExamAppealsQuery(
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

    @SchemaMapping(typeName = "AppealReviewerLite", field = "user")
    public CompletableFuture<UserDto> appealReviewerUser(
            AppealReviewerLiteInfo source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.id());
    }
}