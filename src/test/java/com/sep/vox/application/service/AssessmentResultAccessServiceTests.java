package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Chốt hai điều dễ sai nhất khi gộp "ai được xem" với "khi nào được xem":
 * cờ {@code candidateOwner} chỉ bật cho đúng thí sinh của bài (system admin KHÔNG phải
 * chính chủ), và luật trạng thái chỉ áp cho chính chủ — giáo viên vẫn phải đọc được bài
 * chưa công bố, nếu không thì không còn gì để mà chấm.
 */
class AssessmentResultAccessServiceTests {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssessmentResultAccessService service;

    @BeforeEach
    void setUp() {
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        service = new AssessmentResultAccessService(
            assessmentSessionRepository,
            mock(AssessmentItemResponseRepository.class),
            assessmentCandidateRepository,
            assessmentCandidateResultRepository,
            assessmentRepository,
            assessmentMemberRepository,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort
        );

        AssessmentSession session = new AssessmentSession(SESSION_ID, ASSESSMENT_ID, CANDIDATE_ID, UUID.randomUUID(),
            Instant.now(), Instant.now(), AssessmentSessionStatus.GRADED, false, null);
        when(assessmentSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(assessmentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(new AssessmentCandidate(
            CANDIDATE_ID, ASSESSMENT_ID, STUDENT_ID, null, null, AssessmentCandidateStatus.ATTENDED,
            Instant.now(), Instant.now(), null, null, null)));
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setSchoolId(SCHOOL_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(schoolUserRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());
    }

    @Test
    void should_report_candidate_owner_when_current_user_is_the_student() {
        givenCurrentUser(STUDENT_ID, false);

        AssessmentResultAccessService.SessionAccess access = service.authorizeSession(SESSION_ID);

        assertThat(access.candidateOwner()).isTrue();
        assertThat(access.session().getId()).isEqualTo(SESSION_ID);
    }

    /**
     * System admin thoát sớm khỏi nhánh phân quyền — nếu vì thế mà bị coi là chính chủ thì
     * họ sẽ mất quyền xem bài chưa công bố, đúng thứ họ cần nhất.
     */
    @Test
    void should_not_report_candidate_owner_for_system_admin() {
        givenCurrentUser(UUID.randomUUID(), true);

        assertThat(service.authorizeSession(SESSION_ID).candidateOwner()).isFalse();
    }

    @Test
    void should_not_report_candidate_owner_for_school_admin() {
        UUID adminId = UUID.randomUUID();
        givenCurrentUser(adminId, false);
        givenSchoolAdmin(adminId);

        assertThat(service.authorizeSession(SESSION_ID).candidateOwner()).isFalse();
    }

    @Test
    void should_reject_outsider() {
        givenCurrentUser(UUID.randomUUID(), false);

        assertThatThrownBy(() -> service.authorizeSession(SESSION_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_candidate_when_result_is_pending_review() {
        givenCurrentUser(STUDENT_ID, false);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> service.requireCandidateVisibleSession(SESSION_ID))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("chưa được công bố");
    }

    @Test
    void should_allow_candidate_when_result_is_released() {
        givenCurrentUser(STUDENT_ID, false);
        givenResult(AssessmentCandidateResultStatus.RELEASED);

        assertThat(service.requireCandidateVisibleSession(SESSION_ID).candidateOwner()).isTrue();
    }

    @Test
    void should_allow_school_admin_when_result_is_pending_review() {
        UUID adminId = UUID.randomUUID();
        givenCurrentUser(adminId, false);
        givenSchoolAdmin(adminId);
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        assertThat(service.requireCandidateVisibleSession(SESSION_ID).candidateOwner()).isFalse();
    }

    /** Chưa có dòng kết quả nào thì chưa có gì để công bố — chính chủ không qua được. */
    @Test
    void should_reject_candidate_when_result_row_is_missing() {
        givenCurrentUser(STUDENT_ID, false);
        when(assessmentCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireCandidateVisibleSession(SESSION_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    private void givenCurrentUser(UUID userId, boolean systemAdmin) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userContextPort.isSystemAdmin()).thenReturn(systemAdmin);
    }

    private void givenSchoolAdmin(UUID adminId) {
        SchoolUser schoolUser = new com.sep.vox.domain.model.school.SchoolUser();
        schoolUser.setSchoolId(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(adminId)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new com.sep.vox.application.query.dto.UserRoleInfo(
                UUID.randomUUID(), adminId, UUID.randomUUID(), Instant.now(),
                "SCHOOL_ADMIN", "School Admin")));
    }

    private void givenResult(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult(
            UUID.randomUUID(), ASSESSMENT_ID, CANDIDATE_ID, SESSION_ID, UUID.randomUUID(), 1,
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            new BigDecimal("7.00"), status, null, null, Instant.now(), Instant.now(), null, null);
        when(assessmentCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(result));
    }
}
