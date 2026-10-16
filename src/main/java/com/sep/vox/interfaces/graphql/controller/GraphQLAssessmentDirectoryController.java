package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryStudentsUseCase;
import com.sep.vox.application.projection.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.projection.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.shared.PageResult;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor 
public class GraphQLAssessmentDirectoryController {

    private final ViewAssessmentDirectoryClassesUseCase viewAssessmentDirectoryClassesUseCase;
    private final ViewAssessmentDirectoryGradesUseCase viewAssessmentDirectoryGradesUseCase;
    private final ViewAssessmentDirectoryStudentsUseCase viewAssessmentDirectoryStudentsUseCase;
    private final ViewAssessmentDirectoryProctorsUseCase viewAssessmentDirectoryProctorsUseCase;

    @QueryMapping(name = "assessmentDirectoryClasses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolClassDto> assessmentDirectoryClasses(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewAssessmentDirectoryClassesUseCase.execute(query(assessmentId, search, page, size));
    }

    @QueryMapping(name = "assessmentDirectoryGrades")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<AssessmentDirectoryGradeInfo> assessmentDirectoryGrades(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewAssessmentDirectoryGradesUseCase.execute(query(assessmentId, search, page, size));
    }

    @QueryMapping(name = "assessmentDirectoryStudents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<AssessmentDirectoryUserInfo> assessmentDirectoryStudents(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewAssessmentDirectoryStudentsUseCase.execute(query(assessmentId, search, page, size, excludeUserIds));
    }

    @QueryMapping(name = "assessmentDirectoryProctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<AssessmentDirectoryUserInfo> assessmentDirectoryProctors(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewAssessmentDirectoryProctorsUseCase.execute(query(assessmentId, search, page, size, excludeUserIds));
    }

    private static ViewAssessmentDirectoryQuery query(UUID assessmentId, String search, Integer page, Integer size) {
        return query(assessmentId, search, page, size, List.of());
    }

    private static ViewAssessmentDirectoryQuery query(UUID assessmentId, String search, Integer page, Integer size,
            List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return new ViewAssessmentDirectoryQuery(assessmentId, search, page, size, excludeUserIds);
    }
}
