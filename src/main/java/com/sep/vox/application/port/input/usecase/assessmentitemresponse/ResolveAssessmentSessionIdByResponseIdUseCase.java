package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;

/**
 * Looks up the sessionId from a responseId -- used in the DLT handler of
 * AssessmentAttemptEvaluationCompletedConsumer when the error payload cannot be parsed to get assessmentAttemptId
 * directly and only the answerId (responseId) remains to derive the session to mark GRADING_FAILED. Split into
 * its own use case because the interfaces layer (the Kafka consumer) must not call a domain repository directly.
 */
@Service
public class ResolveAssessmentSessionIdByResponseIdUseCase implements IUseCase<UUID, UUID> {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;

    public ResolveAssessmentSessionIdByResponseIdUseCase(AssessmentItemResponseRepository assessmentItemResponseRepository) {
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID execute(UUID responseId) {
        if (responseId == null) {
            return null;
        }
        return assessmentItemResponseRepository.findById(responseId)
            .map(response -> response.getSessionId())
            .orElse(null);
    }
}
