package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.util.PageRequest;
import com.sep.vox.domain.util.PageResult;
import com.sep.vox.infrastructure.persistence.mapper.SchoolMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolRepository;

@Repository
public class SchoolRepositoryImpl implements SchoolRepository {

    private final SpringDataSchoolRepository springDataSchoolRepository;

    public SchoolRepositoryImpl(SpringDataSchoolRepository springDataSchoolRepository) {
        this.springDataSchoolRepository = springDataSchoolRepository;
    }

    @Override
    public Optional<School> findById(UUID id) {
        return springDataSchoolRepository.findById(id)
            .map(SchoolMapper::toDomain);
    }

    @Override
    public Optional<School> findByCode(String code) {
        return springDataSchoolRepository.findByCode(code)
            .map(SchoolMapper::toDomain);
    }

    @Override
    public Optional<School> findByDomain(String domain) {
        return springDataSchoolRepository.findByDomain(domain)
            .map(SchoolMapper::toDomain);
    }

    @Override
    public PageResult<School> findAll(PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = springDataSchoolRepository.findAll(pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(SchoolMapper::toDomain)
                .toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public School save(School school) {
        var entity = SchoolMapper.toJpa(school);
        var saved = springDataSchoolRepository.save(entity);
        return SchoolMapper.toDomain(saved);
    }
    
}
