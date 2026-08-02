package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.personalization.TopicSuggestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.TopicSuggestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicSuggestionRepository;

@Repository
public class TopicSuggestionRepositoryImpl implements TopicSuggestionRepository {

    private final SpringDataTopicSuggestionRepository repository;

    public TopicSuggestionRepositoryImpl(
            SpringDataTopicSuggestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TopicSuggestion> findById(UUID id) {
        return repository.findById(id).map(TopicSuggestionMapper::toDomain);
    }

    @Override
    public TopicSuggestion save(TopicSuggestion suggestion) {
        return TopicSuggestionMapper.toDomain(
            repository.save(TopicSuggestionMapper.toJpa(suggestion))
        );
    }

    @Override
    public List<TopicSuggestion> findPendingByStudentId(UUID studentId) {
        return repository
            .findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, "PENDING")
            .stream()
            .map(TopicSuggestionMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<TopicSuggestion> findByIdAndStudentIdAndStatusForUpdate(
            UUID id,
            UUID studentId,
            String status) {
        return repository.findByIdAndStudentIdAndStatus(id, studentId, status)
            .map(TopicSuggestionMapper::toDomain);
    }

    @Override
    public int countByStudentIdAndStatus(UUID studentId, String status) {
        return repository.countByStudentIdAndStatus(studentId, status);
    }

    @Override
    public List<TopicSuggestion> findByStudentIdAndStatus(UUID studentId, String status) {
        return repository
            .findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, status)
            .stream()
            .map(TopicSuggestionMapper::toDomain)
            .toList();
    }

    @Override
    public int countWeeklyKeywordRequests(UUID studentId) {
        return repository.countWeeklyKeywordRequests(studentId);
    }

    @Override
    public List<UUID> findStudentsDueForSuggestionRefresh(int limit) {
        return repository.findStudentsDueForSuggestionRefresh(limit);
    }

    @Override
    public List<StudentTranscript> findRecentTranscripts(UUID studentId) {
        return repository.findRecentTranscripts(studentId).stream()
            .map(row -> new StudentTranscript(row.getSessionId(), row.getTranscript()))
            .toList();
    }
}
