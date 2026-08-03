package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.command.EndPracticeSessionCommand;
import com.sep.vox.application.port.input.service.InterestVectorService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

@Service
public class EndPracticeSessionUseCase implements IUseCase<EndPracticeSessionCommand, PracticeSession> {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final InterestVectorService interestVectorService;
    private final JsonSerializationPort jsonSerializationPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserContextPort userContextPort;

    public EndPracticeSessionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            InterestVectorService interestVectorService,
            JsonSerializationPort jsonSerializationPort,
            ApplicationEventPublisher applicationEventPublisher,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.interestVectorService = interestVectorService;
        this.jsonSerializationPort = jsonSerializationPort;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PracticeSession execute(EndPracticeSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var sessionId = input.sessionId();
        requireOwnedInProgress(sessionId, studentId);
        var lastScore = practiceItemEvaluationRepository.findLastValidNormalizedScore(sessionId);
        var completedItems = practiceItemEvaluationRepository.countCompletedBySessionId(sessionId);
        var status = completedItems > 0 ? "COMPLETED" : "ABANDONED";
        var diagnosis = "ABANDONED".equals(status)
            ? SessionDiagnosisPolicy.diagnose(lastScore, input.helpRequestCount(), input.longPauseCount())
            : null;
        var session = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        practiceSessionRepository.save(session.ended(
            status,
            diagnosis,
            input.helpRequestCount(),
            input.longPauseCount(),
            Instant.now(),
            practiceItemEvaluationRepository.findAverageItemScoreBySessionId(sessionId)
        ));
        var row = practiceSessionQueryRepository.findSessionRowById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        interestVectorService.recordSessionOutcome(
            studentId,
            row.getChosenPracticeTopicId(),
            sessionId,
            row.getOrigin(),
            diagnosis,
            "COMPLETED".equals(status),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "current"),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "previous")
        );
        applicationEventPublisher.publishEvent(new PracticeSessionEndedEvent(studentId, sessionId));
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(sessionId, studentId).orElse(null)
            )
        );
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new NotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

    private List<UUID> offeredTopicIds(String json, String field) {
        return jsonSerializationPort.toStringListField(json, field).stream()
            .map(UUID::fromString)
            .toList();
    }
}
