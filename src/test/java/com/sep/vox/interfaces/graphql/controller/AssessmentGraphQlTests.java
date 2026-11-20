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
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.ViewAssessmentStatusCountsQuery;
import com.sep.vox.application.query.dto.AssessmentStatusCountsDto;
import com.sep.vox.application.port.input.usecase.assessment.CanViewAssessmentBlueprintDataUseCase;
import com.sep.vox.application.port.input.usecase.assessment.EstimateAssessmentTokenQuotaUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentRoleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.ViewAssessmentPaperDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
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
 * Sends assessment queries through the real schema and asserts the typed values: the
 * {@code DateTime} scalar and the assessment, member, paper and secure pool enums, on the assessment and on
 * the members, papers and secure pool nested under it (resolved through the real data loaders).
 */
@GraphQlControllerTest(AssessmentController.class)
@Import(AssessmentGraphQlDataLoaderConfig.class)
@MockitoBean(types = {
    SchoolRoomRepository.class, AssessmentRepository.class, AssessmentScheduleRepository.class,
    AssessmentBlueprintRepository.class, AssessmentBlueprintVersionRepository.class,
    AssessmentBlueprintSlotRepository.class, AssessmentPaperSectionRepository.class,
    AssessmentPaperItemRepository.class, AssessmentCandidateRepository.class, SchoolClassUserRepository.class,
    ResolveAssessmentCandidateAttemptsUseCase.class, ViewAssessmentPaperDetailsUseCase.class,
    ViewMyAssessmentRoleUseCase.class,
    CanViewAssessmentBlueprintDataUseCase.class, EstimateAssessmentTokenQuotaUseCase.class, UserContextPort.class })
class AssessmentGraphQlTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant OPEN_AT = Instant.parse("2026-08-01T01:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentsUseCase viewAssessmentsUseCase;
    @MockitoBean
    private ViewAssessmentDetailsUseCase viewAssessmentDetailsUseCase;
    @MockitoBean
    private ViewAssessmentStatusCountsUseCase viewAssessmentStatusCountsUseCase;
    @MockitoBean
    private AssessmentMemberRepository assessmentMemberRepository;
    @MockitoBean
    private AssessmentPaperRepository assessmentPaperRepository;
    @MockitoBean
    private AssessmentSecurePoolRepository assessmentSecurePoolRepository;

    private static final String ASSESSMENT_QUERY = """
        {
            assessment(id: "%s") {
                kind deliveryMode status resultDecisionMethod requiredStreamType streamTypePermission
                openAt closeAt createdAt updatedAt
            }
        }""".formatted(ASSESSMENT_ID);

    private AssessmentDto assessment() {
        return new AssessmentDto(ASSESSMENT_ID, null, null, "AS-01", "Midterm", null, UUID.randomUUID(), UUID.randomUUID(),
            AssessmentKind.CLASS_TEST, AssessmentDeliveryMode.LAB, AssessmentStatus.SCHEDULED, 1, 900,
            ResultDecisionMethod.HIGHEST, OPEN_AT, null, null, false, AssessmentRequiredStreamType.CAMERA_AND_SCREEN,
            AssessmentStreamTypePermission.ALL, null, CREATED_AT, null, null, null, null);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_should_return_enum_names_and_date_time_values() {
        when(viewAssessmentDetailsUseCase.execute(any())).thenReturn(assessment());

        graphQlTester.document(ASSESSMENT_QUERY).execute()
            .errors().verify()
            .path("assessment.kind").entity(String.class).isEqualTo("CLASS_TEST")
            .path("assessment.deliveryMode").entity(String.class).isEqualTo("LAB")
            .path("assessment.status").entity(String.class).isEqualTo("SCHEDULED")
            .path("assessment.resultDecisionMethod").entity(String.class).isEqualTo("HIGHEST")
            .path("assessment.requiredStreamType").entity(String.class).isEqualTo("CAMERA_AND_SCREEN")
            .path("assessment.streamTypePermission").entity(String.class).isEqualTo("ALL")
            .path("assessment.openAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessment.closeAt").valueIsNull()
            .path("assessment.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("assessment.updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_members_should_return_role_enum_and_granted_at_date_time() {
        when(viewAssessmentDetailsUseCase.execute(any())).thenReturn(assessment());
        when(assessmentMemberRepository.findByAssessmentIdIn(any())).thenReturn(List.of(
            new AssessmentMember(UUID.randomUUID(), ASSESSMENT_ID, UUID.randomUUID(), AssessmentMemberRole.CHAIR, CREATED_AT, null)));

        graphQlTester.document("{ assessment(id: \"%s\") { members { role grantedAt } } }".formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessment.members[0].role").entity(String.class).isEqualTo("CHAIR")
            .path("assessment.members[0].grantedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_papers_should_keep_only_the_papers_with_the_requested_status_enum() {
        when(viewAssessmentDetailsUseCase.execute(any())).thenReturn(assessment());
        when(assessmentPaperRepository.findByAssessmentIdIn(any())).thenReturn(List.of(
            paper("P-01", AssessmentPaperStatus.DRAFT), paper("P-02", AssessmentPaperStatus.LOCKED)));

        graphQlTester.document("""
            query($status: AssessmentPaperStatus) {
                assessment(id: "%s") { papers(status: $status) { code status createdAt } }
            }""".formatted(ASSESSMENT_ID)).variable("status", "LOCKED").execute()
            .errors().verify()
            .path("assessment.papers[*].code").entityList(String.class).containsExactly("P-02")
            .path("assessment.papers[0].status").entity(String.class).isEqualTo("LOCKED")
            .path("assessment.papers[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_secure_pool_should_return_enums_and_date_time_values() {
        when(viewAssessmentDetailsUseCase.execute(any())).thenReturn(assessment());
        when(assessmentSecurePoolRepository.findByAssessmentIdIn(any())).thenReturn(List.of(
            new AssessmentSecurePool(UUID.randomUUID(), ASSESSMENT_ID, AssessmentSecurePoolStatus.SEALED,
                AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, OPEN_AT, null, null, CREATED_AT, null, null, null)));

        graphQlTester.document("""
            { assessment(id: "%s") { securePool { status releaseMode embargoUntil releasedAt } } }""".formatted(ASSESSMENT_ID)).execute()
            .errors().verify()
            .path("assessment.securePool.status").entity(String.class).isEqualTo("SEALED")
            .path("assessment.securePool.releaseMode").entity(String.class).isEqualTo("AUTO_AFTER_CLOSE")
            .path("assessment.securePool.embargoUntil").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessment.securePool.releasedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assessment_papers_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ assessment(id: \"%s\") { papers(status: NOT_A_STATUS) { code } } }".formatted(ASSESSMENT_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewAssessmentDetailsUseCase, assessmentPaperRepository);
    }

    private static final String STATUS_COUNTS_QUERY = """
        query($dateFrom: DateTime, $dateTo: DateTime) {
            assessmentStatusCounts(kind: CENTRALIZED, dateFrom: $dateFrom, dateTo: $dateTo) { total draft }
        }""";

    @Test
    @WithMockUser(roles = "TEACHER")
    void status_counts_should_deliver_offset_date_time_variables_as_instants() {
        when(viewAssessmentStatusCountsUseCase.execute(any())).thenReturn(new AssessmentStatusCountsDto(3, 1, 0, 0, 0, 0, 0));

        graphQlTester.document(STATUS_COUNTS_QUERY)
            .variable("dateFrom", "2026-08-01T08:00:00+07:00").variable("dateTo", "2026-08-02T00:00:00Z").execute()
            .errors().verify()
            .path("assessmentStatusCounts.total").entity(Integer.class).isEqualTo(3);

        verify(viewAssessmentStatusCountsUseCase).execute(new ViewAssessmentStatusCountsQuery(
            null, AssessmentKind.CENTRALIZED, OPEN_AT, Instant.parse("2026-08-02T00:00:00Z")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void status_counts_should_reject_a_date_without_an_offset() {
        graphQlTester.document(STATUS_COUNTS_QUERY).variable("dateFrom", "2026-08-01").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("dateFrom"))
            .verify();

        verifyNoInteractions(viewAssessmentStatusCountsUseCase);
    }

    private AssessmentPaper paper(String code, AssessmentPaperStatus status) {
        return new AssessmentPaper(UUID.randomUUID(), ASSESSMENT_ID, null, code, 1, status, null, CREATED_AT, null, null, null);
    }
}
