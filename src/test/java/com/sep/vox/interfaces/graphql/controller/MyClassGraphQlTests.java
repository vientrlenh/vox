package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.ViewMyClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends "my classes" queries through the real schema and asserts the typed values: the
 * {@code SchoolClassStatus} enum and the {@code DateTime} scalar, on output and on input.
 */
@GraphQlControllerTest(MyClassController.class)
class MyClassGraphQlTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();

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
    void my_classes_should_return_status_enum_and_date_time_values() {
        when(viewMyClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(schoolClass()), 1, 10, 1L, 1));

        graphQlTester.document("{ myClasses(schoolId: \"%s\") { content { status createdAt updatedAt } } }"
                .formatted(SCHOOL_ID))
            .execute()
            .errors().verify()
            .path("myClasses.content[0].status").entity(String.class).isEqualTo("ACTIVE")
            .path("myClasses.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("myClasses.content[0].updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_classes_should_bind_the_status_variable_to_the_enum() {
        when(viewMyClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

        graphQlTester.document("query($status: SchoolClassStatus) { myClasses(schoolId: \"%s\", status: $status) { totalElements } }"
                .formatted(SCHOOL_ID))
            .variable("status", "ARCHIVED")
            .execute()
            .errors().verify();

        verify(viewMyClassesUseCase).execute(new ViewMyClassesQuery(SCHOOL_ID, null, SchoolClassStatus.ARCHIVED, 1, 10));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_classes_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ myClasses(schoolId: \"%s\", status: NOT_A_STATUS) { totalElements } }"
                .formatted(SCHOOL_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewMyClassesUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_class_members_should_return_joined_and_left_at_as_date_time() {
        SchoolClassUserDto member = new SchoolClassUserDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            false, Instant.parse("2026-01-05T01:00:00Z"), Instant.parse("2026-06-01T10:00:00Z"), null);
        when(viewMyClassMembersUseCase.execute(any())).thenReturn(new PageResult<>(List.of(member), 1, 20, 1L, 1));

        graphQlTester.document("{ myClassMembers(schoolClassId: \"%s\") { content { joinedAt leftAt } } }"
                .formatted(UUID.randomUUID()))
            .execute()
            .errors().verify()
            .path("myClassMembers.content[0].joinedAt").entity(String.class).isEqualTo("2026-01-05T01:00:00Z")
            .path("myClassMembers.content[0].leftAt").entity(String.class).isEqualTo("2026-06-01T10:00:00Z");
    }

    private SchoolClassDto schoolClass() {
        return new SchoolClassDto(UUID.randomUUID(), SCHOOL_ID, UUID.randomUUID(), UUID.randomUUID(), "ENG-01",
            "English 01", null, SchoolClassStatus.ACTIVE, Instant.parse("2026-07-28T02:30:00Z"), null);
    }
}
