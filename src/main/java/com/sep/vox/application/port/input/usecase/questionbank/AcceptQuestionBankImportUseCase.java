package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.questionbank.AcceptQuestionBankImportCommand;
import com.sep.vox.application.port.input.service.ImportSessionAcceptanceGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

/**
 * Xác nhận ghép cột rồi đẩy phiên import ngân hàng câu hỏi sang hàng đợi xử lý ngầm.
 *
 * <p>Không tự chạy commit ở đây: {@code ImportQueueService} nhặt phiên đã queued và gọi
 * handler tương ứng, nên một file lớn không giữ request HTTP của người dùng.
 */
@Service
@RequiredArgsConstructor
public class AcceptQuestionBankImportUseCase implements IUseCase<AcceptQuestionBankImportCommand, Void> {

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
    public Void execute(AcceptQuestionBankImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        importSessionAcceptanceGuard.requireAcceptableSession(
            input.importSessionId(), ImportType.QUESTION_BANK, "Phiên import không phải là loại ngân hàng câu hỏi", currentUserId, now);
        importSessionAcceptanceGuard.requireMappedFields(input.confirmedMapping(), REQUIRED_FIELDS);

        int queued = importSessionRepository.markQueued(
            input.importSessionId(),
            ImportType.QUESTION_BANK.name(),
            jsonSerializationPort.toJson(input.confirmedMapping()),
            now
        );
        if (queued == 0) {
            throw new IllegalArgumentException("Phiên import không ở trạng thái cho accept hoặc đã hết hạn");
        }
        return null;
    }

    private void validateCommand(AcceptQuestionBankImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }
}
