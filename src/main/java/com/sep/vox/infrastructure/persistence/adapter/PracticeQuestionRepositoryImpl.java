package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticeQuestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeQuestionRepository;

@Repository
public class PracticeQuestionRepositoryImpl implements PracticeQuestionRepository {

    private final SpringDataPracticeQuestionRepository repository;

    public PracticeQuestionRepositoryImpl(SpringDataPracticeQuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PracticeQuestion> findById(UUID id) {
        return repository.findById(id).map(PracticeQuestionMapper::toDomain);
    }

    @Override
    public List<PracticeQuestion> findUnseenByTopic(UUID topicId, UUID studentId) {
        return repository.findUnseenByTopic(topicId, studentId).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
            UUID topicId,
            UUID studentId,
            String criterion,
            int rankMin,
            int rankMax) {
        return repository
            .findUnseenByTopicAndCriterionAndRankRange(
                topicId, studentId, criterion, rankMin, rankMax, cooldownCutoff())
            .stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<PracticeQuestion> findUnseenByIds(List<UUID> ids, UUID studentId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findUnseenByIds(ids, studentId, cooldownCutoff()).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    // Gói 11 mục 3.3: câu chưa đạt band mục tiêu chỉ bị loại trong 24h kể từ lần gặp gần nhất.
    private static Instant cooldownCutoff() {
        return Instant.now().minus(Duration.ofDays(1));
    }

    @Override
    public List<PracticeQuestion> findByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public PracticeQuestion save(PracticeQuestion question) {
        return PracticeQuestionMapper.toDomain(repository.save(PracticeQuestionMapper.toJpa(question)));
    }

    /**
     * Transaction NGẮN, đặt đúng ở đây chứ không ở tầng trên.
     *
     * <p>insertGeneratedQuestion là @Modifying nên Hibernate bắt buộc phải có transaction
     * đang mở, nếu không thì ném TransactionRequiredException. Trước đây nó mượn tạm
     * transaction của BuildPracticePaperUseCase; khi transaction đó bị gỡ (để không giữ
     * connection HikariCP suốt 10-40 giây gọi LLM) thì lệnh ghi này mất chỗ dựa và hỏng.
     *
     * <p>Đặt ở tầng adapter là đúng: nó chỉ bao đúng một lệnh INSERT chạy SAU khi lời gọi
     * LLM đã trả về, nên không kéo dài thời gian giữ connection.
     */
    @Override
    @Transactional
    public void saveGenerated(PracticeQuestion question) {
        repository.insertGeneratedQuestion(
            question.getId(),
            question.getPracticeTopicId(),
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            question.getDifficultyFeaturesJson(),
            question.getEvaluationGuideJson(),
            question.getSuggestedIdeasJson(),
            question.getMaxResponseSeconds(),
            question.getMinResponseSeconds(),
            question.getVstepPart()
        );
    }

    /** Cũng là @Modifying như saveGenerated -- cần transaction riêng vì tầng gọi
     * (resolveNextQuestion) cố ý chạy ngoài transaction. Chưa nổ lỗi chỉ vì luồng chết ở
     * saveGenerated trước khi tới được đây. */
    @Override
    @Transactional
    public void incrementUsageCount(UUID id) {
        repository.incrementUsageCount(id);
    }

    @Override
    public Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId) {
        return repository.findQuestionWithTopic(questionId)
            .map(row -> new QuestionEvaluationInfo(
                row.getQuestionText(),
                row.getEvaluationGuideJson(),
                row.getQuestionType(),
                row.getMinResponseSeconds(),
                row.getMaxResponseSeconds(),
                row.getTopicName(),
                row.getTopicDescription()
            ));
    }
}
