package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeOverview;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.SetInterestAutoUpdateCommand;
import com.sep.vox.application.port.input.command.SetPracticeGoalCommand;
import com.sep.vox.application.port.input.command.SubmitFlsaSelfReportCommand;
import com.sep.vox.application.port.input.command.SubmitInterestQuizCommand;
import com.sep.vox.application.port.input.query.ViewClassPracticeOverviewQuery;
import com.sep.vox.application.port.input.query.ViewMyPracticeProgressQuery;
import com.sep.vox.application.port.input.query.ViewStudentPracticeProgressQuery;
import com.sep.vox.application.port.input.query.ViewStudentWeaknessProfileQuery;
import com.sep.vox.application.port.input.usecase.learnerprofile.SetInterestAutoUpdateUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SetPracticeGoalUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitFlsaSelfReportUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.SubmitInterestQuizUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewInterestQuizItemsUseCase;
import com.sep.vox.application.port.input.usecase.learnerprofile.ViewLearnerProfileUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewClassPracticeOverviewUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewMyPracticeProgressUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewMyWeaknessProfileUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewStudentPracticeProgressUseCase;
import com.sep.vox.application.port.input.usecase.practiceinsights.ViewStudentWeaknessProfileUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.domain.model.personalization.QuizAnswer;
import com.sep.vox.interfaces.graphql.dto.request.SubmitInterestQuizInput;

@Controller
public class PracticeController {

    private final ViewMyWeaknessProfileUseCase viewMyWeaknessProfileUseCase;
    private final ViewMyPracticeProgressUseCase viewMyPracticeProgressUseCase;
    private final ViewStudentWeaknessProfileUseCase viewStudentWeaknessProfileUseCase;
    private final ViewStudentPracticeProgressUseCase viewStudentPracticeProgressUseCase;
    private final ViewClassPracticeOverviewUseCase viewClassPracticeOverviewUseCase;
    private final ViewLearnerProfileUseCase viewLearnerProfileUseCase;
    private final ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase;
    private final SubmitInterestQuizUseCase submitInterestQuizUseCase;
    private final SubmitFlsaSelfReportUseCase submitFlsaSelfReportUseCase;
    private final SetPracticeGoalUseCase setPracticeGoalUseCase;
    private final SetInterestAutoUpdateUseCase setInterestAutoUpdateUseCase;

    public PracticeController(
            ViewMyWeaknessProfileUseCase viewMyWeaknessProfileUseCase,
            ViewMyPracticeProgressUseCase viewMyPracticeProgressUseCase,
            ViewStudentWeaknessProfileUseCase viewStudentWeaknessProfileUseCase,
            ViewStudentPracticeProgressUseCase viewStudentPracticeProgressUseCase,
            ViewClassPracticeOverviewUseCase viewClassPracticeOverviewUseCase,
            ViewLearnerProfileUseCase viewLearnerProfileUseCase,
            ViewInterestQuizItemsUseCase viewInterestQuizItemsUseCase,
            SubmitInterestQuizUseCase submitInterestQuizUseCase,
            SubmitFlsaSelfReportUseCase submitFlsaSelfReportUseCase,
            SetPracticeGoalUseCase setPracticeGoalUseCase,
            SetInterestAutoUpdateUseCase setInterestAutoUpdateUseCase) {
        this.viewMyWeaknessProfileUseCase = viewMyWeaknessProfileUseCase;
        this.viewMyPracticeProgressUseCase = viewMyPracticeProgressUseCase;
        this.viewStudentWeaknessProfileUseCase = viewStudentWeaknessProfileUseCase;
        this.viewStudentPracticeProgressUseCase = viewStudentPracticeProgressUseCase;
        this.viewClassPracticeOverviewUseCase = viewClassPracticeOverviewUseCase;
        this.viewLearnerProfileUseCase = viewLearnerProfileUseCase;
        this.viewInterestQuizItemsUseCase = viewInterestQuizItemsUseCase;
        this.submitInterestQuizUseCase = submitInterestQuizUseCase;
        this.submitFlsaSelfReportUseCase = submitFlsaSelfReportUseCase;
        this.setPracticeGoalUseCase = setPracticeGoalUseCase;
        this.setInterestAutoUpdateUseCase = setInterestAutoUpdateUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public WeaknessProfile myWeaknessProfile() {
        return viewMyWeaknessProfileUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<CriterionProgressPoint> myPracticeProgress(
            @Argument("criterionCode") String criterionCode,
            @Argument("days") Integer days) {
        return viewMyPracticeProgressUseCase.execute(
            new ViewMyPracticeProgressQuery(criterionCode, days == null ? 90 : days)
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public WeaknessProfile studentWeaknessProfile(@Argument("studentId") UUID studentId) {
        return viewStudentWeaknessProfileUseCase.execute(new ViewStudentWeaknessProfileQuery(studentId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public List<CriterionProgressPoint> studentPracticeProgress(@Argument("studentId") UUID studentId) {
        return viewStudentPracticeProgressUseCase.execute(new ViewStudentPracticeProgressQuery(studentId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ClassPracticeOverview classPracticeOverview(@Argument("classId") UUID classId) {
        return viewClassPracticeOverviewUseCase.execute(new ViewClassPracticeOverviewQuery(classId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile myLearnerProfile() {
        return viewLearnerProfileUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<InterestQuizItem> interestQuizItems() {
        return viewInterestQuizItemsUseCase.execute(null);
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

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public LearnerProfile setInterestAutoUpdate(@Argument("enabled") boolean enabled) {
        return setInterestAutoUpdateUseCase.execute(new SetInterestAutoUpdateCommand(enabled));
    }
}
