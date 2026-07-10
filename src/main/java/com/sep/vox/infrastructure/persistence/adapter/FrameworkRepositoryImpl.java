package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkRepository;

@Repository
public class FrameworkRepositoryImpl implements FrameworkRepository {

    private final SpringDataFrameworkRepository springDataFrameworkRepository;

    public FrameworkRepositoryImpl(SpringDataFrameworkRepository springDataFrameworkRepository) {
        this.springDataFrameworkRepository = springDataFrameworkRepository;
    }

    @Override
    public Optional<Framework> findById(UUID id) {
        return springDataFrameworkRepository.findById(id)
                .map(FrameworkMapper::toDomain);
    }

    @Override
    public Optional<Framework> findByCode(String code) {
        return springDataFrameworkRepository.findByCode(code).map(FrameworkMapper::toDomain);
    }

    @Override
    public PageResult<Framework> findAll(int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataFrameworkRepository.findAll(pageable);
        return new PageResult<>(
                page.getContent().stream().map(FrameworkMapper::toDomain).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public PageResult<Framework> findAllActive(int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataFrameworkRepository.findAllActive(pageable);
        return new PageResult<>(
                page.getContent().stream().map(FrameworkMapper::toDomain).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public Framework save(Framework framework) {
        var entity = FrameworkMapper.toJpa(framework);
        var saved = springDataFrameworkRepository.save(entity);
        return FrameworkMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataFrameworkRepository.deleteById(id);
    }

    @Override
    public List<Framework> findByIdIn(Collection<UUID> ids) {
        return springDataFrameworkRepository.findByIdIn(ids)
                .stream()
                .map(FrameworkMapper::toDomain)
                .toList();

    }
}
