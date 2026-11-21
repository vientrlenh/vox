package com.sep.vox.infrastructure.persistence.entity.question;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "question_evaluation_guides", indexes = {
    @Index(columnList = "question_id", name = "idx_question_evaluation_guides_question", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionEvaluationGuideJpaEntity {
    
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "expected_content", columnDefinition = "TEXT")
    private String expectedContent;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "acceptable_responses", columnDefinition = "TEXT")
    private String acceptableResponses;

    @Column(name = "off_topic_examples", columnDefinition = "TEXT")
    private String offTopicExamples;

    @Column(name = "scoring_hints", columnDefinition = "TEXT")
    private String scoringHints;

    @Column(name = "common_mistakes", columnDefinition = "TEXT")
    private String commonMistakes;
}
