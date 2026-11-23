package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.InterestQuizItemJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataInterestQuizItemRepository;

@Repository
@RequiredArgsConstructor
public class InterestQuizItemRepositoryImpl
        implements InterestQuizItemRepository {

    private final SpringDataInterestQuizItemRepository repository;
    private final JsonSerializationPort jsonSerialization;

    @Override
    public List<InterestQuizSeedItem> findAllActiveQuizItems() {
        return repository.findTop50ByActiveTrueOrderById().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<InterestQuizSeedItem> findAllActiveQuizItemsForStudent(UUID studentId) {
        return repository.findTop50ByStudentIdAndActiveTrueOrderById(studentId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean hasQuizItemsForStudent(UUID studentId) {
        return repository.existsByStudentIdAndActiveTrue(studentId);
    }

    @Override
    public Optional<InterestQuizSeedItem> findActiveQuizItem(UUID itemId) {
        return repository.findByIdAndActiveTrue(itemId).stream()
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public void seedQuizItemsIfEmpty(List<InterestQuizSeedItem> items) {
        if (repository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        repository.saveAll(items.stream()
            .map(item -> InterestQuizItemJpaEntity.builder()
                    .dimensionsJson(jsonSerialization.toJson(item.getDimensionPerStatement()))
                    .statementsJson(jsonSerialization.toJson(item.getStatements()))
                    .desirabilityNote(item.getNote())
                    .active(true)
                    .studentId(null)
                    .createdAt(now)
                    .build())
            .toList());
    }

    @Override
    public void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items) {
        Instant now = Instant.now();
        repository.saveAll(items.stream()
            .map(item -> InterestQuizItemJpaEntity.builder()
                    .dimensionsJson(jsonSerialization.toJson(item.getDimensionPerStatement()))
                    .statementsJson(jsonSerialization.toJson(item.getStatements()))
                    .desirabilityNote(item.getNote())
                    .active(true)
                    .studentId(studentId)
                    .createdAt(now)
                    .build())
            .toList());
    }

    private InterestQuizSeedItem toDomain(InterestQuizItemJpaEntity entity) {
        return InterestQuizSeedItem.builder()
                .id(entity.getId())
                .dimensionPerStatement(jsonSerialization.toStringList(entity.getDimensionsJson()))
                .statements(jsonSerialization.toStringList(entity.getStatementsJson()))
                .note(entity.getDesirabilityNote())
                .build();
    }

    @Override
    @Transactional
    public int deactivateGeneratedForStudent(UUID studentId) {
        return repository.deactivateByStudentId(studentId);
    }
}
