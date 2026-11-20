package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Bài kiểm tra trên lớp: nhà trường không chấm, nên chính giáo viên tạo bài duyệt /
 * từ chối đơn và tự nhận chấm phúc khảo.
 *
 * <p>Bề mặt REST + GraphQL của phúc khảo đã nới sang {@code TEACHER}, nên chốt quyền
 * thật nằm ở đây — ca {@code should_reject_a_teacher_who_is_not_the_chair} là thứ giữ
 * cho việc nới đó không biến thành "mọi giáo viên đọc mọi đơn của trường".
 */
class AssessmentAppealAccessClassTestTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID OTHER_TEACHER_ID = UUID.randomUUID();

    private AssessmentGradingAccessService assessmentGradingAccessService;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssessmentAppealAccessService service;

    @BeforeEach
    void setUp() {
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        service = new AssessmentAppealAccessService(
            mock(AssessmentResultAppealRepository.class),
            mock(AssessmentCandidateResultRepository.class),
            mock(AssessmentSessionRepository.class),
            mock(AssessmentCandidateRepository.class),
            mock(AssessmentRepository.class),
            mock(UserRepository.class),
            mock(SchoolUserRepository.class),
            userRoleQueryRepository,
            userContextPort,
            assessmentGradingAccessService);

        when(assessmentGradingAccessService.isClassTestChair(eq(ASSESSMENT_ID), eq(CHAIR_ID))).thenReturn(true);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(OTHER_TEACHER_ID)).thenReturn(java.util.List.of());
    }

    @Test
    void should_let_the_class_test_chair_through() {
        assertThatCode(() -> service.authorizeSchoolAdminOrClassTestChair(context(), CHAIR_ID))
            .doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_teacher_who_is_not_the_chair() {
        assertThatThrownBy(() -> service.authorizeSchoolAdminOrClassTestChair(context(), OTHER_TEACHER_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    private AssessmentAppealAccessService.AppealContext context() {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        result.setAssessmentId(ASSESSMENT_ID);
        return new AssessmentAppealAccessService.AppealContext(
            null, result, null, SCHOOL_ID, UUID.randomUUID(), "Kiểm tra 15 phút");
    }
}
