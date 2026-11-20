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
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
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
class ExamAppealAccessClassTestTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID OTHER_TEACHER_ID = UUID.randomUUID();

    private ExamGradingAccessService examGradingAccessService;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ExamAppealAccessService service;

    @BeforeEach
    void setUp() {
        examGradingAccessService = mock(ExamGradingAccessService.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        service = new ExamAppealAccessService(
            mock(ExamResultAppealRepository.class),
            mock(ExamCandidateResultRepository.class),
            mock(ExamSessionRepository.class),
            mock(ExamCandidateRepository.class),
            mock(ExamRepository.class),
            mock(UserRepository.class),
            mock(SchoolUserRepository.class),
            userRoleQueryRepository,
            userContextPort,
            examGradingAccessService);

        when(examGradingAccessService.isClassTestChair(eq(EXAM_ID), eq(CHAIR_ID))).thenReturn(true);
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

    private ExamAppealAccessService.AppealContext context() {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        result.setExamId(EXAM_ID);
        return new ExamAppealAccessService.AppealContext(
            null, result, null, SCHOOL_ID, UUID.randomUUID(), "Kiểm tra 15 phút");
    }
}
