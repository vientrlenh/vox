package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolDebtEventJpaEntity;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.repository.SchoolDebtEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolDebtEventMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolDebtEventRepository;

@Repository
@RequiredArgsConstructor
public class SchoolDebtEventRepositoryImpl implements SchoolDebtEventRepository {

    private final SpringDataSchoolDebtEventRepository springDataSchoolDebtEventRepository;

    @Override
    public SchoolDebtEvent save(SchoolDebtEvent event) {
        SchoolDebtEventJpaEntity entity = SchoolDebtEventMapper.toJpa(event);
        SchoolDebtEventJpaEntity saved = springDataSchoolDebtEventRepository.save(entity);
        return SchoolDebtEventMapper.toDomain(saved);
    }

    @Override
    public PageResult<SchoolDebtEvent> findBySchoolId(UUID schoolId, int page, int size) {
        Page<SchoolDebtEventJpaEntity> result = springDataSchoolDebtEventRepository
            .findBySchoolIdOrderByOccurredAtDescIdDesc(schoolId, OneBasedPaging.toPageRequest(page, size));

        return new PageResult<>(
            result.getContent().stream().map(SchoolDebtEventMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
