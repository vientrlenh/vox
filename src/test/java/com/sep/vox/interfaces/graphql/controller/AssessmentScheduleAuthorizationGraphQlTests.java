package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewMyAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.application.response.input.assessmentschedule.ProctorBusySlotResponse;
import com.sep.vox.application.response.input.assessmentschedule.StudentBusySlotResponse;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the {@link AssessmentScheduleController} queries through the real schema and {@code @PreAuthorize}, to prove
 * that {@code myAssessmentSchedules} is student only and the three planning queries are school admin or teacher only.
 * The direct-call behaviour stays covered by {@link AssessmentScheduleControllerTests}.
 *
 * <p>{@code updateAssessmentSchedule} is not covered here: it has no {@code @PreAuthorize}, so there is no role rule
 * at the controller to test.
 */
@GraphQlControllerTest(AssessmentScheduleController.class)
@MockitoBean(types = { AssessmentScheduleProctorRepository.class, AssessmentCandidateRepository.class })
class AssessmentScheduleAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String MY_ASSESSMENT_SCHEDULES_QUERY = "{ myAssessmentSchedules { id } }";
    private static final String ASSESSMENT_SCHEDULES_QUERY = "{ assessmentSchedules { id } }";
    private static final String PROCTOR_BUSY_SLOTS_QUERY =
        "{ proctorBusySlots(scheduleId: \"%s\", teacherIds: [\"%s\"]) { teacherId } }".formatted(ID, ID);
    private static final String STUDENT_BUSY_SLOTS_QUERY =
        "{ studentBusySlots(scheduleIds: [\"%s\"], studentIds: [\"%s\"]) { studentId } }".formatted(ID, ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentSchedulesUseCase viewAssessmentSchedulesUseCase;
    @MockitoBean
    private ViewMyAssessmentSchedulesUseCase viewMyAssessmentSchedulesUseCase;
    @MockitoBean
    private UpdateAssessmentScheduleUseCase updateAssessmentScheduleUseCase;
    @MockitoBean
    private ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase;
    @MockitoBean
    private ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;

    private static AssessmentScheduleDto scheduleWithOnlyId() {
        return new AssessmentScheduleDto(ID, UUID.randomUUID(), null, null, null, AssessmentScheduleStatus.DRAFT, null);
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private void assertFirstElementIsReturned(String document, String operationName, String fieldName) {
        graphQlTester.document(document).execute()
            .errors().verify()
            .path("%s[0].%s".formatted(operationName, fieldName)).entity(String.class).isEqualTo(ID.toString());
    }

    private void verifyNoUseCaseWasCalled() {
        verifyNoInteractions(viewAssessmentSchedulesUseCase, viewMyAssessmentSchedulesUseCase, updateAssessmentScheduleUseCase,
            viewProctorBusySlotsUseCase, viewStudentBusySlotsUseCase);
    }

    private static Stream<String> everyQuery() {
        return Stream.of(MY_ASSESSMENT_SCHEDULES_QUERY, ASSESSMENT_SCHEDULES_QUERY, PROCTOR_BUSY_SLOTS_QUERY,
            STUDENT_BUSY_SLOTS_QUERY);
    }

    private static Stream<String> schoolAdminOrTeacherQueries() {
        return Stream.of(ASSESSMENT_SCHEDULES_QUERY, PROCTOR_BUSY_SLOTS_QUERY, STUDENT_BUSY_SLOTS_QUERY);
    }

    @ParameterizedTest
    @MethodSource("everyQuery")
    @WithAnonymousUser
    void every_query_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("schoolAdminOrTeacherQueries")
    @WithMockUser(roles = "STUDENT")
    void school_admin_or_teacher_queries_should_be_forbidden_for_student(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void my_assessment_schedules_should_be_forbidden_for_school_admin() {
        assertErrorType(MY_ASSESSMENT_SCHEDULES_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_assessment_schedules_should_be_forbidden_for_teacher() {
        assertErrorType(MY_ASSESSMENT_SCHEDULES_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_assessment_schedules_should_return_data_for_student() {
        when(viewMyAssessmentSchedulesUseCase.execute(any())).thenReturn(List.of(scheduleWithOnlyId()));

        assertFirstElementIsReturned(MY_ASSESSMENT_SCHEDULES_QUERY, "myAssessmentSchedules", "id");
    }

    private void assertPlanningQueriesReturnData() {
        when(viewAssessmentSchedulesUseCase.execute(any())).thenReturn(List.of(scheduleWithOnlyId()));
        when(viewProctorBusySlotsUseCase.execute(any()))
            .thenReturn(List.of(new ProctorBusySlotResponse(ID, ID, null, null)));
        when(viewStudentBusySlotsUseCase.execute(any()))
            .thenReturn(List.of(new StudentBusySlotResponse(ID, ID, ID, null, null)));

        assertFirstElementIsReturned(ASSESSMENT_SCHEDULES_QUERY, "assessmentSchedules", "id");
        assertFirstElementIsReturned(PROCTOR_BUSY_SLOTS_QUERY, "proctorBusySlots", "teacherId");
        assertFirstElementIsReturned(STUDENT_BUSY_SLOTS_QUERY, "studentBusySlots", "studentId");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void planning_queries_should_return_data_for_school_admin() {
        assertPlanningQueriesReturnData();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void planning_queries_should_return_data_for_teacher() {
        assertPlanningQueriesReturnData();
    }
}
