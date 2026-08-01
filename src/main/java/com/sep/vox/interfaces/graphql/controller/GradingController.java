package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.query.ViewAiQualityReportQuery;
import com.sep.vox.application.port.input.query.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.query.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.usecase.examgrading.ViewAiQualityReportUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewAssignableTeachersUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.domain.common.PageResult;

@Controller("graphqlGradingController")
public class GradingController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase;
    private final ViewGradingStatsUseCase viewGradingStatsUseCase;
    private final ViewMyGradingTasksUseCase viewMyGradingTasksUseCase;
    private final ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase;
    private final ViewAssignableTeachersUseCase viewAssignableTeachersUseCase;
    private final ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase;
    private final ViewAiQualityReportUseCase viewAiQualityReportUseCase;

    public GradingController(
            ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase,
            ViewGradingStatsUseCase viewGradingStatsUseCase,
            ViewMyGradingTasksUseCase viewMyGradingTasksUseCase,
            ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase,
            ViewAssignableTeachersUseCase viewAssignableTeachersUseCase,
            ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase,
            ViewAiQualityReportUseCase viewAiQualityReportUseCase) {
        this.viewGradingAssignmentsUseCase = viewGradingAssignmentsUseCase;
        this.viewGradingStatsUseCase = viewGradingStatsUseCase;
        this.viewMyGradingTasksUseCase = viewMyGradingTasksUseCase;
        this.viewGradingTaskDetailUseCase = viewGradingTaskDetailUseCase;
        this.viewAssignableTeachersUseCase = viewAssignableTeachersUseCase;
        this.viewResultStatusHistoryUseCase = viewResultStatusHistoryUseCase;
        this.viewAiQualityReportUseCase = viewAiQualityReportUseCase;
    }

    @QueryMapping(name = "gradingAssignments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<GradingAssignmentRowInfo> gradingAssignments(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "teacherId") UUID teacherId,
            @Argument(name = "resultStatus") String resultStatus,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "status") String status,
            @Argument(name = "unassignedOnly") Boolean unassignedOnly,
            @Argument(name = "overdueOnly") Boolean overdueOnly,
            @Argument(name = "hasOpenAppeal") Boolean hasOpenAppeal,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        return viewGradingAssignmentsUseCase.execute(new SearchGradingAssignmentsQuery(
            examId, scheduleId, teacherId, resultStatus, roundType, status,
            Boolean.TRUE.equals(unassignedOnly), Boolean.TRUE.equals(overdueOnly), hasOpenAppeal, search,
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
            @Argument(name = "roundType") String roundType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        return viewMyGradingTasksUseCase.execute(new ViewMyGradingTasksQuery(
            status, roundType, page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size));
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
     * Học sinh xem được dòng thời gian điểm của CHÍNH MÌNH; admin xem được của bài
     * thuộc trường mình. Phân quyền nằm trong use case, không ở đây.
     */
    @QueryMapping(name = "resultStatusHistory")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ResultStatusHistoryInfo> resultStatusHistory(
            @Argument(name = "candidateResultId") UUID candidateResultId) {
        return viewResultStatusHistoryUseCase.execute(candidateResultId);
    }

    @QueryMapping(name = "aiQualityReport")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AiQualityReportInfo aiQualityReport(@Argument(name = "examId") UUID examId) {
        return viewAiQualityReportUseCase.execute(new ViewAiQualityReportQuery(examId));
    }
}
