package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
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
        var now = Instant.now();
        repository.saveAll(items.stream()
            .map(item -> new InterestQuizItemJpaEntity(
                jsonSerialization.toJson(item.getDimensionPerStatement()),
                jsonSerialization.toJson(item.getStatements()),
                item.getNote(),
                true,
                null,
                now
            ))
            .toList());
    }

    @Override
    public void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items) {
        var now = Instant.now();
        repository.saveAll(items.stream()
            .map(item -> new InterestQuizItemJpaEntity(
                jsonSerialization.toJson(item.getDimensionPerStatement()),
                jsonSerialization.toJson(item.getStatements()),
                item.getNote(),
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

    @Override
    @Transactional
    public int deactivateGeneratedForStudent(UUID studentId) {
        return repository.deactivateByStudentId(studentId);
    }
}
