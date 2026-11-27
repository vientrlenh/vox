package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolBalanceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolBalanceRepository;

@Repository
@RequiredArgsConstructor
public class SchoolBalanceRepositoryImpl implements SchoolBalanceRepository {

    private final SpringDataSchoolBalanceRepository springDataSchoolBalanceRepository;

    @Override
    public Optional<SchoolBalance> findById(UUID id) {
        return springDataSchoolBalanceRepository.findById(id).map(SchoolBalanceMapper::toDomain);
    }

    @Override
    public Optional<SchoolBalance> findBySchoolId(UUID schoolId) {
        return springDataSchoolBalanceRepository.findBySchoolId(schoolId).map(SchoolBalanceMapper::toDomain);
    }

    /**
     * INSERT ... ON CONFLICT DO NOTHING first, then lock: two statements, but the first writes exactly once for
     * the whole life of the school and later calls cost only one unique-constraint check.
     */
    @Override
    public SchoolBalance findBySchoolIdForUpdateOrCreate(UUID schoolId, Instant now) {
        springDataSchoolBalanceRepository.insertIfAbsent(schoolId, now);
        return springDataSchoolBalanceRepository.findWithLockBySchoolId(schoolId)
            .map(SchoolBalanceMapper::toDomain)
            .orElseThrow(() -> new IllegalStateException(
                "Could not read the balance of school " + schoolId + " right after ensuring the row exists"));
    }

    @Override
    public SchoolBalance save(SchoolBalance balance) {
        SchoolBalanceJpaEntity entity = SchoolBalanceMapper.toJpa(balance);
        SchoolBalanceJpaEntity saved = springDataSchoolBalanceRepository.save(entity);
        return SchoolBalanceMapper.toDomain(saved);
    }
}
