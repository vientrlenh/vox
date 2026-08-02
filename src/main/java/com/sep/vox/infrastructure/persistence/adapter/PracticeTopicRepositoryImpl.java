package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticeTopicMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeTopicRepository;

@Repository
public class PracticeTopicRepositoryImpl implements PracticeTopicRepository {

    private final SpringDataPracticeTopicRepository repository;

    public PracticeTopicRepositoryImpl(SpringDataPracticeTopicRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PracticeTopic> findTopicById(UUID id) {
        return repository.findById(id).map(PracticeTopicMapper::toDomain);
    }

    @Override
    public boolean existsActiveById(UUID id) {
        return repository.existsByIdAndActiveTrue(id);
    }

    @Override
    public PracticeTopic save(PracticeTopic topic) {
        return PracticeTopicMapper.toDomain(repository.save(PracticeTopicMapper.toJpa(topic)));
    }

    @Override
    public Map<UUID, String> findAllTopicDimensions() {
        return repository.findAll().stream()
            .collect(Collectors.toMap(
                entity -> entity.getId(),
                entity -> entity.getInterestDimension(),
                (left, right) -> left
            ));
    }

    @Override
    public List<PracticeTopic> findAllActive() {
        return repository.findByActiveTrue().stream()
            .map(PracticeTopicMapper::toDomain)
            .toList();
    }

    @Override
    public List<PracticeTopic> findAllActiveOrderByName() {
        return repository.findByActiveTrueOrderByName().stream()
            .map(PracticeTopicMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<PracticeTopic> findByNormalizedName(String normalizedName) {
        return repository.findByNormalizedName(normalizedName)
            .map(PracticeTopicMapper::toDomain);
    }

    @Override
    public List<String> findExhaustedTopicNames(UUID studentId) {
        return repository.findExhaustedTopicNames(studentId);
    }

    @Override
    public Map<String, Double> findInterestScoresByDimension(UUID studentId) {
        var result = new java.util.HashMap<String, Double>();
        repository.findInterestScores(studentId)
            .forEach(row -> result.put(row.getDimension(), row.getScore()));
        return result;
    }

    @Override
    public Optional<PracticeTopic> findBySourceQuestionTopicId(UUID sourceQuestionTopicId) {
        return repository.findBySourceQuestionTopicId(sourceQuestionTopicId).map(PracticeTopicMapper::toDomain);
    }

    @Override
    public List<QuestionTopicInfo> findPublishedExamTopics(UUID schoolId, UUID gradeId) {
        return repository.findPublishedExamTopics(schoolId, gradeId);
    }
}
