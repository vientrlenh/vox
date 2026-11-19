package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryStudentsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the exam directory queries through the real schema and {@code @PreAuthorize}, to prove that only
 * the two exam-organising roles get through. {@link ExamDirectoryAuthorizationBoundaryTests} only reads
 * the annotation values.
 */
@GraphQlControllerTest(ExamDirectoryController.class)
class ExamDirectoryAuthorizationGraphQlTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewExamDirectoryClassesUseCase viewExamDirectoryClassesUseCase;
    @MockitoBean
    private ViewExamDirectoryGradesUseCase viewExamDirectoryGradesUseCase;
    @MockitoBean
    private ViewExamDirectoryStudentsUseCase viewExamDirectoryStudentsUseCase;
    @MockitoBean
    private ViewExamDirectoryProctorsUseCase viewExamDirectoryProctorsUseCase;

    private static String document(String queryName) {
        return "{ %s(examId: \"%s\") { totalElements } }".formatted(queryName, UUID.randomUUID());
    }

    private void assertExamDirectoryClassesReturnsData() {
        when(viewExamDirectoryClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 20, 0L, 0));

        graphQlTester.document(document("examDirectoryClasses")).execute()
            .errors().verify()
            .path("examDirectoryClasses.totalElements").entity(Integer.class).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void exam_directory_classes_should_return_data_for_teacher() {
        assertExamDirectoryClassesReturnsData();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void exam_directory_classes_should_return_data_for_school_admin() {
        assertExamDirectoryClassesReturnsData();
    }

    @ParameterizedTest
    @ValueSource(strings = { "examDirectoryClasses", "examDirectoryGrades", "examDirectoryStudents",
        "examDirectoryProctors" })
    @WithMockUser(roles = "STUDENT")
    void every_exam_directory_query_should_be_forbidden_for_student(String queryName) {
        graphQlTester.document(document(queryName)).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.FORBIDDEN).verify();

        verifyNoInteractions(viewExamDirectoryClassesUseCase, viewExamDirectoryGradesUseCase,
            viewExamDirectoryStudentsUseCase, viewExamDirectoryProctorsUseCase);
    }

    @Test
    @WithAnonymousUser
    void exam_directory_classes_should_be_unauthorized_without_authentication() {
        graphQlTester.document(document("examDirectoryClasses")).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.UNAUTHORIZED).verify();

        verifyNoInteractions(viewExamDirectoryClassesUseCase);
    }
}
