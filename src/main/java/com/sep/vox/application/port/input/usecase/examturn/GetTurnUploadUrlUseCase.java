package com.sep.vox.application.port.input.usecase.examturn;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.examturn.TurnUploadUrlResponse;

@Service
public class GetTurnUploadUrlUseCase implements IUseCase<GetTurnUploadUrlQuery, TurnUploadUrlResponse> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StoragePort storagePort;

    public GetTurnUploadUrlUseCase(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public TurnUploadUrlResponse execute(GetTurnUploadUrlQuery input) {
        var key = "%s/turn-%d.wav".formatted(input.attemptAnswerId(), input.turnOrder());
        var presigned = storagePort.presignUpload(key, "audio/wav", TTL);
        return new TurnUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }
}
