package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.Instant;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.questiontopic.AcceptQuestionTopicImportCommand;
import com.sep.vox.application.port.input.service.ImportSessionAcceptanceGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

/**
 * Confirms the column mapping and then hands the question topic import session to the background processing queue.
 *
 * <p>The commit is not run here: {@code ImportQueueService} picks up a queued session and calls the
 * matching handler, so a large file does not hold the user's HTTP request.
 */
@Service
@RequiredArgsConstructor
public class AcceptQuestionTopicImportUseCase implements IUseCase<AcceptQuestionTopicImportCommand, Void> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "code",
        "name"
    );

    private final ImportSessionRepository importSessionRepository;
    private final ImportSessionAcceptanceGuard importSessionAcceptanceGuard;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public Void execute(AcceptQuestionTopicImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        importSessionAcceptanceGuard.requireAcceptableSession(
            input.importSessionId(), ImportType.QUESTION_TOPIC, "The import session is not a question topic import", currentUserId, now);
        importSessionAcceptanceGuard.requireMappedFields(input.confirmedMapping(), REQUIRED_FIELDS);

        int queued = importSessionRepository.markQueued(
            input.importSessionId(),
            ImportType.QUESTION_TOPIC.name(),
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now
        );
        if (queued == 0) {
            throw new IllegalArgumentException("The import session is not in a status that allows accepting, or it has expired");
        }
        return null;
    }

    private void validateCommand(AcceptQuestionTopicImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Import session must not be empty");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Import mapping must not be empty");
        }
    }
}
