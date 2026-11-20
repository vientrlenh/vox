package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
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

import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidatesAttendanceUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewAssessmentCandidatesUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the two {@link AssessmentCandidateController} mutations through the real schema and {@code @PreAuthorize},
 * to prove that only school admins and teachers get through. The direct-call behaviour stays covered by
 * {@link AssessmentCandidateControllerTests}.
 *
 * <p>The {@code assessmentCandidates} query is not covered here: it has no {@code @PreAuthorize} by design, because
 * {@code ViewAssessmentCandidatesUseCase} decides per assessment (school admin of the school, assessment chair, or proctor of the
 * schedule), which a role check cannot express.
 */
@GraphQlControllerTest(AssessmentCandidateController.class)
class AssessmentCandidateAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String UPDATE_STATUS_MUTATION =
        "mutation { updateAssessmentCandidateStatus(candidateId: \"%s\", status: ATTENDED) }".formatted(ID);
    private static final String UPDATE_ATTENDANCE_MUTATION =
        "mutation { updateAssessmentCandidatesAttendance(scheduleId: \"%s\", candidateIds: [\"%s\"]) { id } }"
            .formatted(ID, ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentCandidatesUseCase viewAssessmentCandidatesUseCase;
    @MockitoBean
    private UpdateAssessmentCandidateStatusUseCase updateAssessmentCandidateStatusUseCase;
    @MockitoBean
    private UpdateAssessmentCandidatesAttendanceUseCase updateAssessmentCandidatesAttendanceUseCase;

    private static AssessmentCandidateDto candidateWithOnlyId() {
        return new AssessmentCandidateDto(ID, UUID.randomUUID(), UUID.randomUUID(), null, null, AssessmentCandidateStatus.ATTENDED, null, null,
            null);
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private void verifyNoUseCaseWasCalled() {
        verifyNoInteractions(viewAssessmentCandidatesUseCase, updateAssessmentCandidateStatusUseCase,
            updateAssessmentCandidatesAttendanceUseCase);
    }

    private void assertBothMutationsReturnData() {
        when(updateAssessmentCandidateStatusUseCase.execute(any())).thenReturn(candidateWithOnlyId());
        when(updateAssessmentCandidatesAttendanceUseCase.execute(any())).thenReturn(List.of(candidateWithOnlyId()));

        graphQlTester.document(UPDATE_STATUS_MUTATION).execute()
            .errors().verify()
            .path("updateAssessmentCandidateStatus").entity(String.class).isEqualTo(ID.toString());
        graphQlTester.document(UPDATE_ATTENDANCE_MUTATION).execute()
            .errors().verify()
            .path("updateAssessmentCandidatesAttendance[0].id").entity(String.class).isEqualTo(ID.toString());
    }

    private static Stream<String> everyMutation() {
        return Stream.of(UPDATE_STATUS_MUTATION, UPDATE_ATTENDANCE_MUTATION);
    }

    @ParameterizedTest
    @MethodSource("everyMutation")
    @WithAnonymousUser
    void every_mutation_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("everyMutation")
    @WithMockUser(roles = "STUDENT")
    void every_mutation_should_be_forbidden_for_student(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("everyMutation")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void every_mutation_should_be_forbidden_for_system_admin(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void mutations_should_return_data_for_school_admin() {
        assertBothMutationsReturnData();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void mutations_should_return_data_for_teacher() {
        assertBothMutationsReturnData();
    }
}
