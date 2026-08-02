package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;
import com.sep.vox.domain.repository.personalization.TurnCorrectionRepository;
import com.sep.vox.infrastructure.persistence.entity.TurnCorrectionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTurnCorrectionRepository;

@Repository
public class TurnCorrectionRepositoryImpl implements TurnCorrectionRepository {

    private final SpringDataTurnCorrectionRepository repository;

    public TurnCorrectionRepositoryImpl(SpringDataTurnCorrectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(
            UUID turnId,
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl) {
        repository.save(new TurnCorrectionJpaEntity(
            UUID.randomUUID(),
            turnId,
            null,
            category,
            originalText,
            correctedText,
            explanation,
            correctAudioUrl
        ));
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
