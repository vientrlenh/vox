package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentItemResponseMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentItemResponseRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentItemResponseRepositoryImpl implements AssessmentItemResponseRepository {

    private final SpringDataAssessmentItemResponseRepository springDataAssessmentItemResponseRepository;

    @Override
    public int sumDurationSecondsBySessionId(UUID sessionId) {
        return springDataAssessmentItemResponseRepository.sumDurationSecondsBySessionId(sessionId);
    }

    @Override
    public Optional<AssessmentItemResponse> findById(UUID id) {
        return springDataAssessmentItemResponseRepository.findById(id)
            .map(AssessmentItemResponseMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataAssessmentItemResponseRepository.existsById(id);
    }

    @Override
    public AssessmentItemResponse save(AssessmentItemResponse response) {
        AssessmentItemResponseJpaEntity saved = springDataAssessmentItemResponseRepository.save(AssessmentItemResponseMapper.toJpa(response));
        return AssessmentItemResponseMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentItemResponse> findBySessionId(UUID sessionId) {
        return springDataAssessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .map(AssessmentItemResponseMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        springDataAssessmentItemResponseRepository.deleteBySessionId(sessionId);
    }

    @Override
    public List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds(sessionIds);
    }
}
