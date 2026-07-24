package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.query.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.query.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.usecase.examgrading.ViewAssignableTeachersUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.domain.common.PageResult;

@Controller("graphqlGradingController")
public class GradingController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase;
    private final ViewGradingStatsUseCase viewGradingStatsUseCase;
    private final ViewMyGradingTasksUseCase viewMyGradingTasksUseCase;
    private final ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase;
    private final ViewAssignableTeachersUseCase viewAssignableTeachersUseCase;

    public GradingController(
            ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase,
            ViewGradingStatsUseCase viewGradingStatsUseCase,
            ViewMyGradingTasksUseCase viewMyGradingTasksUseCase,
            ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase,
            ViewAssignableTeachersUseCase viewAssignableTeachersUseCase) {
        this.viewGradingAssignmentsUseCase = viewGradingAssignmentsUseCase;
        this.viewGradingStatsUseCase = viewGradingStatsUseCase;
        this.viewMyGradingTasksUseCase = viewMyGradingTasksUseCase;
        this.viewGradingTaskDetailUseCase = viewGradingTaskDetailUseCase;
        this.viewAssignableTeachersUseCase = viewAssignableTeachersUseCase;
    }

    @QueryMapping(name = "gradingAssignments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<GradingAssignmentRowInfo> gradingAssignments(
            @Argument("examId") UUID examId,
            @Argument("scheduleId") UUID scheduleId,
            @Argument("teacherId") UUID teacherId,
            @Argument("status") String status,
            @Argument("search") String search,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return viewGradingAssignmentsUseCase.execute(new SearchGradingAssignmentsQuery(
            examId, scheduleId, teacherId, status, search,
            page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size));
    }

    @QueryMapping(name = "gradingStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradingStatsInfo gradingStats(
            @Argument("examId") UUID examId,
            @Argument("scheduleId") UUID scheduleId) {
        return viewGradingStatsUseCase.execute(new ViewGradingStatsQuery(examId, scheduleId));
    }

    @QueryMapping(name = "myGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myGradingTasks(
            @Argument("status") String status,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        return viewMyGradingTasksUseCase.execute(new ViewMyGradingTasksQuery(
            status, page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size));
    }

    @QueryMapping(name = "gradingTaskDetail")
    @PreAuthorize("hasRole('TEACHER')")
    public GradingTaskDetailInfo gradingTaskDetail(@Argument("assignmentId") UUID assignmentId) {
        return viewGradingTaskDetailUseCase.execute(assignmentId);
    }

    @QueryMapping(name = "assignableTeachers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<AssignableTeacherInfo> assignableTeachers(@Argument("search") String search) {
        return viewAssignableTeachersUseCase.execute(new ViewAssignableTeachersQuery(search));
    }
}
