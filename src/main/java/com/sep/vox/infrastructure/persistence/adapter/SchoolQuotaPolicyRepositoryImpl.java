package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolQuotaPolicyJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SchoolQuotaPolicyMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolQuotaPolicyRepository;

@Repository
@RequiredArgsConstructor
public class SchoolQuotaPolicyRepositoryImpl implements SchoolQuotaPolicyRepository {

    private final SpringDataSchoolQuotaPolicyRepository springDataSchoolQuotaPolicyRepository;

    @Override
    public SchoolQuotaPolicy findBySchoolIdAndQuotaType(UUID schoolId, QuotaType quotaType) {
        return springDataSchoolQuotaPolicyRepository
            .findBySchoolIdAndQuotaType(schoolId, quotaType.name())
            .map(SchoolQuotaPolicyMapper::toDomain)
            // Nothing set = the whole amount is distributable. Built in place rather than written: no row and a
            // row with 1.0 mean the same thing, and writing one just to read it would create a row for every school.
            .orElseGet(() -> SchoolQuotaPolicy.fullyDistributable(schoolId, quotaType));
    }

    @Override
    public List<SchoolQuotaPolicy> findBySchoolId(UUID schoolId) {
        return springDataSchoolQuotaPolicyRepository.findBySchoolId(schoolId).stream()
            .map(SchoolQuotaPolicyMapper::toDomain)
            .toList();
    }

    @Override
    public SchoolQuotaPolicy upsertRatio(UUID schoolId, QuotaType quotaType, BigDecimal distributableRatio) {
        Instant now = Instant.now();
        Optional<SchoolQuotaPolicyJpaEntity> existing = springDataSchoolQuotaPolicyRepository
            .findBySchoolIdAndQuotaType(schoolId, quotaType.name());

        SchoolQuotaPolicyJpaEntity entity = SchoolQuotaPolicyJpaEntity.builder()
            .id(existing.map(e -> e.getId()).orElse(null))
            .schoolId(schoolId)
            .quotaType(quotaType.name())
            .distributableRatio(distributableRatio)
            .createdAt(existing.map(e -> e.getCreatedAt()).orElse(now))
            .updatedAt(now)
            .build();

        return SchoolQuotaPolicyMapper.toDomain(springDataSchoolQuotaPolicyRepository.save(entity));
    }
}
