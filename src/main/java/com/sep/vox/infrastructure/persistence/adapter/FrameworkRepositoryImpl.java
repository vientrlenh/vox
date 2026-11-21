package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

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
    public Optional<Framework> findFrameworkByIdForUpdate(UUID id) {
        return springDataFrameworkRepository.findByIdForUpdate(id)
                .map(FrameworkMapper::toDomain);
    }

    @Override
    public Optional<Framework> findByCode(String code) {
        return springDataFrameworkRepository.findByCode(code).map(FrameworkMapper::toDomain);
    }

    @Override
    public PageResult<Framework> findAll(int pageNumber, int size, String search, Boolean isActive) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size);
        String pattern = StringNormalization.toLikePattern(search);
        Page<FrameworkJpaEntity> page = isActive != null
            ? springDataFrameworkRepository.findAllBySearchAndIsActive(pattern, isActive, pageable)
            : springDataFrameworkRepository.findAllBySearch(pattern, pageable);
        return OneBasedPaging.toPageResult(page, page.getContent().stream().map(FrameworkMapper::toDomain).toList());
    }

    @Override
    public PageResult<Framework> findAllActive(int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size);
        Page<FrameworkJpaEntity> page = springDataFrameworkRepository.findAllActive(pageable);
        return OneBasedPaging.toPageResult(page, page.getContent().stream().map(FrameworkMapper::toDomain).toList());
    }

    @Override
    public Framework save(Framework framework) {
        FrameworkJpaEntity entity = FrameworkMapper.toJpa(framework);
        FrameworkJpaEntity saved = DataConflictTranslation.translating(() -> springDataFrameworkRepository.save(entity));
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
