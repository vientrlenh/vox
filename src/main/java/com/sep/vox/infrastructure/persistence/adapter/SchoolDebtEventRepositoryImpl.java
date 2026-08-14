package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.SchoolDebtEvent;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolDebtEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolDebtEventRepository;

@Repository
public class SchoolDebtEventRepositoryImpl implements SchoolDebtEventRepository {

    private final SpringDataSchoolDebtEventRepository springDataSchoolDebtEventRepository;

    public SchoolDebtEventRepositoryImpl(SpringDataSchoolDebtEventRepository springDataSchoolDebtEventRepository) {
        this.springDataSchoolDebtEventRepository = springDataSchoolDebtEventRepository;
    }

    @Override
    public SchoolDebtEvent save(SchoolDebtEvent event) {
        var entity = SchoolDebtEventMapper.toJpa(event);
        var saved = springDataSchoolDebtEventRepository.save(entity);
        return SchoolDebtEventMapper.toDomain(saved);
    }

    @Override
    public List<SchoolDebtEvent> findAllBySchoolId(UUID schoolId) {
        return springDataSchoolDebtEventRepository.findAllBySchoolId(schoolId).stream()
            .map(SchoolDebtEventMapper::toDomain)
            .toList();
    }
}
