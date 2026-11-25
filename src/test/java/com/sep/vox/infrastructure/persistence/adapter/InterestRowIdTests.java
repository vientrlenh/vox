package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.SavedTopicRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;
import com.sep.vox.domain.repository.TopicInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSavedTopicRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataStudentQuestionExposureRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestEventRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestScoreRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({SavedTopicRepositoryImpl.class, TopicInterestEventRepositoryImpl.class,
    TopicInterestScoreRepositoryImpl.class, StudentQuestionExposureRepositoryImpl.class})
class InterestRowIdTests {

    @Autowired
    private SavedTopicRepository savedTopicRepository;
    @Autowired
    private TopicInterestEventRepository eventRepository;
    @Autowired
    private TopicInterestScoreRepository scoreRepository;
    @Autowired
    private StudentQuestionExposureRepository exposureRepository;
    @Autowired
    private SpringDataSavedTopicRepository springDataSavedTopicRepository;
    @Autowired
    private SpringDataTopicInterestEventRepository springDataEventRepository;
    @Autowired
    private SpringDataTopicInterestScoreRepository springDataScoreRepository;
    @Autowired
    private SpringDataStudentQuestionExposureRepository springDataExposureRepository;

    @Test
    void should_store_a_version_7_id_for_a_saved_topic() {
        savedTopicRepository.save(UUID.randomUUID(), UUID.randomUUID());

        assertThat(springDataSavedTopicRepository.findAll())
            .singleElement().satisfies(entity -> assertThat(entity.getId().version()).isEqualTo(7));
    }

    @Test
    void should_store_a_version_7_id_for_an_interest_event() {
        eventRepository.save(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VIEW", 0.5);

        assertThat(springDataEventRepository.findAll())
            .singleElement().satisfies(entity -> assertThat(entity.getId().version()).isEqualTo(7));
    }

    @Test
    void should_store_version_7_ids_and_survive_a_second_replace_for_the_same_student() {
        UUID studentId = UUID.randomUUID();
        List<TopicInterestScoreEntry> scores = List.of(entry(), entry());

        scoreRepository.replaceForStudent(studentId, scores);
        scoreRepository.replaceForStudent(studentId, scores);

        assertThat(springDataScoreRepository.findAll()).hasSize(2)
            .allSatisfy(entity -> assertThat(entity.getId().version()).isEqualTo(7));
    }

    @Test
    void should_store_a_version_7_id_for_a_question_exposure() {
        exposureRepository.recordExposure(UUID.randomUUID(), UUID.randomUUID());

        assertThat(springDataExposureRepository.findAll())
            .singleElement().satisfies(entity -> assertThat(entity.getId().version()).isEqualTo(7));
    }

    private static TopicInterestScoreEntry entry() {
        return TopicInterestScoreEntry.builder()
            .topicId(UUID.randomUUID())
            .score(0.4)
            .sessionCount(1)
            .lastEventAt(Instant.now())
            .build();
    }
}
