package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolDebtEvent;
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
    public PageResult<SchoolDebtEvent> findBySchoolId(UUID schoolId, int page, int size) {
        var result = springDataSchoolDebtEventRepository
            // page vào theo lối 1-BASED như mọi repository khác trong dự án, PageRequest đếm từ 0 --
            // xem SchoolBalanceEntryRepositoryImpl.findBySchoolId.
            .findBySchoolIdOrderByOccurredAtDescIdDesc(schoolId, PageRequest.of(page - 1, size));

        return new PageResult<>(
            result.getContent().stream().map(SchoolDebtEventMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
