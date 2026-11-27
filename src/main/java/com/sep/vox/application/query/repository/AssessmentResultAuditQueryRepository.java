package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;

/**
 * The read side for two "look back" screens: the score timeline of one submission, and the AI quality
 * report for a whole assessment.
 *
 * <p>Both join to {@code users} to get the name of the person who acted, so they go through a query
 * repository rather than a domain repository.
 */
public interface AssessmentResultAuditQueryRepository {

    /** The timeline of one submission, oldest first. */
    List<ResultStatusHistoryInfo> findHistory(UUID candidateResultId);

    /**
     * The AI quality report within a school, additionally filtered by assessment if given.
     * The denominator is the completed post-review rounds.
     */
    AiQualityReportInfo aiQualityReport(UUID schoolId, UUID assessmentId);
}
