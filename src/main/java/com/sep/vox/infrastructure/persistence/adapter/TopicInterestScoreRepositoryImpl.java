package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.TopicInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.TopicInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestScoreRepository;

@Repository
public class TopicInterestScoreRepositoryImpl
        implements TopicInterestScoreRepository {

    private final SpringDataTopicInterestScoreRepository repository;

    public TopicInterestScoreRepositoryImpl(
            SpringDataTopicInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForStudent(
            UUID studentId,
            List<TopicInterestScoreEntry> scores) {
        repository.deleteByStudentId(studentId);
        // PUSH THE DELETE STATEMENT DOWN TO THE DB BEFORE INSERTING.
        //
        // Without this line Hibernate batches both into a single flush at the end of the transaction, and
        // its ActionQueue always runs INSERT BEFORE DELETE. So the new rows collide with the very old
        // rows that should already have been deleted -> duplicate key on
        // uq_topic_interest_score_student_topic (student_id, practice_topic_id).
        //
        // It only shows up from the SECOND run onwards: the first time the table is still empty, so there
        // is nothing to collide with. That is exactly why it stayed hidden for so long -- recordSessionOutcome
        // used to run only when the student pressed "Complete", which happened exactly once.
        repository.flush();
        Instant now = Instant.now();
        repository.saveAll(scores.stream()
            .map(entry -> TopicInterestScoreJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .studentId(studentId)
                    .practiceTopicId(entry.getTopicId())
                    .score(BigDecimal.valueOf(entry.getScore()))
                    .sessionsMentioned(entry.getSessionCount())
                    .lastMentionedAt(entry.getLastEventAt())
                    .computedAt(now)
                    .build())
            .toList());
    }
}
