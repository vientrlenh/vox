package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptQuestionTopicImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

/**
 * Xác nhận ghép cột rồi đẩy phiên import chủ đề câu hỏi sang hàng đợi xử lý ngầm.
 *
 * <p>Không tự chạy commit ở đây: {@code ImportQueueService} nhặt phiên đã queued và gọi
 * handler tương ứng, nên một file lớn không giữ request HTTP của người dùng.
 */
@Service
public class AcceptQuestionTopicImportUseCase implements IUseCase<AcceptQuestionTopicImportCommand, Void> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "code",
        "name"
    );

    private final ImportSessionRepository importSessionRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptQuestionTopicImportUseCase(
            ImportSessionRepository importSessionRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public Void execute(AcceptQuestionTopicImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }

        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var session = importSessionRepository.findById(input.importSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import với id: " + input.importSessionId()));

        if (session.getType() != ImportType.QUESTION_TOPIC) {
            throw new IllegalArgumentException("Phiên import không phải là loại chủ đề câu hỏi");
        }
        if (!Objects.equals(session.getCreatedBy(), currentUserId)) {
            throw new IllegalArgumentException("Phiên import không thuộc người dùng hiện tại");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalStateException("Phiên import đã hết hạn");
        }
        validateRequiredMapping(input.confirmedMapping());

        var queued = importSessionRepository.markQueued(
            input.importSessionId(),
            ImportType.QUESTION_TOPIC.name(),
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now,
            currentUserId
        );
        if (queued == 0) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept hoặc đã hết hạn");
        }
        return null;
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
                "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields)
            );
        }
    }
}
