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
import com.sep.vox.application.port.input.service.TopicOfferBackfillService;
import com.sep.vox.application.port.output.UserContextPort;
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
    private final TopicOfferBackfillService topicOfferBackfillService;
    private final UserContextPort userContextPort;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public PracticeController(
            ViewLearnerProfileUseCase viewLearnerProfileUseCase,
            ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase,
            ViewPracticeBandOptionsUseCase viewPracticeBandOptionsUseCase,
            ViewPracticeFrameworkOptionsUseCase viewPracticeFrameworkOptionsUseCase,
            SubmitInterestQuizUseCase submitInterestQuizUseCase,
            SetPracticeGoalUseCase setPracticeGoalUseCase,
            TopicOfferBackfillService topicOfferBackfillService,
            UserContextPort userContextPort,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.topicOfferBackfillService = topicOfferBackfillService;
        this.userContextPort = userContextPort;
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
        List<QuizAnswer> answers = input.answers().stream()
            .map(answer -> QuizAnswer.builder()
                    .itemId(answer.itemId())
                    .mostStatementIndex(answer.mostStatementIndex())
                    .leastStatementIndex(answer.leastStatementIndex())
                    .build())
            .toList();
        LearnerProfile profile = submitInterestQuizUseCase.execute(new SubmitInterestQuizCommand(answers));

        // Generate topics RIGHT AFTER the quiz is submitted, in the background, WITHOUT blocking the response.
        //
        // Previously there was only one generation path: the topic-selection screen called it when it saw
        // `offers.size() < 3` (PracticePlanningController). That means a student who has just finished the quiz
        // while the shared pool is full gets NO topic generated for the interests they just declared -- they
        // can only pick from topics other people already generated. Here it is called unconditionally: even
        // when the pool is full, backfillAsync still generates BACKFILL_COUNT_STEADY = 2 topics that follow the
        // interest scores just recorded.
        //
        // CALL IT IN THE CONTROLLER, NOT in the use case: the use case is @Transactional while backfillAsync is
        // @Async, so calling it inside would let the background thread run BEFORE the transaction commits --
        // hasCompletedInterestQuiz would read "quiz not done" and skip the turn, silently doing nothing. The
        // controller has no transaction, so by this line the quiz has already committed.
        //
        // A duplicate call costs nothing extra: backfillAsync has an in-flight lock per studentId.
        topicOfferBackfillService.backfillAsync(userContextPort.getCurrentAuthenticatedUserId());
        return profile;
    }

    /**
     * Async cùng lý do với {@link #interestQuizItems()}: nhánh sinh mới gọi LLM 10-20 giây.
     */
    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<List<InterestQuizItem>> regenerateInterestQuiz() {
        return CompletableFuture.supplyAsync(
            viewInterestQuizItemsUseCase::regenerate,
            practiceGenerationExecutor
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile setPracticeGoal(@Argument("goalType") String goalType) {
        return setPracticeGoalUseCase.execute(new SetPracticeGoalCommand(goalType));
    }

}
