package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.query.ViewAssignableReviewersQuery;
import com.sep.vox.application.port.input.usecase.examappeal.ViewAssignableReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealDetailUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealStatsUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ViewExamAppealsUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Chỉ còn góc nhìn của school admin.
 *
 * <p>{@code myAppealTasks} / {@code appealTaskDetail} đã bị gỡ: vòng phúc khảo giờ là
 * một dòng phân công như ba vòng kia, nên giáo viên xem ở {@code myGradingTasks} và
 * chấm ở {@code gradingTaskDetail} — đúng mục tiêu "một hàng đợi duy nhất".
 */
@Controller("graphqlExamAppealController")
public class ExamAppealController {

    private final ViewExamAppealsUseCase viewExamAppealsUseCase;
    private final ViewExamAppealStatsUseCase viewExamAppealStatsUseCase;
    private final ViewExamAppealDetailUseCase viewExamAppealDetailUseCase;
    private final ViewAssignableReviewersUseCase viewAssignableReviewersUseCase;

    public ExamAppealController(
            ViewExamAppealsUseCase viewExamAppealsUseCase,
            ViewExamAppealStatsUseCase viewExamAppealStatsUseCase,
            ViewExamAppealDetailUseCase viewExamAppealDetailUseCase,
            ViewAssignableReviewersUseCase viewAssignableReviewersUseCase) {
        this.viewExamAppealsUseCase = viewExamAppealsUseCase;
        this.viewExamAppealStatsUseCase = viewExamAppealStatsUseCase;
        this.viewExamAppealDetailUseCase = viewExamAppealDetailUseCase;
        this.viewAssignableReviewersUseCase = viewAssignableReviewersUseCase;
    }

    @QueryMapping(name = "appeals")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<AppealSummaryInfo> appeals(
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
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
    public AppealDetailInfo appeal(@Argument(name = "id") UUID id) {
        return viewExamAppealDetailUseCase.execute(id);
    }

    @QueryMapping(name = "appealReviewers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<AppealReviewerLiteInfo> appealReviewers(
            @Argument(name = "appealId") UUID appealId,
            @Argument(name = "keyword") String keyword) {
        return viewAssignableReviewersUseCase.execute(new ViewAssignableReviewersQuery(appealId, keyword));
    }
}
