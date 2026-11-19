package com.sep.vox.interfaces.graphql.controller;

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

import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewMyExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.application.response.input.examschedule.ProctorBusySlotResponse;
import com.sep.vox.application.response.input.examschedule.StudentBusySlotResponse;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the {@link ExamScheduleController} queries through the real schema and {@code @PreAuthorize}, to prove
 * that {@code myExamSchedules} is student only and the three planning queries are school admin or teacher only.
 * The direct-call behaviour stays covered by {@link ExamScheduleControllerTests}.
 *
 * <p>{@code updateExamSchedule} is not covered here: it has no {@code @PreAuthorize}, so there is no role rule
 * at the controller to test.
 */
@GraphQlControllerTest(ExamScheduleController.class)
@MockitoBean(types = { ExamScheduleProctorRepository.class, ExamCandidateRepository.class })
class ExamScheduleAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String MY_EXAM_SCHEDULES_QUERY = "{ myExamSchedules { id } }";
    private static final String EXAM_SCHEDULES_QUERY = "{ examSchedules { id } }";
    private static final String PROCTOR_BUSY_SLOTS_QUERY =
        "{ proctorBusySlots(scheduleId: \"%s\", teacherIds: [\"%s\"]) { teacherId } }".formatted(ID, ID);
    private static final String STUDENT_BUSY_SLOTS_QUERY =
        "{ studentBusySlots(scheduleIds: [\"%s\"], studentIds: [\"%s\"]) { studentId } }".formatted(ID, ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewExamSchedulesUseCase viewExamSchedulesUseCase;
    @MockitoBean
    private ViewMyExamSchedulesUseCase viewMyExamSchedulesUseCase;
    @MockitoBean
    private UpdateExamScheduleUseCase updateExamScheduleUseCase;
    @MockitoBean
    private ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase;
    @MockitoBean
    private ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;

    private static ExamScheduleDto scheduleWithOnlyId() {
        return new ExamScheduleDto(ID, UUID.randomUUID(), null, null, null, "DRAFT", null);
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
        verifyNoInteractions(viewExamSchedulesUseCase, viewMyExamSchedulesUseCase, updateExamScheduleUseCase,
            viewProctorBusySlotsUseCase, viewStudentBusySlotsUseCase);
    }

    private static Stream<String> everyQuery() {
        return Stream.of(MY_EXAM_SCHEDULES_QUERY, EXAM_SCHEDULES_QUERY, PROCTOR_BUSY_SLOTS_QUERY,
            STUDENT_BUSY_SLOTS_QUERY);
    }

    private static Stream<String> schoolAdminOrTeacherQueries() {
        return Stream.of(EXAM_SCHEDULES_QUERY, PROCTOR_BUSY_SLOTS_QUERY, STUDENT_BUSY_SLOTS_QUERY);
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
    void my_exam_schedules_should_be_forbidden_for_school_admin() {
        assertErrorType(MY_EXAM_SCHEDULES_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void my_exam_schedules_should_be_forbidden_for_teacher() {
        assertErrorType(MY_EXAM_SCHEDULES_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_exam_schedules_should_return_data_for_student() {
        when(viewMyExamSchedulesUseCase.execute(any())).thenReturn(List.of(scheduleWithOnlyId()));

        assertFirstElementIsReturned(MY_EXAM_SCHEDULES_QUERY, "myExamSchedules", "id");
    }

    private void assertPlanningQueriesReturnData() {
        when(viewExamSchedulesUseCase.execute(any())).thenReturn(List.of(scheduleWithOnlyId()));
        when(viewProctorBusySlotsUseCase.execute(any()))
            .thenReturn(List.of(new ProctorBusySlotResponse(ID, ID, null, null)));
        when(viewStudentBusySlotsUseCase.execute(any()))
            .thenReturn(List.of(new StudentBusySlotResponse(ID, ID, ID, null, null)));

        assertFirstElementIsReturned(EXAM_SCHEDULES_QUERY, "examSchedules", "id");
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
