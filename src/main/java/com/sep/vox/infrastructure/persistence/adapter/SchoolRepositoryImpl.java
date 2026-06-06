package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
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
    public PageResult<School> findAll(int page, int size) {
        var pageRequest = PageRequest.of(page - 1, size);
        var pageable = springDataSchoolRepository.findAll(pageRequest);
        return new PageResult<>(
            pageable.getContent().stream()
                .map(SchoolMapper::toDomain)
                .toList(),
            pageable.getNumber() + 1,
            pageable.getSize(),
            pageable.getTotalElements(),
            pageable.getTotalPages()
        );
    }

    @Override
    public School save(School school) {
        var entity = SchoolMapper.toJpa(school);
        var saved = springDataSchoolRepository.save(entity);
        return SchoolMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataSchoolRepository.existsById(id);
    }

    @Override
    public boolean existsByDomain(String domain) {
        return springDataSchoolRepository.existsByDomain(domain);
    }

    @Override
    public List<School> findByIdIn(Collection<UUID> ids, int page, int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByIdIn'");
    }
    
}
