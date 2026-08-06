package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.SetPracticeGoalCommand;
import com.sep.vox.application.port.input.command.SubmitFlsaSelfReportCommand;
import com.sep.vox.application.port.input.command.SubmitInterestQuizCommand;
import com.sep.vox.application.port.input.usecase.learnerprofile.SetPracticeGoalUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitFlsaSelfReportUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitInterestQuizUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewInterestQuizItemsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewPracticeBandOptionsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewLearnerProfileUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewMyWeaknessProfileUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeBandOption;
import com.sep.vox.domain.model.personalization.QuizAnswer;
import com.sep.vox.interfaces.graphql.dto.request.SubmitInterestQuizInput;

@Controller
public class PracticeController {

    private final ViewMyWeaknessProfileUseCase viewMyWeaknessProfileUseCase;
    private final ViewLearnerProfileUseCase viewLearnerProfileUseCase;
    private final ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase;
    private final ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase;
    private final SubmitInterestQuizUseCase submitInterestQuizUseCase;
    private final SubmitFlsaSelfReportUseCase submitFlsaSelfReportUseCase;
    private final SetPracticeGoalUseCase setPracticeGoalUseCase;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public PracticeController(
            ViewMyWeaknessProfileUseCase viewMyWeaknessProfileUseCase,
            ViewLearnerProfileUseCase viewLearnerProfileUseCase,
            ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase,
            ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase,
            SubmitInterestQuizUseCase submitInterestQuizUseCase,
            SubmitFlsaSelfReportUseCase submitFlsaSelfReportUseCase,
            SetPracticeGoalUseCase setPracticeGoalUseCase,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.practiceGenerationExecutor = practiceGenerationExecutor;
        this.viewMyWeaknessProfileUseCase = viewMyWeaknessProfileUseCase;
        this.viewLearnerProfileUseCase = viewLearnerProfileUseCase;
        this.viewInterestQuizItemsUseCase = viewInterestQuizItemsUseCase;
        this.viewPracticeBandOptionsUseCase = viewPracticeBandOptionsUseCase;
        this.submitInterestQuizUseCase = submitInterestQuizUseCase;
        this.submitFlsaSelfReportUseCase = submitFlsaSelfReportUseCase;
        this.setPracticeGoalUseCase = setPracticeGoalUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public WeaknessProfile myWeaknessProfile() {
        return viewMyWeaknessProfileUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile myLearnerProfile() {
        return viewLearnerProfileUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeBandOption> myPracticeBandOptions() {
        return viewPracticeBandOptionsUseCase.execute(null);
    }

    /**
     * Async vì use case này có thể phải nhờ AI sinh bộ quiz sở thích riêng cho học sinh
     * (10-20s). Chạy trên executor riêng khiến servlet vào chế độ bất đồng bộ, lúc đó OSIV
     * nhả EntityManager + connection DB thay vì giữ tới hết request.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<List<InterestQuizItem>> interestQuizItems() {
        return CompletableFuture.supplyAsync(
            () -> viewInterestQuizItemsUseCase.execute(null),
            practiceGenerationExecutor
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile submitInterestQuiz(@Argument("input") SubmitInterestQuizInput input) {
        var answers = input.answers().stream()
            .map(answer -> new QuizAnswer(
                answer.itemId(),
                answer.mostStatementIndex(),
                answer.leastStatementIndex()
            ))
            .toList();
        return submitInterestQuizUseCase.execute(new SubmitInterestQuizCommand(answers));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile submitFlsaSelfReport(@Argument("answers") List<Integer> answers) {
        return submitFlsaSelfReportUseCase.execute(new SubmitFlsaSelfReportCommand(answers));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile setPracticeGoal(@Argument("goalType") String goalType) {
        return setPracticeGoalUseCase.execute(new SetPracticeGoalCommand(goalType));
    }

}
