package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricRepository;

@Repository
@RequiredArgsConstructor
public class RubricRepositoryImpl implements RubricRepository {

    private final SpringDataRubricRepository springDataRubricRepository;

    @Override
    public Optional<Rubric> findById(UUID id) {
        return springDataRubricRepository.findById(id).map(RubricMapper::toDomain);
    }

    @Override
    public List<Rubric> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataRubricRepository.findAllById(ids)
                .stream().map(RubricMapper::toDomain).toList();
    }

    @Override
    public Rubric save(Rubric rubric) {
        RubricJpaEntity entity = RubricMapper.toJpa(rubric);
        RubricJpaEntity saved = springDataRubricRepository.save(entity);
        return RubricMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRubricRepository.deleteById(id);
    }

    @Override
    public boolean existsByOwnerTypeAndSchoolIdAndLanguageId(String ownerType, UUID schoolId, UUID languageId) {
        return springDataRubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageId(ownerType, schoolId, languageId);
    }

    @Override
    public boolean existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
            String ownerType, UUID schoolId, UUID languageId, UUID frameworkId, String code) {
        return springDataRubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                ownerType, schoolId, languageId, frameworkId, code);
    }

    @Override
    public boolean existsByOwnerTypeAndLanguageId(String ownerType, UUID languageId) {
        return springDataRubricRepository.existsByOwnerTypeAndLanguageId(ownerType, languageId);
    }

    // Implement hàm vừa khai báo
    @Override
    public void updateRubricAtomic(UUID id, String name, String description) {
        springDataRubricRepository.updateRubricAtomic(id, name, description);
    }

    @Override
    public PageResult<Rubric> findAllByOwnerType(RubricOwnerType ownerType, int page, int size) {
        Pageable pageable = OneBasedPaging.toPageRequest(page, size);

        // Calls Spring Data, which returns a Spring Page
        Page<RubricJpaEntity> entityPage = springDataRubricRepository.findAllByOwnerType(ownerType.name(), pageable);

        List<Rubric> rubrics = entityPage.getContent().stream()
                .map(RubricMapper::toDomain)
                .toList();

        // Wraps it into the domain PageResult returned to the caller
        return OneBasedPaging.toPageResult(entityPage, rubrics);
    }

    @Override
    public PageResult<Rubric> findAllByOwnerTypeAndSchoolId(RubricOwnerType ownerType, UUID schoolId, int page, int size) {
        Pageable pageable = OneBasedPaging.toPageRequest(page, size);
        Page<RubricJpaEntity> entityPage = springDataRubricRepository.findAllByOwnerTypeAndSchoolId(ownerType.name(), schoolId, pageable);

        List<Rubric> rubrics = entityPage.getContent().stream()
                .map(RubricMapper::toDomain)
                .toList();

        return OneBasedPaging.toPageResult(entityPage, rubrics);
    }

    @Override
    public PageResult<Rubric> searchSystemRubrics(String keyword, UUID frameworkId, UUID languageId, int page, int size) {
        Pageable springPageable = OneBasedPaging.toPageRequest(page, size);

        // 2. Call the DB
       Page<RubricJpaEntity> pageEntity =
                springDataRubricRepository.searchSystemRubrics(keyword, frameworkId, languageId, springPageable);

        //  3. Return the standard domain PageResult (the page number is counted from 1 for the client)
        return OneBasedPaging.toPageResult(pageEntity, pageEntity.getContent().stream().map(RubricMapper::toDomain).toList());
    }

    @Override
    public PageResult<Rubric> searchSchoolRubrics(UUID schoolId, String keyword, UUID frameworkId, UUID languageId, int page, int size) {
        Pageable springPageable = OneBasedPaging.toPageRequest(page, size);

        // 2. Call the DB
        Page<RubricJpaEntity> pageEntity =
                springDataRubricRepository.searchSchoolRubrics(schoolId, keyword, frameworkId, languageId, springPageable);

        //  3. Return the standard domain PageResult (the page number is counted from 1 for the client)
        return OneBasedPaging.toPageResult(pageEntity, pageEntity.getContent().stream().map(RubricMapper::toDomain).toList());
    }
}
