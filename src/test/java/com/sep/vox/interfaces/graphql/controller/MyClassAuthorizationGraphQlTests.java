package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the "my classes" queries through the real schema and {@code @PreAuthorize}, to prove the role check
 * is actually enforced. {@link MyClassAuthorizationBoundaryTests} only reads the annotation values.
 */
@GraphQlControllerTest(MyClassController.class)
class MyClassAuthorizationGraphQlTests {

    private static final String MY_CLASSES_QUERY =
        "{ myClasses(schoolId: \"%s\") { totalElements } }".formatted(UUID.randomUUID());

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewMyClassesUseCase viewMyClassesUseCase;
    @MockitoBean
    private ViewMyClassDetailsUseCase viewMyClassDetailsUseCase;
    @MockitoBean
    private ViewMyClassMembersUseCase viewMyClassMembersUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_classes_should_return_data_for_teacher() {
        when(viewMyClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

        graphQlTester.document(MY_CLASSES_QUERY).execute()
            .errors().verify()
            .path("myClasses.totalElements").entity(Integer.class).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_classes_should_be_forbidden_for_student() {
        graphQlTester.document(MY_CLASSES_QUERY).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.FORBIDDEN).verify();

        verifyNoInteractions(viewMyClassesUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void my_classes_should_be_forbidden_for_school_admin() {
        graphQlTester.document(MY_CLASSES_QUERY).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.FORBIDDEN).verify();

        verifyNoInteractions(viewMyClassesUseCase);
    }

    @Test
    @WithAnonymousUser
    void my_classes_should_be_unauthorized_without_authentication() {
        graphQlTester.document(MY_CLASSES_QUERY).execute()
            .errors().expect(error -> error.getErrorType() == ErrorType.UNAUTHORIZED).verify();

        verifyNoInteractions(viewMyClassesUseCase);
    }
}
