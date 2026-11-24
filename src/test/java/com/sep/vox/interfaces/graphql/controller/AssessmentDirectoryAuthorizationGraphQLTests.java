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

import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryStudentsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Runs the assessment directory queries through the real schema and {@code @PreAuthorize}, to prove that only
 * the two assessment-organising roles get through. {@link AssessmentDirectoryAuthorizationBoundaryTests} only reads
 * the annotation values.
 */
@GraphQLControllerTest(GraphQLAssessmentDirectoryController.class)
class AssessmentDirectoryAuthorizationGraphQLTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentDirectoryClassesUseCase viewAssessmentDirectoryClassesUseCase;
    @MockitoBean
    private ViewAssessmentDirectoryGradesUseCase viewAssessmentDirectoryGradesUseCase;
    @MockitoBean
    private ViewAssessmentDirectoryStudentsUseCase viewAssessmentDirectoryStudentsUseCase;
    @MockitoBean
    private ViewAssessmentDirectoryProctorsUseCase viewAssessmentDirectoryProctorsUseCase;

    private static String document(String queryName) {
        return "{ %s(assessmentId: \"%s\") { totalElements } }".formatted(queryName, UUID.randomUUID());
    }

    private void assertAssessmentDirectoryClassesReturnsData() {
        when(viewAssessmentDirectoryClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 20, 0L, 0));

        graphQlTester.document(document("assessmentDirectoryClasses")).execute()
            .errors().verify()
            .path("assessmentDirectoryClasses.totalElements").entity(Integer.class).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_directory_classes_should_return_data_for_teacher() {
        assertAssessmentDirectoryClassesReturnsData();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assessment_directory_classes_should_return_data_for_school_admin() {
        assertAssessmentDirectoryClassesReturnsData();
    }

    @ParameterizedTest
    @ValueSource(strings = { "assessmentDirectoryClasses", "assessmentDirectoryGrades", "assessmentDirectoryStudents",
        "assessmentDirectoryProctors" })
    @WithMockUser(roles = "STUDENT")
    void every_assessment_directory_query_should_be_forbidden_for_student(String queryName) {
        graphQlTester.document(document(queryName)).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.FORBIDDEN).verify();

        verifyNoInteractions(viewAssessmentDirectoryClassesUseCase, viewAssessmentDirectoryGradesUseCase,
            viewAssessmentDirectoryStudentsUseCase, viewAssessmentDirectoryProctorsUseCase);
    }

    @Test
    @WithAnonymousUser
    void assessment_directory_classes_should_be_unauthorized_without_authentication() {
        graphQlTester.document(document("assessmentDirectoryClasses")).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.UNAUTHORIZED).verify();

        verifyNoInteractions(viewAssessmentDirectoryClassesUseCase);
    }
}
