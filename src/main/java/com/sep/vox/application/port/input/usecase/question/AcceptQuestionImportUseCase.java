package com.sep.vox.application.port.input.usecase.question;

import java.time.Instant;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.question.AcceptQuestionImportCommand;
import com.sep.vox.application.port.input.service.ImportSessionAcceptanceGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
@RequiredArgsConstructor
public class AcceptQuestionImportUseCase implements IUseCase<AcceptQuestionImportCommand, Void> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "type",
        "questionText",
        "preparationTimeSeconds",
        "minResponseSeconds",
        "maxResponseSeconds"
    );
    private static final String QUESTION_TYPE = "QUESTION";

    private final ImportSessionRepository importSessionRepository;
    private final ImportSessionAcceptanceGuard importSessionAcceptanceGuard;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public Void execute(AcceptQuestionImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        importSessionAcceptanceGuard.requireAcceptableSession(
            input.importSessionId(), ImportType.QUESTION, "Phiên import không phải là loại câu hỏi", currentUserId, now);
        importSessionAcceptanceGuard.requireMappedFields(input.confirmedMapping(), REQUIRED_FIELDS);

        int queued = importSessionRepository.markQueued(
            input.importSessionId(),
            QUESTION_TYPE,
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now
        );
        if (queued == 0) {
            throw new IllegalArgumentException("Phiên import không ở trạng thái cho accept hoac đã hết hạn");
        }
        return null;
    }

    private void validateCommand(AcceptQuestionImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }
}
