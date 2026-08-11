package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.InterestQuizItemJpaEntity;

public interface SpringDataInterestQuizItemRepository
        extends JpaRepository<InterestQuizItemJpaEntity, UUID> {

    List<InterestQuizItemJpaEntity> findByIdAndActiveTrue(UUID id);

    // Lấy nguyên kho (giới hạn mềm bằng Top50) chứ không phải Top7: bộ chọn cân bằng cần
    // nhiều ứng viên hơn số câu sẽ hỏi thì mới chọn được bộ phủ đều các chiều.
    List<InterestQuizItemJpaEntity> findTop50ByActiveTrueOrderById();

    List<InterestQuizItemJpaEntity> findTop50ByStudentIdAndActiveTrueOrderById(UUID studentId);

    boolean existsByStudentId(UUID studentId);
}
