package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptQuestionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
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
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptQuestionImportUseCase(
            ImportSessionRepository importSessionRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public Void execute(AcceptQuestionImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var session = importSessionRepository.findById(input.importSessionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay phien import"));

        if (session.getType() != ImportType.QUESTION) {
            throw new IllegalArgumentException("Phien import khong phai la import cau hoi");
        }
        if (!Objects.equals(session.getCreatedBy(), currentUserId)) {
            throw new IllegalArgumentException("Phien import khong thuoc nguoi dung hien tai");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phien import khong o trang thai cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalStateException("Phien import da het han");
        }
        validateRequiredMapping(input.confirmedMapping());

        var queued = importSessionRepository.markQueued(
            input.importSessionId(),
            QUESTION_TYPE,
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now,
            currentUserId
        );
        if (queued == 0) {
            throw new IllegalStateException("Phien import khong o trang thai cho accept hoac da het han");
        }
        return null;
    }

    private void validateCommand(AcceptQuestionImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phien import khong duoc de trong");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import khong duoc de trong");
        }
    }

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        var mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .forEach(mappedFields::add);
        var missingFields = REQUIRED_FIELDS.stream()
            .filter(field -> !mappedFields.contains(field))
            .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                "Mapping import thieu truong bat buoc: " + String.join(", ", missingFields)
            );
        }
    }
}
