package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryStudentsUseCase;
import com.sep.vox.application.query.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends the assessment directory queries through the real schema and asserts the typed values: the
 * {@code SchoolClassStatus}, {@code SchoolGradeStatus} and {@code UserStatus} enums. Authorization is covered by
 * {@link AssessmentDirectoryAuthorizationGraphQlTests}.
 */
@GraphQlControllerTest(AssessmentDirectoryController.class)
class AssessmentDirectoryGraphQlTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();

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

    @Test
    @WithMockUser(roles = "TEACHER")
    void classes_should_return_the_status_enum() {
        SchoolClassDto schoolClass = new SchoolClassDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "10A1", "Class 10A1", null, SchoolClassStatus.ACTIVE, Instant.parse("2026-07-28T02:30:00Z"), null);
        when(viewAssessmentDirectoryClassesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(schoolClass), 1, 20, 1L, 1));

        graphQlTester.document("{ assessmentDirectoryClasses(assessmentId: \"%s\") { content { code status } } }"
            .formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessmentDirectoryClasses.content[0].status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void grades_should_return_the_status_enum() {
        when(viewAssessmentDirectoryGradesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(
            new AssessmentDirectoryGradeInfo(UUID.randomUUID(), "2026", "School year 2026", SchoolGradeStatus.ACTIVE)), 1, 20, 1L, 1));

        graphQlTester.document("{ assessmentDirectoryGrades(assessmentId: \"%s\") { content { code status } } }"
            .formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessmentDirectoryGrades.content[0].status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void students_and_proctors_should_return_the_user_status_enum() {
        AssessmentDirectoryUserInfo user = new AssessmentDirectoryUserInfo(UUID.randomUUID(), "An Nguyen", "an@example.com", UserStatus.ACTIVE);
        when(viewAssessmentDirectoryStudentsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(user), 1, 20, 1L, 1));
        when(viewAssessmentDirectoryProctorsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(user), 1, 20, 1L, 1));

        graphQlTester.document("""
            { assessmentDirectoryStudents(assessmentId: "%1$s") { content { status } }
              assessmentDirectoryProctors(assessmentId: "%1$s") { content { status } } }""".formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessmentDirectoryStudents.content[0].status").entity(String.class).isEqualTo("ACTIVE")
            .path("assessmentDirectoryProctors.content[0].status").entity(String.class).isEqualTo("ACTIVE");
    }
}
