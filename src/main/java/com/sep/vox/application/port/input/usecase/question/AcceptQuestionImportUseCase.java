package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.importfile.ImportSession;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.AcceptQuestionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
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
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public Void execute(AcceptQuestionImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        ImportSession session = importSessionRepository.findById(input.importSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên import với id: " + input.importSessionId()));

        if (session.getType() != ImportType.QUESTION) {
            throw new IllegalArgumentException("Phiên import không phải là loại câu hỏi");
        }
        if (!session.isCreatedBy(currentUserId)) {
            throw new IllegalArgumentException("Phiên import không thuộc người dùng hiện tại");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalArgumentException("Phiên import đã hết hạn");
        }
        validateRequiredMapping(input.confirmedMapping());

        int queued = importSessionRepository.markQueued(
            input.importSessionId(),
            QUESTION_TYPE,
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now,
            currentUserId.toString()
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

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        Set<String> mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
            .filter(Objects::nonNull)
            .map(s -> s.strip())
            .forEach(mappedFields::add);
        List<String> missingFields = REQUIRED_FIELDS.stream()
            .filter(field -> !mappedFields.contains(field))
            .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields)
            );
        }
    }
}
