package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemResponseTurn;

public interface ExamItemResponseTurnRepository {
    record SessionFollowupCount(UUID examItemResponseId, long followupCount, long totalTurns) {
    }

    ExamItemResponseTurn upsert(ExamItemResponseTurn turn);
    List<ExamItemResponseTurn> findByExamItemResponseId(UUID examItemResponseId);
    Optional<ExamItemResponseTurn> findByExamItemResponseIdAndTurnOrder(UUID examItemResponseId, int turnOrder);
    Optional<ExamItemResponseTurn> findLatestByExamItemResponseId(UUID examItemResponseId);
    List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId);
}
