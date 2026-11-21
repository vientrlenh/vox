package com.sep.vox.application.port.input.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;

@ExtendWith(MockitoExtension.class)
class TopicOfferBackfillServiceTests {

    @Mock
    private TopicSuggestionService topicSuggestionService;
    @Mock
    private PracticeTopicRepository practiceTopicRepository;
    @Mock
    private LearnerProfileRepository learnerProfileRepository;
    @Mock
    private CacheManagerPort cacheManagerPort;
    @Mock
    private PracticeGenerationExecutorPort practiceGenerationExecutorPort;
    @InjectMocks
    private TopicOfferBackfillService service;

    @BeforeEach
    void runScheduledWorkInline() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(practiceGenerationExecutorPort).runAsync(any(Runnable.class));
    }

    @Test
    void backfillAsync_doesNothingForANullStudentId() {
        service.backfillAsync(null);

        verifyNoInteractions(topicSuggestionService, learnerProfileRepository, cacheManagerPort);
    }

    @Test
    void backfillAsync_generatesNothingWhenTheStudentHasNotCompletedTheInterestQuiz() {
        UUID studentId = UUID.randomUUID();
        when(learnerProfileRepository.findCurrent(studentId)).thenReturn(Optional.empty());

        service.backfillAsync(studentId);

        verifyNoInteractions(topicSuggestionService, cacheManagerPort);
    }

    @Test
    void backfillAsync_generatesOffersAndReleasesTheLockWhenTheQuizIsCompleted() {
        UUID studentId = UUID.randomUUID();
        LearnerProfile profile = LearnerProfile.builder().quizCompletedAt(Instant.now()).build();
        when(learnerProfileRepository.findCurrent(studentId)).thenReturn(Optional.of(profile));
        when(cacheManagerPort.saveIfAbsentAndGet(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(practiceTopicRepository.countOfferablePool()).thenReturn(0L);
        when(topicSuggestionService.synchronousOffers(any(), anyInt())).thenReturn(List.of());

        service.backfillAsync(studentId);

        verify(topicSuggestionService).synchronousOffers(studentId, 8);
        verify(cacheManagerPort).deleteIfValueMatches(any(), any());
    }
}
