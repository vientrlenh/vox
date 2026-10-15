package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswer;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentItemAnswerMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentItemAnswerRepository;

@Repository
public class AssessmentItemAnswerRepositoryImpl implements AssessmentItemAnswerRepository {

    private final SpringDataAssessmentItemAnswerRepository springDataAssessmentItemAnswerRepository;

    public AssessmentItemAnswerRepositoryImpl(SpringDataAssessmentItemAnswerRepository springDataAssessmentItemAnswerRepository) {
        this.springDataAssessmentItemAnswerRepository = springDataAssessmentItemAnswerRepository;
    }

    @Override
    public int sumDurationSecondsBySessionId(UUID sessionId) {
        return springDataAssessmentItemAnswerRepository.sumDurationSecondsBySessionId(sessionId);
    }

    @Override
    public Optional<AssessmentItemAnswer> findById(UUID id) {
        return springDataAssessmentItemAnswerRepository.findById(id)
            .map(AssessmentItemAnswerMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataAssessmentItemAnswerRepository.existsById(id);
    }

    @Override
    public AssessmentItemAnswer save(AssessmentItemAnswer response) {
        var saved = springDataAssessmentItemAnswerRepository.save(AssessmentItemAnswerMapper.toJpa(response));
        return AssessmentItemAnswerMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentItemAnswer> findBySessionId(UUID sessionId) {
        return springDataAssessmentItemAnswerRepository.findBySessionId(sessionId).stream()
            .map(AssessmentItemAnswerMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        springDataAssessmentItemAnswerRepository.deleteBySessionId(sessionId);
    }

    @Override
    public List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentItemAnswerRepository.sumDurationSecondsGroupedBySessionIds(sessionIds);
    }
}
