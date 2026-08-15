package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricRepository;

@Repository
public class RubricRepositoryImpl implements RubricRepository {

    private final SpringDataRubricRepository springDataRubricRepository;

    public RubricRepositoryImpl(SpringDataRubricRepository springDataRubricRepository) {
        this.springDataRubricRepository = springDataRubricRepository;
    }

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
        var entity = RubricMapper.toJpa(rubric);
        var saved = springDataRubricRepository.save(entity);
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
        Pageable pageable = PageRequest.of(page, size);

        // Gọi xuống Spring Data trả về Page của Spring
        Page<RubricJpaEntity> entityPage = springDataRubricRepository.findAllByOwnerType(ownerType.name(), pageable);

        List<Rubric> rubrics = entityPage.getContent().stream()
                .map(RubricMapper::toDomain)
                .toList();

        // Đóng gói thành PageResult của Domain trả ra ngoài
        return new PageResult<>(
                rubrics,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public PageResult<Rubric> findAllByOwnerTypeAndSchoolId(RubricOwnerType ownerType, UUID schoolId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RubricJpaEntity> entityPage = springDataRubricRepository.findAllByOwnerTypeAndSchoolId(ownerType.name(), schoolId, pageable);

        List<Rubric> rubrics = entityPage.getContent().stream()
                .map(RubricMapper::toDomain)
                .toList();

        return new PageResult<>(
                rubrics,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public PageResult<Rubric> searchSystemRubrics(String keyword, UUID frameworkId, UUID languageId, int page, int size) {
        Pageable springPageable = PageRequest.of(page, size);

        // 2. Gọi DB
       Page<RubricJpaEntity> pageEntity =
                springDataRubricRepository.searchSystemRubrics(keyword, frameworkId, languageId, springPageable);

        //  3. Trả về PageResult chuẩn Domain (Cộng 1 để trả về số trang đếm từ 1 cho Client)
        return new PageResult<>(
                pageEntity.getContent().stream().map(RubricMapper::toDomain).toList(),
                pageEntity.getNumber() + 1, // Trả lại số trang đếm từ 1
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    @Override
    public PageResult<Rubric> searchSchoolRubrics(UUID schoolId, String keyword, UUID frameworkId, UUID languageId, int page, int size) {
        Pageable springPageable = PageRequest.of(page, size);

        // 2. Gọi DB
        Page<RubricJpaEntity> pageEntity =
                springDataRubricRepository.searchSchoolRubrics(schoolId, keyword, frameworkId, languageId, springPageable);

        //  3. Trả về PageResult chuẩn Domain (Cộng 1 để trả về số trang đếm từ 1 cho Client)
        return new PageResult<>(
                pageEntity.getContent().stream().map(RubricMapper::toDomain).toList(),
                pageEntity.getNumber() + 1, // Trả lại số trang đếm từ 1
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }
}
