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
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingTaskDetailBySchoolUseCase;
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
    private final ViewGradingTaskDetailBySchoolUseCase viewGradingTaskDetailBySchoolUseCase;
    private final ViewAssignableTeachersUseCase viewAssignableTeachersUseCase;

    public GradingController(
            ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase,
            ViewGradingStatsUseCase viewGradingStatsUseCase,
            ViewMyGradingTasksUseCase viewMyGradingTasksUseCase,
            ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase,
            ViewGradingTaskDetailBySchoolUseCase viewGradingTaskDetailBySchoolUseCase,
            ViewAssignableTeachersUseCase viewAssignableTeachersUseCase) {
        this.viewGradingAssignmentsUseCase = viewGradingAssignmentsUseCase;
        this.viewGradingStatsUseCase = viewGradingStatsUseCase;
        this.viewMyGradingTasksUseCase = viewMyGradingTasksUseCase;
        this.viewGradingTaskDetailUseCase = viewGradingTaskDetailUseCase;
        this.viewGradingTaskDetailBySchoolUseCase = viewGradingTaskDetailBySchoolUseCase;
        this.viewAssignableTeachersUseCase = viewAssignableTeachersUseCase;
    }

    @QueryMapping(name = "gradingAssignments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<GradingAssignmentRowInfo> gradingAssignments(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "teacherId") UUID teacherId,
            @Argument(name = "status") String status,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        return viewGradingAssignmentsUseCase.execute(new SearchGradingAssignmentsQuery(
            examId, scheduleId, teacherId, status, search,
            page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size));
    }

    @QueryMapping(name = "gradingStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradingStatsInfo gradingStats(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewGradingStatsUseCase.execute(new ViewGradingStatsQuery(examId, scheduleId));
    }

    @QueryMapping(name = "myGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myGradingTasks(
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        return viewMyGradingTasksUseCase.execute(new ViewMyGradingTasksQuery(
            status, page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size));
    }

    @QueryMapping(name = "gradingTaskDetail")
    @PreAuthorize("hasRole('TEACHER')")
    public GradingTaskDetailInfo gradingTaskDetail(@Argument(name = "assignmentId") UUID assignmentId) {
        return viewGradingTaskDetailUseCase.execute(assignmentId);
    }

    // Nhà trường xem/chấm trực tiếp theo candidateResultId, không cần phân công --
    // luôn xem được bất kỳ bài PENDING_REVIEW nào của trường mình.
    @QueryMapping(name = "gradingTaskDetailBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradingTaskDetailInfo gradingTaskDetailBySchool(
            @Argument(name = "candidateResultId") UUID candidateResultId) {
        return viewGradingTaskDetailBySchoolUseCase.execute(candidateResultId);
    }

    @QueryMapping(name = "assignableTeachers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<AssignableTeacherInfo> assignableTeachers(@Argument(name = "search") String search) {
        return viewAssignableTeachersUseCase.execute(new ViewAssignableTeachersQuery(search));
    }
}
