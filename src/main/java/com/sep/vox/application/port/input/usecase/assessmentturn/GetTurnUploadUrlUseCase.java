package com.sep.vox.application.port.input.usecase.assessmentturn;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.assessmentturn.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.assessmentturn.TurnUploadUrlResponse;

@Service
@RequiredArgsConstructor
public class GetTurnUploadUrlUseCase implements IUseCase<GetTurnUploadUrlQuery, TurnUploadUrlResponse> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StoragePort storagePort;

    @Override
    public TurnUploadUrlResponse execute(GetTurnUploadUrlQuery input) {
        String key = "%s/turn-%d.wav".formatted(input.attemptAnswerId(), input.turnOrder());
        StoragePort.PresignedUpload presigned = storagePort.presignUpload(key, "audio/wav", TTL);
        return new TurnUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }
}
