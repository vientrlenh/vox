package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolBalanceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolBalanceRepository;

@Repository
public class SchoolBalanceRepositoryImpl implements SchoolBalanceRepository {

    private final SpringDataSchoolBalanceRepository springDataSchoolBalanceRepository;

    public SchoolBalanceRepositoryImpl(SpringDataSchoolBalanceRepository springDataSchoolBalanceRepository) {
        this.springDataSchoolBalanceRepository = springDataSchoolBalanceRepository;
    }

    @Override
    public Optional<SchoolBalance> findById(UUID id) {
        return springDataSchoolBalanceRepository.findById(id).map(SchoolBalanceMapper::toDomain);
    }

    @Override
    public Optional<SchoolBalance> findBySchoolId(UUID schoolId) {
        return springDataSchoolBalanceRepository.findBySchoolId(schoolId).map(SchoolBalanceMapper::toDomain);
    }

    @Override
    public Optional<SchoolBalance> findBySchoolIdForUpdate(UUID schoolId) {
        return springDataSchoolBalanceRepository.findWithLockBySchoolId(schoolId).map(SchoolBalanceMapper::toDomain);
    }

    @Override
    public SchoolBalance save(SchoolBalance balance) {
        var entity = SchoolBalanceMapper.toJpa(balance);
        var saved = springDataSchoolBalanceRepository.save(entity);
        return SchoolBalanceMapper.toDomain(saved);
    }
}
