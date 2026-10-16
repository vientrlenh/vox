package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealItemJpaEntity;

public interface SpringDataAssessmentResultAppealItemRepository
        extends JpaRepository<AssessmentResultAppealItemJpaEntity, UUID> {

    /** id là uuidv7 nên order theo id giữ đúng thứ tự học sinh chọn phần thi. */
    List<AssessmentResultAppealItemJpaEntity> findByAppealIdOrderByIdAsc(UUID appealId);

    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
