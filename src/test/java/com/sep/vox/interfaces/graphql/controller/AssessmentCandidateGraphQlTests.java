package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.ViewAssessmentCandidatesQuery;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidatesAttendanceUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewAssessmentCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.dto.AssessmentCandidateAttempts;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.interfaces.graphql.config.AssessmentGraphQlDataLoaderConfig;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends candidate queries through the real schema and asserts the typed values: the {@code DateTime} scalar and the
 * {@code AssessmentCandidateStatus} and {@code AssessmentSessionStatus} enums, on the candidate and on the attempts
 * resolved through the real data loader.
 */
@GraphQlControllerTest(AssessmentCandidateController.class)
@Import(AssessmentGraphQlDataLoaderConfig.class)
@MockitoBean(types = {
    SchoolRoomRepository.class, AssessmentRepository.class, AssessmentScheduleRepository.class,
    AssessmentPaperRepository.class, AssessmentBlueprintRepository.class, AssessmentBlueprintVersionRepository.class,
    AssessmentBlueprintSlotRepository.class, AssessmentMemberRepository.class, AssessmentPaperSectionRepository.class,
    AssessmentPaperItemRepository.class, AssessmentSecurePoolRepository.class, AssessmentCandidateRepository.class,
    SchoolClassUserRepository.class, UpdateAssessmentCandidateStatusUseCase.class,
    UpdateAssessmentCandidatesAttendanceUseCase.class })
class AssessmentCandidateGraphQlTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final Instant ASSIGNED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-08-01T01:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentCandidatesUseCase viewAssessmentCandidatesUseCase;
    @MockitoBean
    private ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase;

    private static final String CANDIDATES_QUERY = """
        query($status: AssessmentCandidateStatus) {
            assessmentCandidates(assessmentId: "%s", status: $status) { status assignedAt updatedAt blockedAt }
        }""".formatted(ASSESSMENT_ID);

    private static AssessmentCandidateDto candidate() {
        return new AssessmentCandidateDto(CANDIDATE_ID, ASSESSMENT_ID, UUID.randomUUID(), null, null,
            AssessmentCandidateStatus.ATTENDED, ASSIGNED_AT, null, null);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void candidates_should_return_status_enum_and_date_time_values() {
        when(viewAssessmentCandidatesUseCase.execute(any())).thenReturn(List.of(candidate()));

        graphQlTester.document(CANDIDATES_QUERY).execute()
            .errors().verify()
            .path("assessmentCandidates[0].status").entity(String.class).isEqualTo("ATTENDED")
            .path("assessmentCandidates[0].assignedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("assessmentCandidates[0].updatedAt").valueIsNull()
            .path("assessmentCandidates[0].blockedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void candidates_should_bind_the_status_variable_as_an_enum() {
        when(viewAssessmentCandidatesUseCase.execute(any())).thenReturn(List.of());

        graphQlTester.document(CANDIDATES_QUERY).variable("status", "ABSENT").execute().errors().verify();

        verify(viewAssessmentCandidatesUseCase).execute(new ViewAssessmentCandidatesQuery(ASSESSMENT_ID, null, AssessmentCandidateStatus.ABSENT));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void candidates_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ assessmentCandidates(assessmentId: \"%s\", status: NOT_A_STATUS) { id } }".formatted(ASSESSMENT_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewAssessmentCandidatesUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void candidate_attempts_should_return_date_time_values_and_the_session_status_enum() {
        when(viewAssessmentCandidatesUseCase.execute(any())).thenReturn(List.of(candidate()));
        AssessmentAttemptSummary attempt = new AssessmentAttemptSummary(CANDIDATE_ID, ASSESSMENT_ID,
            AssessmentCandidateStatus.ATTENDED, UUID.randomUUID(), STARTED_AT, null, AssessmentSessionStatus.IN_PROGRESS,
            false, null, BigDecimal.TEN, null, null, null, null, null, null, null);
        when(resolveAssessmentCandidateAttemptsUseCase.executeBatch(any())).thenReturn(
            Map.of(CANDIDATE_ID, new AssessmentCandidateAttempts(List.of(attempt), attempt, BigDecimal.TEN)));

        graphQlTester.document("""
            { assessmentCandidates(assessmentId: "%s") { attempts { status startedAt submittedAt } } }"""
            .formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessmentCandidates[0].attempts[0].status").entity(String.class).isEqualTo("IN_PROGRESS")
            .path("assessmentCandidates[0].attempts[0].startedAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentCandidates[0].attempts[0].submittedAt").valueIsNull();
    }
}
