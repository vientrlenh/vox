package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.InterestProfile;
import static com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import static com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import static com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicSearchResult;
import static com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeDashboardStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.command.SaveTopicCommand;
import com.sep.vox.application.port.input.command.UnsaveTopicCommand;
import com.sep.vox.application.port.input.query.SearchPracticeTopicsQuery;
import com.sep.vox.application.port.input.query.ViewPracticeTopicOffersQuery;
import com.sep.vox.application.port.input.query.ViewSynchronousTopicOffersQuery;
import com.sep.vox.application.port.input.usecase.practiceplanning.BuildPracticePaperUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.PickRandomTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.SaveTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.SearchPracticeTopicsUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.UnsaveTopicUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewMyInterestProfileUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewMySavedTopicsUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewPracticeTopicOffersUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewPracticeDashboardStatsUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.ViewSynchronousTopicOffersUseCase;
import com.sep.vox.interfaces.graphql.dto.request.StartPracticeSessionInput;

@Controller
public class PracticePlanningController {

    private final ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase;
    private final SearchPracticeTopicsUseCase searchPracticeTopicsUseCase;
    private final ViewMyInterestProfileUseCase viewMyInterestProfileUseCase;
    private final BuildPracticePaperUseCase buildPracticePaperUseCase;
    private final PickRandomTopicUseCase pickRandomTopicUseCase;
    private final SaveTopicUseCase saveTopicUseCase;
    private final UnsaveTopicUseCase unsaveTopicUseCase;
    private final ViewSynchronousTopicOffersUseCase viewSynchronousTopicOffersUseCase;
    private final ViewMySavedTopicsUseCase viewMySavedTopicsUseCase;
    private final ViewPracticeDashboardStatsUseCase viewPracticeDashboardStatsUseCase;

    public PracticePlanningController(
            ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase,
            SearchPracticeTopicsUseCase searchPracticeTopicsUseCase,
            ViewMyInterestProfileUseCase viewMyInterestProfileUseCase,
            BuildPracticePaperUseCase buildPracticePaperUseCase,
            PickRandomTopicUseCase pickRandomTopicUseCase,
            SaveTopicUseCase saveTopicUseCase,
            UnsaveTopicUseCase unsaveTopicUseCase,
            ViewSynchronousTopicOffersUseCase viewSynchronousTopicOffersUseCase,
            ViewMySavedTopicsUseCase viewMySavedTopicsUseCase,
            ViewPracticeDashboardStatsUseCase viewPracticeDashboardStatsUseCase) {
        this.viewPracticeTopicOffersUseCase = viewPracticeTopicOffersUseCase;
        this.searchPracticeTopicsUseCase = searchPracticeTopicsUseCase;
        this.viewMyInterestProfileUseCase = viewMyInterestProfileUseCase;
        this.buildPracticePaperUseCase = buildPracticePaperUseCase;
        this.pickRandomTopicUseCase = pickRandomTopicUseCase;
        this.saveTopicUseCase = saveTopicUseCase;
        this.unsaveTopicUseCase = unsaveTopicUseCase;
        this.viewMySavedTopicsUseCase = viewMySavedTopicsUseCase;
        this.viewPracticeDashboardStatsUseCase = viewPracticeDashboardStatsUseCase;
        this.viewSynchronousTopicOffersUseCase = viewSynchronousTopicOffersUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeTopicOffer> practiceTopicOffers(
            @Argument("excludeTopicIds") List<UUID> excludeTopicIds,
            @Argument("round") Integer round,
            @Argument("bucket") String bucket) {
        var offers = new ArrayList<>(viewPracticeTopicOffersUseCase.execute(
            new ViewPracticeTopicOffersQuery(
                excludeTopicIds,
                round == null ? 1 : Math.max(1, Math.min(round, 3)),
                bucket == null ? "FOR_YOU" : bucket
            )
        ));
        if (offers.size() < 3) {
            offers.addAll(viewSynchronousTopicOffersUseCase.execute(
                new ViewSynchronousTopicOffersQuery(3 - offers.size())
            ));
            Collections.shuffle(offers);
        }
        return offers;
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public TopicSearchResult searchPracticeTopics(@Argument("keyword") String keyword) {
        return searchPracticeTopicsUseCase.execute(new SearchPracticeTopicsQuery(keyword));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public InterestProfile myInterestProfile() {
        return viewMyInterestProfileUseCase.execute(null);
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

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticePaper buildPracticePaper(@Argument("input") StartPracticeSessionInput input) {
        return buildPracticePaperUseCase.execute(new BuildPracticePaperCommand(
            input.topicId(),
            input.origin(),
            input.fromSubAttribute(),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds()
        ));
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
}
