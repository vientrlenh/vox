package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.infrastructure.persistence.entity.SchoolQuotaPolicyJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SchoolQuotaPolicyMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolQuotaPolicyRepository;

@Repository
public class SchoolQuotaPolicyRepositoryImpl implements SchoolQuotaPolicyRepository {

    private final SpringDataSchoolQuotaPolicyRepository springDataSchoolQuotaPolicyRepository;

    public SchoolQuotaPolicyRepositoryImpl(
            SpringDataSchoolQuotaPolicyRepository springDataSchoolQuotaPolicyRepository) {
        this.springDataSchoolQuotaPolicyRepository = springDataSchoolQuotaPolicyRepository;
    }

    @Override
    public SchoolQuotaPolicy findBySchoolIdAndQuotaType(UUID schoolId, QuotaType quotaType) {
        return springDataSchoolQuotaPolicyRepository
            .findBySchoolIdAndQuotaType(schoolId, quotaType.name())
            .map(SchoolQuotaPolicyMapper::toDomain)
            // Chưa đặt gì = chia được toàn bộ. Dựng tại chỗ chứ không ghi xuống: không có dòng và có
            // dòng 1.0 là cùng một nghĩa, ghi thêm chỉ để đọc là đẻ ra một hàng cho mọi trường.
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
        var now = Instant.now();
        var existing = springDataSchoolQuotaPolicyRepository
            .findBySchoolIdAndQuotaType(schoolId, quotaType.name());

        var entity = new SchoolQuotaPolicyJpaEntity(
            existing.map(e -> e.getId()).orElse(null),
            schoolId,
            quotaType.name(),
            distributableRatio,
            existing.map(e -> e.getCreatedAt()).orElse(now),
            now
        );

        return SchoolQuotaPolicyMapper.toDomain(springDataSchoolQuotaPolicyRepository.save(entity));
    }
}
