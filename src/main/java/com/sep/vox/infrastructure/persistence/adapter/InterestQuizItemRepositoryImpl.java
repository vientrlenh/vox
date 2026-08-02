package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.personalization.InterestQuizItemRepository;
import com.sep.vox.infrastructure.persistence.entity.InterestQuizItemJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataInterestQuizItemRepository;

@Repository
public class InterestQuizItemRepositoryImpl
        implements InterestQuizItemRepository {

    private final SpringDataInterestQuizItemRepository repository;
    private final JsonSerializationPort jsonSerialization;

    public InterestQuizItemRepositoryImpl(
            SpringDataInterestQuizItemRepository repository,
            JsonSerializationPort jsonSerialization) {
        this.repository = repository;
        this.jsonSerialization = jsonSerialization;
    }

    @Override
    public List<InterestQuizSeedItem> findActiveQuizItems(int limit) {
        return repository.findTop7ByActiveTrueOrderById().stream()
            .limit(Math.max(1, limit))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<InterestQuizSeedItem> findActiveQuizItemsForStudent(UUID studentId, int limit) {
        return repository.findTop7ByStudentIdAndActiveTrueOrderById(studentId).stream()
            .limit(Math.max(1, limit))
            .map(this::toDomain)
            .toList();
    }

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
        return repository.existsByStudentId(studentId);
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
        var now = OffsetDateTime.now();
        repository.saveAll(items.stream()
            .map(item -> new InterestQuizItemJpaEntity(
                jsonSerialization.toJson(item.dimensionPerStatement()),
                jsonSerialization.toJson(item.statements()),
                item.note(),
                true,
                null,
                now
            ))
            .toList());
    }

    @Override
    public void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items) {
        var now = OffsetDateTime.now();
        repository.saveAll(items.stream()
            .map(item -> new InterestQuizItemJpaEntity(
                jsonSerialization.toJson(item.dimensionPerStatement()),
                jsonSerialization.toJson(item.statements()),
                item.note(),
                true,
                studentId,
                now
            ))
            .toList());
    }

    private InterestQuizSeedItem toDomain(InterestQuizItemJpaEntity entity) {
        return new InterestQuizSeedItem(
            entity.getId(),
            jsonSerialization.toStringList(entity.getDimensionsJson()),
            jsonSerialization.toStringList(entity.getStatementsJson()),
            entity.getDesirabilityNote()
        );
    }
}
