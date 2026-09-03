package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.DimensionInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataDimensionInterestScoreRepository;

@Repository
public class DimensionInterestScoreRepositoryImpl
        implements DimensionInterestScoreRepository {

    private final SpringDataDimensionInterestScoreRepository repository;

    public DimensionInterestScoreRepositoryImpl(
            SpringDataDimensionInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void primeBaselineFromScoreWhereMissing(UUID learnerProfileId) {
        repository.setBaselineFromScoreWhereMissing(learnerProfileId);
    }

    @Override
    public Map<String, Double> findByLearnerProfile(UUID learnerProfileId) {
        return repository.findByLearnerProfileId(learnerProfileId).stream()
            .collect(Collectors.toMap(
                entity -> entity.getDimension(),
                entity -> (entity.getBaselineScore() != null
                    ? entity.getBaselineScore()
                    : entity.getScore()).doubleValue()
            ));
    }

    @Override
    @Transactional
    public void upsertScore(
            UUID learnerProfileId,
            String dimension,
            double score) {
        repository.upsertScore(
            UUID.randomUUID(),
            learnerProfileId,
            dimension,
            BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP)
        );
    }

    @Override
    @Transactional
    public void replaceScores(
            UUID learnerProfileId,
            Map<String, Double> scores) {
        repository.deleteByLearnerProfileId(learnerProfileId);
        // FLUSH bắt buộc trước saveAll -- KHÔNG phải thừa.
        //
        // deleteByLearnerProfileId không có @Modifying nên Spring Data JPA thực hiện nó bằng
        // entityManager.remove() cho từng dòng, tức chỉ XẾP HÀNG một EntityDeleteAction trong
        // Hibernate persistence context, không chạy DELETE ngay. Hibernate luôn thực thi hàng đợi
        // action theo một THỨ TỰ CỐ ĐỊNH lúc flush -- insert TRƯỚC, delete SAU -- bất kể thứ tự
        // gọi trong code Java. Không flush() ở đây thì saveAll() bên dưới chèn dòng mới trong khi
        // dòng cũ (cùng khoá (learner_profile_id, dimension)) vẫn còn nằm trong bảng, vi phạm
        // idx_dimension_interest_profile_dimension.
        //
        // Đo thật: học sinh làm lại quiz sở thích (mọi lần thứ hai trở đi, vì lần đầu bảng còn
        // trống) là hỏng 100% -- DataIntegrityViolationException, GraphQL trả lỗi, transaction
        // rollback sạch nên không mất dữ liệu, nhưng học sinh không cách nào làm lại quiz.
        repository.flush();
        repository.saveAll(scores.entrySet().stream()
            .map(entry -> new DimensionInterestScoreJpaEntity(
                learnerProfileId,
                entry.getKey(),
                BigDecimal.valueOf(entry.getValue()).setScale(4, RoundingMode.HALF_UP),
                null
            ))
            .toList());
    }

}
