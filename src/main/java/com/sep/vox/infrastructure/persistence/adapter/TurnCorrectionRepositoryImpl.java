package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;
import com.sep.vox.domain.repository.TurnCorrectionRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.TurnCorrectionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTurnCorrectionRepository;

@Repository
@RequiredArgsConstructor
public class TurnCorrectionRepositoryImpl implements TurnCorrectionRepository {

    private final SpringDataTurnCorrectionRepository repository;

    @Override
    public void save(
            UUID turnId,
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl) {
        repository.save(TurnCorrectionJpaEntity.builder()
                .turnId(turnId)
                .category(category)
                .originalText(originalText)
                .correctedText(correctedText)
                .explanation(explanation)
                .correctAudioUrl(correctAudioUrl)
                .build());
    }

    @Override
    public List<TurnCorrectionDto> findByTurnIdOrderById(UUID turnId) {
        return repository.findByTurnIdOrderById(turnId).stream()
            .map(entity -> new TurnCorrectionDto(
                entity.getCategory(),
                entity.getOriginalText(),
                entity.getCorrectedText(),
                entity.getExplanation(),
                entity.getCorrectAudioUrl()
            ))
            .toList();
    }
}
