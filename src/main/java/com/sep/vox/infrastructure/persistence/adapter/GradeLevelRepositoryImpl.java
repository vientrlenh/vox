package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.GradeLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataGradeLevelRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class GradeLevelRepositoryImpl implements GradeLevelRepository {

    private final SpringDataGradeLevelRepository gradeLevelRepository;

    public GradeLevelRepositoryImpl(SpringDataGradeLevelRepository gradeLevelRepository) {
        this.gradeLevelRepository = gradeLevelRepository;
    }

    @Override
    public Optional<GradeLevel> findById(UUID id) {
        return gradeLevelRepository.findById(id).map(GradeLevelMapper::toDomain);
    }

    @Override
    public Optional<GradeLevel> findByCode(String code) {
        return gradeLevelRepository.findByCode(code).map(GradeLevelMapper::toDomain);
    }

    @Override
    public Optional<GradeLevel> findByName(String name) {
        return gradeLevelRepository.findByName(name).map(GradeLevelMapper::toDomain);
    }

    @Override
    public PageResult<GradeLevel> findAll(String search, GradeLevelStatus status, int page, int size) {
        var searchPattern = (search == null || search.isBlank())
            ? null
            : "%" + search.strip().toLowerCase() + "%";
        var statusFilter = status == null ? null : status.name();
        var pageable = PageRequest.of(page - 1, size);
        var result = gradeLevelRepository.findAllWithFilters(searchPattern, statusFilter, pageable);
        return new PageResult<>(
            result.getContent().stream()
                .map(GradeLevelMapper::toDomain)
                .toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<GradeLevel> findByCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        var upperCodes = codes.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return gradeLevelRepository.findByCodeIn(upperCodes)
                .stream()
                .map(GradeLevelMapper::toDomain)
                .toList();
    }

    @Override
    public List<GradeLevel> findByNameIn(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return gradeLevelRepository.findByNameIn(names)
                .stream()
                .map(GradeLevelMapper::toDomain)
                .toList();
    }

    @Override
    public List<GradeLevel> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return gradeLevelRepository.findAllById(ids)
                .stream()
                .map(GradeLevelMapper::toDomain)
                .toList();
    }

    @Override
    public GradeLevel save(GradeLevel gradeLevel) {
        var entity = GradeLevelMapper.toJpa(gradeLevel);
        var savedEntity = gradeLevelRepository.save(entity);
        return GradeLevelMapper.toDomain(savedEntity);
    }

    @Override
    public int updateGradeLevelAtomic(UUID id, String name, String description, Integer order,
            Instant updatedAt, UUID updatedBy) {
        return gradeLevelRepository.updateGradeLevelAtomic(id, name, description, order, updatedAt, updatedBy);
    }

    @Override
    public boolean existsByCode(String code) {
        return gradeLevelRepository.existsByCode(code);
    }

    @Override
    public boolean existsByOrder(int order) {
        return gradeLevelRepository.existsByOrder(order);
    }

    @Override
    public void deleteById(UUID id) {
        gradeLevelRepository.deleteById(id);
    }
}
