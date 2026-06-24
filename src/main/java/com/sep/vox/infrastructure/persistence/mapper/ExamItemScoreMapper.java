package com.sep.vox.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.util.Map;

import tools.jackson.core.type.TypeReference;

import com.sep.vox.domain.model.exam.ExamItemScore;
import com.sep.vox.domain.model.exam.ExamItemScoreStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamItemScoreJpaEntity;

public final class ExamItemScoreMapper {

    private static final TypeReference<Map<String, BigDecimal>> RUBRIC_SCORES_TYPE =
        new TypeReference<>() {};

    private ExamItemScoreMapper() {}

    public static ExamItemScore toDomain(ExamItemScoreJpaEntity jpa) {
        return new ExamItemScore(
            jpa.getId(),
            jpa.getResponseId(),
            jpa.getPaperItemId(),
            rubricScoresFromJson(jpa.getRubricScores()),
            jpa.getItemScore(),
            jpa.getGradedByModel(),
            jpa.getGradedAt(),
            statusFromString(jpa.getStatus())
        );
    }

    public static ExamItemScoreJpaEntity toJpa(ExamItemScore domain) {
        return new ExamItemScoreJpaEntity(
            domain.getId(),
            domain.getResponseId(),
            domain.getPaperItemId(),
            JsonValueObjectMapper.toJson(domain.getRubricScores()),
            domain.getItemScore(),
            domain.getGradedByModel(),
            domain.getGradedAt(),
            domain.getStatus().name()
        );
    }

    private static Map<String, BigDecimal> rubricScoresFromJson(String json) {
        return json == null ? null : JsonValueObjectMapper.fromJson(json, RUBRIC_SCORES_TYPE);
    }

    private static ExamItemScoreStatus statusFromString(String status) {
        return status == null ? null : ExamItemScoreStatus.valueOf(status);
    }
}
