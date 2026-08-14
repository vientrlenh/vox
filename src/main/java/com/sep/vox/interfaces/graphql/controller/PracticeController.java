package com.sep.vox.interfaces.graphql.controller;

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
import com.sep.vox.application.port.input.command.SubmitInterestQuizCommand;
import com.sep.vox.application.port.input.usecase.learnerprofile.SetPracticeGoalUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitInterestQuizUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewInterestQuizItemsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewPracticeBandOptionsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewPracticeFrameworkOptionsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewLearnerProfileUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeBandOption;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeFrameworkOption;
import com.sep.vox.domain.model.personalization.QuizAnswer;
import com.sep.vox.interfaces.graphql.dto.request.SubmitInterestQuizInput;

@Controller
public class PracticeController {

    private final ViewLearnerProfileUseCase viewLearnerProfileUseCase;
    private final ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase;
    private final ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase;
    private final ViewPracticeFrameworkOptionsUseCase viewPracticeFrameworkOptionsUseCase;
    private final SubmitInterestQuizUseCase submitInterestQuizUseCase;
    private final SetPracticeGoalUseCase setPracticeGoalUseCase;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public PracticeController(
            ViewLearnerProfileUseCase viewLearnerProfileUseCase,
            ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase,
            ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase,
            ViewPracticeFrameworkOptionsUseCase viewPracticeFrameworkOptionsUseCase,
            SubmitInterestQuizUseCase submitInterestQuizUseCase,
            SetPracticeGoalUseCase setPracticeGoalUseCase,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.practiceGenerationExecutor = practiceGenerationExecutor;
        this.viewLearnerProfileUseCase = viewLearnerProfileUseCase;
        this.viewInterestQuizItemsUseCase = viewInterestQuizItemsUseCase;
        this.viewPracticeBandOptionsUseCase = viewPracticeBandOptionsUseCase;
        this.viewPracticeFrameworkOptionsUseCase = viewPracticeFrameworkOptionsUseCase;
        this.submitInterestQuizUseCase = submitInterestQuizUseCase;
        this.setPracticeGoalUseCase = setPracticeGoalUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile myLearnerProfile() {
        return viewLearnerProfileUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeFrameworkOption> myPracticeFrameworkOptions() {
        return viewPracticeFrameworkOptionsUseCase.execute(null);
    }

    /**
     * @param frameworkVersionId khung học sinh vừa chọn ở ô phía trên. Bỏ trống = khung đang
     *     hiệu lực toàn hệ, giữ nguyên hành vi cho client cũ.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeBandOption> myPracticeBandOptions(
            @Argument(name = "frameworkVersionId") UUID frameworkVersionId) {
        return viewPracticeBandOptionsUseCase.execute(frameworkVersionId);
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
    public LearnerProfile setPracticeGoal(@Argument("goalType") String goalType) {
        return setPracticeGoalUseCase.execute(new SetPracticeGoalCommand(goalType));
    }

}
