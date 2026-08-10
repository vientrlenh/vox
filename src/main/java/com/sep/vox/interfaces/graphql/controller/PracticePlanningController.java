package com.sep.vox.interfaces.graphql.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.command.DismissTopicOfferCommand;
import com.sep.vox.application.port.input.command.SaveTopicCommand;
import com.sep.vox.application.port.input.command.UnsaveTopicCommand;
import com.sep.vox.application.port.input.query.SearchPracticeTopicsQuery;
import com.sep.vox.application.port.input.query.ViewPracticeTopicOffersQuery;
import com.sep.vox.application.port.input.service.PracticePaperDraftService;
import com.sep.vox.application.port.input.service.TopicOfferBackfillService;
import com.sep.vox.application.port.input.usecase.practiceplanning.BuildPracticePaperUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.DismissTopicOfferUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.PickRandomTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.SaveTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.SearchPracticeTopicsSemanticUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.SearchPracticeTopicsUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.UnsaveTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewMySavedTopicsUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewPracticeTopicOffersUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewPracticeDashboardStatsUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.ViewSynchronousTopicOffersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicSearchResult;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeDashboardStats;
import com.sep.vox.interfaces.graphql.dto.request.StartPracticeSessionInput;

@Controller
public class PracticePlanningController {

    private final ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase;
    private final SearchPracticeTopicsUseCase searchPracticeTopicsUseCase;
    private final SearchPracticeTopicsSemanticUseCase searchPracticeTopicsSemanticUseCase;
    private final BuildPracticePaperUseCase buildPracticePaperUseCase;
    private final PickRandomTopicUseCase pickRandomTopicUseCase;
    private final DismissTopicOfferUseCase dismissTopicOfferUseCase;
    private final SaveTopicUseCase saveTopicUseCase;
    private final UnsaveTopicUseCase unsaveTopicUseCase;
    private final ViewSynchronousTopicOffersUseCase viewSynchronousTopicOffersUseCase;
    private final ViewMySavedTopicsUseCase viewMySavedTopicsUseCase;
    private final ViewPracticeDashboardStatsUseCase viewPracticeDashboardStatsUseCase;
    private final PracticePaperDraftService practicePaperDraftService;
    private final UserContextPort userContextPort;
    private final TopicOfferBackfillService topicOfferBackfillService;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public PracticePlanningController(
            ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase,
            SearchPracticeTopicsUseCase searchPracticeTopicsUseCase,
            SearchPracticeTopicsSemanticUseCase searchPracticeTopicsSemanticUseCase,
            BuildPracticePaperUseCase buildPracticePaperUseCase,
            PickRandomTopicUseCase pickRandomTopicUseCase,
            DismissTopicOfferUseCase dismissTopicOfferUseCase,
            SaveTopicUseCase saveTopicUseCase,
            UnsaveTopicUseCase unsaveTopicUseCase,
            ViewSynchronousTopicOffersUseCase viewSynchronousTopicOffersUseCase,
            ViewMySavedTopicsUseCase viewMySavedTopicsUseCase,
            ViewPracticeDashboardStatsUseCase viewPracticeDashboardStatsUseCase,
            PracticePaperDraftService practicePaperDraftService,
            UserContextPort userContextPort,
            TopicOfferBackfillService topicOfferBackfillService,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.practicePaperDraftService = practicePaperDraftService;
        this.userContextPort = userContextPort;
        this.topicOfferBackfillService = topicOfferBackfillService;
        this.practiceGenerationExecutor = practiceGenerationExecutor;
        this.viewPracticeTopicOffersUseCase = viewPracticeTopicOffersUseCase;
        this.searchPracticeTopicsUseCase = searchPracticeTopicsUseCase;
        this.searchPracticeTopicsSemanticUseCase = searchPracticeTopicsSemanticUseCase;
        this.buildPracticePaperUseCase = buildPracticePaperUseCase;
        this.pickRandomTopicUseCase = pickRandomTopicUseCase;
        this.dismissTopicOfferUseCase = dismissTopicOfferUseCase;
        this.saveTopicUseCase = saveTopicUseCase;
        this.unsaveTopicUseCase = unsaveTopicUseCase;
        this.viewMySavedTopicsUseCase = viewMySavedTopicsUseCase;
        this.viewPracticeDashboardStatsUseCase = viewPracticeDashboardStatsUseCase;
        this.viewSynchronousTopicOffersUseCase = viewSynchronousTopicOffersUseCase;
    }

    /**
     * Trả {@code CompletableFuture} chứ không chạy thẳng trên thread request: khi kết quả
     * xếp hạng mỏng (&lt;3), nhánh viewSynchronousTopicOffers sẽ nhờ AI đề xuất chủ đề mới --
     * một cuộc gọi LLM chậm. Chạy async khiến servlet chuyển sang chế độ bất đồng bộ, lúc đó
     * OSIV nhả EntityManager + connection DB thay vì giữ tới hết request.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<List<PracticeTopicOffer>> practiceTopicOffers(
            @Argument("excludeTopicIds") List<UUID> excludeTopicIds,
            @Argument("round") Integer round,
            @Argument("bucket") String bucket) {
        return CompletableFuture.supplyAsync(
            () -> {
                var offers = new ArrayList<>(viewPracticeTopicOffersUseCase.execute(
                    new ViewPracticeTopicOffersQuery(
                        excludeTopicIds,
                        round == null ? 1 : Math.max(1, Math.min(round, 3)),
                        bucket == null ? "FOR_YOU" : bucket
                    )
                ));
                // Kho còn thưa -> bổ sung chạy NỀN, KHÔNG chặn request.
                //
                // Bản trước gọi thẳng viewSynchronousTopicOffersUseCase ở đây, tức chờ LLM
                // đề xuất chủ đề rồi index vector store ngay trong request. Với kho trống
                // (học sinh mới, hoặc vừa dựng lại dữ liệu) thì LẦN NÀO cũng rơi vào đường
                // đó, nên lượt mở màn ngay sau khi làm xong quiz luôn timeout.
                //
                // Trả về những gì đang có -- kể cả rỗng. Client hiện "đang tổng hợp, quay
                // lại sau 1-2 phút"; chủ đề sinh xong được ghi thẳng vào kho nên lần mở sau
                // đường xếp hạng thông thường tự thấy chúng.
                if (offers.size() < 3) {
                    topicOfferBackfillService.backfillAsync(
                        userContextPort.getCurrentAuthenticatedUserId()
                    );
                }
                return (List<PracticeTopicOffer>) offers;
            },
            practiceGenerationExecutor
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public TopicSearchResult searchPracticeTopics(@Argument("keyword") String keyword) {
        return searchPracticeTopicsUseCase.execute(new SearchPracticeTopicsQuery(keyword));
    }

    /**
     * Query RIÊNG, không gộp vào searchPracticeTopics: hai nguồn chênh nhau hàng chục lần về độ
     * trễ, nên client bắn song song rồi vẽ dần thay vì chờ cả hai.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeTopicOffer> searchPracticeTopicsSemantic(@Argument("keyword") String keyword) {
        return searchPracticeTopicsSemanticUseCase.execute(new SearchPracticeTopicsQuery(keyword));
    }


    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeTopicOffer> mySavedTopics() {
        return viewMySavedTopicsUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeDashboardStats myPracticeDashboardStats() {
        return viewPracticeDashboardStatsUseCase.execute(null);
    }

    /**
     * Pha 1 của dựng đề 2 pha: khởi động rồi trả về ngay (READY nếu kho có sẵn câu, PREPARING
     * nếu phải nhờ AI sinh). Việc dựng chạy trên executor riêng nên thread request + connection
     * DB được trả về sớm thay vì bị giữ suốt 10-20s chờ LLM.
     */
    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticePaperDraft buildPracticePaper(@Argument("input") StartPracticeSessionInput input) {
        var command = new BuildPracticePaperCommand(
            input.topicId(),
            input.targetFrameworkBandId(),
            input.origin(),
            input.fromSubAttribute(),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds()
        );
        return practicePaperDraftService.start(
            userContextPort.getCurrentAuthenticatedUserId(),
            () -> buildPracticePaperUseCase.execute(command)
        );
    }

    /** Pha 2: client hỏi lại kết quả dựng đề đã khởi động ở pha 1. */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticePaperDraft practicePaperDraft(@Argument("draftId") UUID draftId) {
        return practicePaperDraftService.get(
            userContextPort.getCurrentAuthenticatedUserId(),
            draftId
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeTopicOffer pickRandomTopic() {
        return pickRandomTopicUseCase.execute(null);
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public boolean saveTopic(@Argument("topicId") UUID topicId) {
        return saveTopicUseCase.execute(new SaveTopicCommand(topicId));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public boolean unsaveTopic(@Argument("topicId") UUID topicId) {
        return unsaveTopicUseCase.execute(new UnsaveTopicCommand(topicId));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public boolean dismissTopicOffer(@Argument("topicId") UUID topicId) {
        return dismissTopicOfferUseCase.execute(new DismissTopicOfferCommand(topicId));
    }
}
