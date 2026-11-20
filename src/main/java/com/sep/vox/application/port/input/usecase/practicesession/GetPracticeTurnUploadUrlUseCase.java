package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.examturn.TurnUploadUrlResponse;

/**
 * Mirror của GetTurnUploadUrlUseCase (examturn) cho luyện tập -- cùng StoragePort, khác
 * key prefix. Gọi từ endpoint nội bộ (Python, không phải client) vì Python là bên đang giữ
 * buffer audio của turn, xem PracticeAttemptConnection._write_turn_wav.
 */
@Service
public class GetPracticeTurnUploadUrlUseCase {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StoragePort storagePort;

    public GetPracticeTurnUploadUrlUseCase(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public TurnUploadUrlResponse execute(UUID practiceSessionId, int turnOrder) {
        var key = "practice/%s/turn-%d.wav".formatted(practiceSessionId, turnOrder);
        var presigned = storagePort.presignUpload(key, "audio/wav", TTL);
        return new TurnUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }
}
