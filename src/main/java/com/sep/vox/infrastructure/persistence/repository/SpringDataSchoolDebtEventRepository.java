package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolDebtEventJpaEntity;

public interface SpringDataSchoolDebtEventRepository extends JpaRepository<SchoolDebtEventJpaEntity, UUID> {

    // The secondary sort key by id is NOT cosmetic: one assessment can generate many events within
    // the same Instant, and ORDER BY occurred_at alone leaves the order among them undefined -- Postgres
    // may return it differently between two runs. With pagination, the result is one row appearing twice on
    // two pages while another row disappears. The id is a uuidv7 so it grows with time: both stable and
    // not breaking the chronological order.
    Page<SchoolDebtEventJpaEntity> findBySchoolIdOrderByOccurredAtDescIdDesc(UUID schoolId, Pageable pageable);
}
