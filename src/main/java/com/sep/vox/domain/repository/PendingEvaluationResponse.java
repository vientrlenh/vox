package com.sep.vox.domain.repository;

import java.util.UUID;

/**
 * An answered question that has no grading yet -- the set to flush for grading when the session closes.
 *
 * Needs BOTH ids: {@code responseId} to gather the turns already spoken, {@code questionId} to fetch the prompt
 * and the grading guide. Returning only questionId and then upserting to get responseId is a workaround -- the upsert
 * also appends the transcript and overwrites audioUrl, i.e. it modifies data just to read an id.
 */
public interface PendingEvaluationResponse {

    UUID getResponseId();

    UUID getQuestionId();
}
