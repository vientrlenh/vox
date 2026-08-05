package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.personalization.TopicInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.TopicInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestScoreRepository;

@Repository
public class TopicInterestScoreRepositoryImpl
        implements TopicInterestScoreRepository {

    private final SpringDataTopicInterestScoreRepository repository;

    public TopicInterestScoreRepositoryImpl(
            SpringDataTopicInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForStudent(
            UUID studentId,
            List<TopicInterestScoreEntry> scores) {
        repository.deleteByStudentId(studentId);
        // ĐẨY LỆNH XOÁ XUỐNG DB TRƯỚC KHI CHÈN.
        //
        // Không có dòng này thì Hibernate gom cả hai vào một lần flush ở cuối transaction, và
        // ActionQueue của nó luôn chạy INSERT TRƯỚC DELETE. Nên các dòng mới đâm vào chính
        // những dòng cũ lẽ ra đã bị xoá -> duplicate key trên
        // uq_topic_interest_score_student_topic (student_id, practice_topic_id).
        //
        // Chỉ lộ ra từ lần thứ HAI trở đi: lần đầu bảng còn rỗng nên không có gì để đâm vào.
        // Đúng vì thế mà nó ẩn lâu -- recordSessionOutcome trước đây chỉ chạy khi học sinh bấm
        // "Hoàn tất", mà điều đó mới xảy ra đúng một lần.
        repository.flush();
        var now = Instant.now();
        repository.saveAll(scores.stream()
            .map(entry -> new TopicInterestScoreJpaEntity(
                UUID.randomUUID(),
                studentId,
                entry.getTopicId(),
                BigDecimal.valueOf(entry.getScore()),
                entry.getSessionCount(),
                entry.getLastEventAt(),
                now
            ))
            .toList());
    }
}
