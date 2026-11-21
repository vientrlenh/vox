package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceEntryJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolBalanceEntryMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolBalanceEntryRepository;

@Repository
public class SchoolBalanceEntryRepositoryImpl implements SchoolBalanceEntryRepository {

    private final SpringDataSchoolBalanceEntryRepository springDataSchoolBalanceEntryRepository;

    public SchoolBalanceEntryRepositoryImpl(SpringDataSchoolBalanceEntryRepository springDataSchoolBalanceEntryRepository) {
        this.springDataSchoolBalanceEntryRepository = springDataSchoolBalanceEntryRepository;
    }

    @Override
    public Optional<SchoolBalanceEntry> findById(UUID id) {
        return springDataSchoolBalanceEntryRepository.findById(id).map(SchoolBalanceEntryMapper::toDomain);
    }

    @Override
    public SchoolBalanceEntry save(SchoolBalanceEntry entry) {
        SchoolBalanceEntryJpaEntity entity = SchoolBalanceEntryMapper.toJpa(entry);
        SchoolBalanceEntryJpaEntity saved = springDataSchoolBalanceEntryRepository.save(entity);
        return SchoolBalanceEntryMapper.toDomain(saved);
    }

    @Override
    public PageResult<SchoolBalanceEntry> findBySchoolId(
            UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to, int page, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(page, size);
        Page<SchoolBalanceEntryJpaEntity> result = entryType == null
            ? springDataSchoolBalanceEntryRepository.findPageBySchoolIdInRange(schoolId, from, to, pageable)
            : springDataSchoolBalanceEntryRepository
                .findPageBySchoolIdAndEntryTypeInRange(schoolId, entryType.name(), from, to, pageable);

        return new PageResult<>(
            result.getContent().stream().map(SchoolBalanceEntryMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<SchoolBalanceEntry> findBySchoolIdInRange(UUID schoolId, Instant from, Instant to) {
        return springDataSchoolBalanceEntryRepository.findBySchoolIdInRange(schoolId, from, to).stream()
            .map(SchoolBalanceEntryMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByOrderIdAndEntryType(UUID orderId, SchoolBalanceEntryType entryType) {
        return springDataSchoolBalanceEntryRepository.existsByOrderIdAndEntryType(orderId, entryType.name());
    }

    @Override
    public BigDecimal sumAmountBySchoolIdAndEntryTypeInRange(
            UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to) {
        return springDataSchoolBalanceEntryRepository
            .sumAmountBySchoolIdAndEntryTypeInRange(schoolId, entryType.name(), from, to);
    }
}
