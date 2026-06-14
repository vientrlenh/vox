package com.sep.vox.application.usecase.questiontopic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ReviewQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.ReviewQuestionTopicUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionTopicRepository;

class ReviewQuestionTopicUseCaseTests {

    private QuestionTopicRepository questionTopicRepository;
    private QuestionTopicPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private ReviewQuestionTopicUseCase useCase;

    @BeforeEach
    void setUp() {
        questionTopicRepository = mock(QuestionTopicRepository.class);
        permissionQuery = mock(QuestionTopicPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ReviewQuestionTopicUseCase(questionTopicRepository, permissionQuery, userContextPort);
    }

    @Test
    void review_should_update_status_when_permitted() {
        var topicId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var topic = new QuestionTopic(topicId, UUID.randomUUID(), "TOPIC", "Topic", null, QuestionTopicStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());

        when(permissionQuery.canPublishTopic(topicId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(questionTopicRepository.save(any(QuestionTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var response = useCase.execute(new ReviewQuestionTopicCommand(topicId, QuestionTopicStatus.PUBLISHED));

        assertThat(response.questionTopicId()).isEqualTo(topicId);
        assertThat(topic.getStatus()).isEqualTo(QuestionTopicStatus.PUBLISHED);
        assertThat(topic.getUpdatedBy()).isEqualTo(userId);
        verify(permissionQuery).canPublishTopic(topicId);
        verify(permissionQuery, never()).canArchiveTopic(topicId);
        verify(permissionQuery, never()).canRestoreTopic(topicId);
    }

    @Test
    void review_should_archive_topic_when_permitted() {
        var topicId = UUID.randomUUID();
        var topic = new QuestionTopic(topicId, UUID.randomUUID(), "TOPIC", "Topic", null, QuestionTopicStatus.PUBLISHED,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());

        when(permissionQuery.canArchiveTopic(topicId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(questionTopicRepository.save(any(QuestionTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        var response = useCase.execute(new ReviewQuestionTopicCommand(topicId, QuestionTopicStatus.ARCHIVED));

        assertThat(response.questionTopicId()).isEqualTo(topicId);
        assertThat(topic.getStatus()).isEqualTo(QuestionTopicStatus.ARCHIVED);
        verify(permissionQuery).canArchiveTopic(topicId);
        verify(permissionQuery, never()).canPublishTopic(topicId);
        verify(permissionQuery, never()).canRestoreTopic(topicId);
    }

    @Test
    void review_should_restore_topic_when_permitted() {
        var topicId = UUID.randomUUID();
        var topic = new QuestionTopic(topicId, UUID.randomUUID(), "TOPIC", "Topic", null, QuestionTopicStatus.ARCHIVED,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());

        when(permissionQuery.canRestoreTopic(topicId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(questionTopicRepository.save(any(QuestionTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        var response = useCase.execute(new ReviewQuestionTopicCommand(topicId, QuestionTopicStatus.DRAFT));

        assertThat(response.questionTopicId()).isEqualTo(topicId);
        assertThat(topic.getStatus()).isEqualTo(QuestionTopicStatus.DRAFT);
        verify(permissionQuery).canRestoreTopic(topicId);
        verify(permissionQuery, never()).canPublishTopic(topicId);
        verify(permissionQuery, never()).canArchiveTopic(topicId);
    }

    @Test
    void review_should_throw_when_not_permitted() {
        var topicId = UUID.randomUUID();
        when(permissionQuery.canArchiveTopic(topicId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ReviewQuestionTopicCommand(topicId, QuestionTopicStatus.ARCHIVED)));
        verify(permissionQuery).canArchiveTopic(topicId);
        verifyNoInteractions(questionTopicRepository, userContextPort);
    }
}
