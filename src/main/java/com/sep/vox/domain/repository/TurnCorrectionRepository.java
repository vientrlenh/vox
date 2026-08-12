package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;

public interface TurnCorrectionRepository {

    void save(
        UUID turnId,
        String category,
        String originalText,
        String correctedText,
        String explanation,
        String correctAudioUrl
    );

    List<TurnCorrectionDto> findByTurnIdOrderById(UUID turnId);
}
