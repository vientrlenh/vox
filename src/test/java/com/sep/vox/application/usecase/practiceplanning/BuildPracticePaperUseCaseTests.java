package com.sep.vox.application.usecase.practiceplanning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.service.PracticePaperPersistenceService;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.usecase.practiceplanning.BuildPracticePaperUseCase;
import com.sep.vox.application.port.input.usecase.practiceplanning.ViewPracticeTopicOffersUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Guard chặn hết gói phải chạy TRƯỚC khi tốn công chọn câu hỏi AI -- xem lý do đầy đủ ở
 * SchoolSubscriptionActiveGuardService.
 */
class BuildPracticePaperUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();

    private PracticeTopicRepository topicRepository;
    private SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService;
    private BuildPracticePaperUseCase useCase;

    @BeforeEach
    void setUp() {
        topicRepository = mock(PracticeTopicRepository.class);
        schoolSubscriptionActiveGuardService = mock(SchoolSubscriptionActiveGuardService.class);
        var userContextPort = mock(UserContextPort.class);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);

        useCase = new BuildPracticePaperUseCase(
            topicRepository,
            mock(PracticePaperRepository.class),
            mock(SchoolSubscriptionRepository.class),
            mock(PracticeTopicOfferEnrichmentService.class),
            mock(PracticeQuestionSelectionService.class),
            mock(PracticePaperPersistenceService.class),
            userContextPort,
            mock(ViewPracticeTopicOffersUseCase.class),
            mock(QuotaPricingPort.class),
            schoolSubscriptionActiveGuardService
        );
    }

    @Test
    void should_reject_beforeSelectingAnyQuestion_when_studentSchoolHasNoActiveSubscription() {
        doThrow(new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể bắt đầu buổi luyện tập cá nhân hóa AI."))
            .when(schoolSubscriptionActiveGuardService).requireActiveForStudent(eq(STUDENT_ID), any());

        var command = new BuildPracticePaperCommand(TOPIC_ID, UUID.randomUUID(), "SELECTED", null, List.of(), List.of());

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("chưa có gói subscription đang hoạt động");
        // Fail-fast: guard chạy trước cả bước tìm topic, không tốn thêm query/AI nào.
        verify(topicRepository, never()).findTopicById(any());
    }
}
