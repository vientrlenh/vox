package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewQuestionStatusCountsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionStatusCountsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;

class ViewQuestionStatusCountsUseCaseTests {

    private QuestionQueryRepository questionQueryRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ViewQuestionStatusCountsUseCase useCase;

    private UUID userId;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        questionQueryRepository = mock(QuestionQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        useCase = new ViewQuestionStatusCountsUseCase(
            questionQueryRepository, userContextPort, schoolUserRepository, userRoleQueryRepository);

        userId = UUID.randomUUID();
        schoolId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());

        var schoolUser = new SchoolUser();
        schoolUser.setSchoolId(schoolId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(questionQueryRepository.countAccessibleByStatus(
            any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
    }

    /**
     * GROUP BY chỉ trả về status có câu hỏi. Client vẽ biểu đồ cần trục ổn định, nên use
     * case phải bù đủ -- đây là phần dễ quên nhất của cả tính năng.
     */
    @Test
    void should_return_every_status_even_when_query_returns_nothing() {
        var result = useCase.execute(query());

        assertThat(result).hasSize(QuestionStatus.values().length);
        assertThat(result).extracting(QuestionStatusCountInfo::status)
            .containsExactly(QuestionStatus.values());
        assertThat(result).allMatch(row -> row.count() == 0L);
    }

    @Test
    void should_keep_counted_values_and_zero_fill_the_rest() {
        when(questionQueryRepository.countAccessibleByStatus(
            any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(
                new QuestionStatusCountInfo(QuestionStatus.PUBLISHED, 12L),
                new QuestionStatusCountInfo(QuestionStatus.DRAFT, 3L)));

        var result = useCase.execute(query());

        assertThat(result).hasSize(QuestionStatus.values().length);
        assertThat(result).contains(
            new QuestionStatusCountInfo(QuestionStatus.PUBLISHED, 12L),
            new QuestionStatusCountInfo(QuestionStatus.DRAFT, 3L),
            new QuestionStatusCountInfo(QuestionStatus.ARCHIVED, 0L));
    }

    /** Thứ tự phải bám enum, không bám thứ tự DB trả về -- client hiển thị thẳng theo đó. */
    @Test
    void should_order_by_status_lifecycle_not_by_query_result_order() {
        when(questionQueryRepository.countAccessibleByStatus(
            any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(
                new QuestionStatusCountInfo(QuestionStatus.ARCHIVED, 1L),
                new QuestionStatusCountInfo(QuestionStatus.DRAFT, 2L)));

        var result = useCase.execute(query());

        assertThat(result).extracting(QuestionStatusCountInfo::status)
            .containsExactly(QuestionStatus.values());
    }

    /** Keyword và topicName phải xuống repository ở dạng LIKE pattern, giống ViewQuestionsUseCase. */
    @Test
    void should_pass_like_patterns_down_to_the_repository() {
        useCase.execute(new ViewQuestionStatusCountsQuery(
            null, null, "Toán", null, null, "ALL", "abc"));

        verify(questionQueryRepository).countAccessibleByStatus(
            userId, schoolId, false, false, null, null, "%toán%", null, null, "ALL", "%abc%");
    }

    @Test
    void should_reject_user_without_school_when_not_system_admin() {
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(query()))
            .isInstanceOf(ForbiddenException.class);
    }

    private ViewQuestionStatusCountsQuery query() {
        return new ViewQuestionStatusCountsQuery(null, null, null, null, null, "ALL", null);
    }
}
