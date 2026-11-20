package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.assessmentturn.TurnUploadUrlResponse;

/**
 * Mirror of GetTurnUploadUrlUseCase (assessmentturn) for practice -- the same StoragePort, a different
 * key prefix. Called from the internal endpoint (Python, not the client) because Python is the side holding the
 * audio buffer of the turn, see PracticeAttemptConnection._write_turn_wav.
 */
@Service
public class GetPracticeTurnUploadUrlUseCase {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StoragePort storagePort;

    public GetPracticeTurnUploadUrlUseCase(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public TurnUploadUrlResponse execute(UUID practiceSessionId, int turnOrder) {
        String key = "practice/%s/turn-%d.wav".formatted(practiceSessionId, turnOrder);
        StoragePort.PresignedUpload presigned = storagePort.presignUpload(key, "audio/wav", TTL);
        return new TurnUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }
}
