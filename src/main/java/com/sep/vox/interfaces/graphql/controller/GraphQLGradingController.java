package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.UserGraphQLDataLoaderConfig;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.assessmentgrading.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewAiQualityReportQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewAiQualityReportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewClassTestGradingResultsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewClassTestGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewAssignableTeachersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewGradingTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyClassTestGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.interfaces.shared.PageArguments;

import graphql.schema.DataFetchingEnvironment;

@Controller
@RequiredArgsConstructor
public class GraphQLGradingController {

    private final ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase;
    private final ViewGradingStatsUseCase viewGradingStatsUseCase;
    private final ViewClassTestGradingStatsUseCase viewClassTestGradingStatsUseCase;
    private final ViewClassTestGradingResultsUseCase viewClassTestGradingResultsUseCase;
    private final ViewMyGradingTasksUseCase viewMyGradingTasksUseCase;
    private final ViewMyGradingAssessmentsUseCase viewMyGradingAssessmentsUseCase;
    private final ViewMyClassTestGradingTasksUseCase viewMyClassTestGradingTasksUseCase;
    private final ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase;
    private final ViewAssignableTeachersUseCase viewAssignableTeachersUseCase;
    private final ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase;
    private final ViewAiQualityReportUseCase viewAiQualityReportUseCase;

    @QueryMapping(name = "gradingAssignments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<GradingAssignmentRowInfo> gradingAssignments(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "teacherId") UUID teacherId,
            @Argument(name = "resultStatus") AssessmentCandidateResultStatus resultStatus,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "status") String status,
            @Argument(name = "unassignedOnly") Boolean unassignedOnly,
            @Argument(name = "overdueOnly") Boolean overdueOnly,
            @Argument(name = "hasOpenAppeal") Boolean hasOpenAppeal,
            @Argument(name = "search") String search,
            @Argument(name = "kind") String kind,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewGradingAssignmentsUseCase.execute(new SearchGradingAssignmentsQuery(
            assessmentId, scheduleId, teacherId, resultStatus, roundType, status,
            Boolean.TRUE.equals(unassignedOnly), Boolean.TRUE.equals(overdueOnly), hasOpenAppeal, search, kind,
            page, size));
    }

    @QueryMapping(name = "gradingStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradingStatsInfo gradingStats(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "kind") String kind) {
        return viewGradingStatsUseCase.execute(new ViewGradingStatsQuery(assessmentId, scheduleId, kind));
    }

    @QueryMapping(name = "classTestGradingStats")
    @PreAuthorize("hasRole('TEACHER')")
    public GradingStatsInfo classTestGradingStats(@Argument(name = "assessmentId") UUID assessmentId) {
        return viewClassTestGradingStatsUseCase.execute(assessmentId);
    }

    @QueryMapping(name = "classTestGradingResults")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingAssignmentRowInfo> classTestGradingResults(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "resultStatus") AssessmentCandidateResultStatus resultStatus,
            @Argument(name = "unassignedOnly") Boolean unassignedOnly,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewClassTestGradingResultsUseCase.execute(new ViewClassTestGradingResultsQuery(
            assessmentId, resultStatus, Boolean.TRUE.equals(unassignedOnly), search,
            page, size));
    }

    @QueryMapping(name = "myGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myGradingTasks(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") String status,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewMyGradingTasksUseCase.execute(new ViewMyGradingTasksQuery(
            assessmentId, status, roundType,
            page, size));
    }

    /**
     * No need to pin the scope here: the scope IS the caller's own assignment set, read
     * from the token in the use case. TEACHER only -- the school already has {@code gradingAssignments},
     * and asking here would only return an empty list.
     */
    @QueryMapping(name = "myGradingAssessments")
    @PreAuthorize("hasRole('TEACHER')")
    public List<GradingAssessmentOptionInfo> myGradingAssessments() {
        return viewMyGradingAssessmentsUseCase.execute(null);
    }

    @QueryMapping(name = "myClassTestGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myClassTestGradingTasks(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") String status,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewMyClassTestGradingTasksUseCase.execute(new ViewMyClassTestGradingTasksQuery(
            assessmentId, status, roundType,
            page, size
        ));
    }

    @QueryMapping(name = "gradingTaskDetail")
    @PreAuthorize("hasRole('TEACHER')")
    public GradingTaskDetailInfo gradingTaskDetail(@Argument(name = "assignmentId") UUID assignmentId) {
        return viewGradingTaskDetailUseCase.execute(assignmentId);
    }

    @QueryMapping(name = "assignableTeachers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<AssignableTeacherInfo> assignableTeachers(@Argument(name = "search") String search) {
        return viewAssignableTeachersUseCase.execute(new ViewAssignableTeachersQuery(search));
    }

    /**
     * A student can see the score timeline of their OWN results; an admin can see that of results
     * belonging to their school. Authorization lives in the use case, not here.
     */
    @QueryMapping(name = "resultStatusHistory")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ResultStatusHistoryInfo> resultStatusHistory(
            @Argument(name = "candidateResultId") UUID candidateResultId) {
        return viewResultStatusHistoryUseCase.execute(candidateResultId);
    }

    @QueryMapping(name = "aiQualityReport")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AiQualityReportInfo aiQualityReport(@Argument(name = "assessmentId") UUID assessmentId) {
        return viewAiQualityReportUseCase.execute(new ViewAiQualityReportQuery(assessmentId));
    }

    /*
     * The three resolvers below attach the user profile to the teacher's STATISTICS/PICKER rows, so the FE can
     * show an avatar. They go through the DataLoader `userById` so N rows are still one query, and the read
     * model underneath does not need any extra join.
     *
     * DELIBERATELY there is no matching resolver for students: the grading read model does not carry studentId anywhere
     * (grep "studentId" in assessment-grading.graphqls gives 0), and that is exactly what keeps the centralized
     * assessment queue anonymous. Showing a student's face would require adding studentId to the read model --
     * exactly the change the anonymity rule was built to prevent.
     */

    @SchemaMapping(typeName = "AssignableTeacher", field = "user")
    public CompletableFuture<UserDto> assignableTeacherUser(
            AssignableTeacherInfo source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "GradingTeacherProgress", field = "teacher")
    public CompletableFuture<UserDto> teacherProgressUser(
            GradingStatsInfo.TeacherProgress source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.teacherId());
    }

    @SchemaMapping(typeName = "AiQualityByTeacher", field = "teacher")
    public CompletableFuture<UserDto> aiQualityTeacherUser(
            AiQualityReportInfo.ByTeacher source, DataFetchingEnvironment env) {
        // teacherId is nullable on this type. The DataLoader throws NPE when it receives a null key and breaks the WHOLE
        // aiQualityReport query, not only this field -- see GraphQLAssessmentScheduleController#room.
        if (source.teacherId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.teacherId());
    }
}