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
import com.sep.vox.application.port.input.query.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.query.ViewGradingStatsQuery;
import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.query.ViewMyClassTestGradingTasksQuery;
import com.sep.vox.application.port.input.usecase.examgrading.ViewAiQualityReportUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewClassTestGradingResultsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewClassTestGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewAssignableTeachersUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingStatsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewGradingTaskDetailUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingExamsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyClassTestGradingTasksUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingExamOptionInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller("graphqlGradingController")
public class GradingController {

    private final ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase;
    private final ViewGradingStatsUseCase viewGradingStatsUseCase;
    private final ViewClassTestGradingStatsUseCase viewClassTestGradingStatsUseCase;
    private final ViewClassTestGradingResultsUseCase viewClassTestGradingResultsUseCase;
    private final ViewMyGradingTasksUseCase viewMyGradingTasksUseCase;
    private final ViewMyGradingExamsUseCase viewMyGradingExamsUseCase;
    private final ViewMyClassTestGradingTasksUseCase viewMyClassTestGradingTasksUseCase;
    private final ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase;
    private final ViewAssignableTeachersUseCase viewAssignableTeachersUseCase;
    private final ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase;
    private final ViewAiQualityReportUseCase viewAiQualityReportUseCase;

    public GradingController(
            ViewGradingAssignmentsUseCase viewGradingAssignmentsUseCase,
            ViewGradingStatsUseCase viewGradingStatsUseCase,
            ViewClassTestGradingStatsUseCase viewClassTestGradingStatsUseCase,
            ViewClassTestGradingResultsUseCase viewClassTestGradingResultsUseCase,
            ViewMyGradingTasksUseCase viewMyGradingTasksUseCase,
            ViewMyGradingExamsUseCase viewMyGradingExamsUseCase,
            ViewMyClassTestGradingTasksUseCase viewMyClassTestGradingTasksUseCase,
            ViewGradingTaskDetailUseCase viewGradingTaskDetailUseCase,
            ViewAssignableTeachersUseCase viewAssignableTeachersUseCase,
            ViewResultStatusHistoryUseCase viewResultStatusHistoryUseCase,
            ViewAiQualityReportUseCase viewAiQualityReportUseCase) {
        this.viewGradingAssignmentsUseCase = viewGradingAssignmentsUseCase;
        this.viewGradingStatsUseCase = viewGradingStatsUseCase;
        this.viewClassTestGradingStatsUseCase = viewClassTestGradingStatsUseCase;
        this.viewClassTestGradingResultsUseCase = viewClassTestGradingResultsUseCase;
        this.viewMyGradingTasksUseCase = viewMyGradingTasksUseCase;
        this.viewMyGradingExamsUseCase = viewMyGradingExamsUseCase;
        this.viewMyClassTestGradingTasksUseCase = viewMyClassTestGradingTasksUseCase;
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
            @Argument(name = "kind") String kind,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewGradingAssignmentsUseCase.execute(new SearchGradingAssignmentsQuery(
            examId, scheduleId, teacherId, resultStatus, roundType, status,
            Boolean.TRUE.equals(unassignedOnly), Boolean.TRUE.equals(overdueOnly), hasOpenAppeal, search, kind,
            page, size));
    }

    @QueryMapping(name = "gradingStats")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradingStatsInfo gradingStats(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "kind") String kind) {
        return viewGradingStatsUseCase.execute(new ViewGradingStatsQuery(examId, scheduleId, kind));
    }

    @QueryMapping(name = "classTestGradingStats")
    @PreAuthorize("hasRole('TEACHER')")
    public GradingStatsInfo classTestGradingStats(@Argument(name = "examId") UUID examId) {
        return viewClassTestGradingStatsUseCase.execute(examId);
    }

    @QueryMapping(name = "classTestGradingResults")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingAssignmentRowInfo> classTestGradingResults(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "resultStatus") String resultStatus,
            @Argument(name = "unassignedOnly") Boolean unassignedOnly,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewClassTestGradingResultsUseCase.execute(new ViewClassTestGradingResultsQuery(
            examId, resultStatus, Boolean.TRUE.equals(unassignedOnly), search,
            page, size));
    }

    @QueryMapping(name = "myGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myGradingTasks(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") String status,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewMyGradingTasksUseCase.execute(new ViewMyGradingTasksQuery(
            examId, status, roundType,
            page, size));
    }

    /**
     * Không cần chốt phạm vi ở đây: phạm vi CHÍNH LÀ tập phân công của người gọi, đọc
     * từ token trong use case. Chỉ TEACHER — nhà trường đã có {@code gradingAssignments}
     * và hỏi ở đây cũng chỉ nhận về danh sách rỗng.
     */
    @QueryMapping(name = "myGradingExams")
    @PreAuthorize("hasRole('TEACHER')")
    public List<GradingExamOptionInfo> myGradingExams() {
        return viewMyGradingExamsUseCase.execute(null);
    }

    @QueryMapping(name = "myClassTestGradingTasks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<GradingTaskInfo> myClassTestGradingTasks(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") String status,
            @Argument(name = "roundType") String roundType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewMyClassTestGradingTasksUseCase.execute(new ViewMyClassTestGradingTasksQuery(
            examId, status, roundType,
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

    /**
     * Biên là chỗ duy nhất kiểm phân trang: repository bên dưới tin vào đây và chỉ còn trừ 1 để đổi
     * sang offset, không tự kẹp lại giá trị sai nữa.
     */
    private void validatePageSize(Integer page, Integer size) {
        PageArguments.validate(page, size);
    }
}