package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryStudentsUseCase;
import com.sep.vox.application.projection.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.projection.dto.ExamDirectoryUserInfo;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.shared.PageResult;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor 
public class GraphQLExamDirectoryController {

    private final ViewExamDirectoryClassesUseCase viewExamDirectoryClassesUseCase;
    private final ViewExamDirectoryGradesUseCase viewExamDirectoryGradesUseCase;
    private final ViewExamDirectoryStudentsUseCase viewExamDirectoryStudentsUseCase;
    private final ViewExamDirectoryProctorsUseCase viewExamDirectoryProctorsUseCase;

    @QueryMapping(name = "examDirectoryClasses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolClassDto> examDirectoryClasses(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewExamDirectoryClassesUseCase.execute(query(examId, search, page, size));
    }

    @QueryMapping(name = "examDirectoryGrades")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryGradeInfo> examDirectoryGrades(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewExamDirectoryGradesUseCase.execute(query(examId, search, page, size));
    }

    @QueryMapping(name = "examDirectoryStudents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryUserInfo> examDirectoryStudents(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewExamDirectoryStudentsUseCase.execute(query(examId, search, page, size, excludeUserIds));
    }

    @QueryMapping(name = "examDirectoryProctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryUserInfo> examDirectoryProctors(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewExamDirectoryProctorsUseCase.execute(query(examId, search, page, size, excludeUserIds));
    }

    private static ViewExamDirectoryQuery query(UUID examId, String search, Integer page, Integer size) {
        return query(examId, search, page, size, List.of());
    }

    private static ViewExamDirectoryQuery query(UUID examId, String search, Integer page, Integer size,
            List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return new ViewExamDirectoryQuery(examId, search, page, size, excludeUserIds);
    }
}
