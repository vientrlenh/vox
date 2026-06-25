package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkVersionRepository;

@Repository
public class FrameworkVersionRepositoryImpl implements FrameworkVersionRepository {

    private final SpringDataFrameworkVersionRepository springDataFrameworkVersionRepository;

    public FrameworkVersionRepositoryImpl(SpringDataFrameworkVersionRepository springDataFrameworkVersionRepository) {
        this.springDataFrameworkVersionRepository = springDataFrameworkVersionRepository;
    }

    @Override
    public Optional<FrameworkVersion> findById(UUID id) {
        return springDataFrameworkVersionRepository.findById(id).map(FrameworkVersionMapper::toDomain);
    }

    @Override
    public Optional<FrameworkVersion> findByIdForUpdate(UUID id) {
        return springDataFrameworkVersionRepository.findByIdForUpdate(id).map(FrameworkVersionMapper::toDomain);
    }

    @Override
    public PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataFrameworkVersionRepository.findByFrameworkId(frameworkId, pageable);
        return new PageResult<>(
            page.getContent().stream().map(FrameworkVersionMapper::toDomain).toList(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public List<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status) {
        return springDataFrameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, status.name())
            .stream().map(FrameworkVersionMapper::toDomain).toList();
    }

    @Override
    public Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version) {
        return springDataFrameworkVersionRepository
            .findByFrameworkIdAndVersion(frameworkId, version)
            .map(FrameworkVersionMapper::toDomain);
    }

    @Override
    public FrameworkVersion save(FrameworkVersion version) {
        var entity = FrameworkVersionMapper.toJpa(version);
        var saved = springDataFrameworkVersionRepository.save(entity);
        return FrameworkVersionMapper.toDomain(saved);
    }

    @Override
    public int updateStatus(UUID id, FrameworkVersionStatus status) {
        return springDataFrameworkVersionRepository.updateStatus(id, status.name());
    }

    @Override
    public void deleteById(UUID id) {
        springDataFrameworkVersionRepository.deleteById(id);
    }

    @Override
    public boolean existsByFrameworkId(UUID frameworkId) {
        return springDataFrameworkVersionRepository.existsByFrameworkId(frameworkId);
    }
}
