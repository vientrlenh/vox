package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolGradeLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolGradeLevelRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository; // Quan trọng: Phải có dòng này

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SchoolGradeLevelRepositoryImpl implements SchoolGradeLevelRepository {

    private final SpringDataSchoolGradeLevelRepository schoolGradeLevelRepository;

    public SchoolGradeLevelRepositoryImpl(SpringDataSchoolGradeLevelRepository schoolGradeLevelRepository) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
    }

    @Override
    public Optional<SchoolGradeLevel> findById(UUID id) {
        return schoolGradeLevelRepository.findById(id).map(SchoolGradeLevelMapper::toDomain);
    }

    @Override
    public Optional<SchoolGradeLevel> findBySchoolIdAndCode(UUID schoolId, String code) {
        return schoolGradeLevelRepository.findBySchoolIdAndCode(schoolId, code)
                .map(SchoolGradeLevelMapper::toDomain);
    }

    @Override
    public Optional<SchoolGradeLevel> findBySchoolIdAndName(UUID schoolId, String name) {
        return schoolGradeLevelRepository.findBySchoolIdAndName(schoolId, name)
                .map(SchoolGradeLevelMapper::toDomain);
    }

    @Override
    public PageResult<SchoolGradeLevel> findBySchoolId(UUID schoolId, String search, SchoolGradeLevelStatus status,
            int page, int size) {
        var searchPattern = (search == null || search.isBlank())
            ? null
            : "%" + search.strip().toLowerCase() + "%";
        var statusFilter = status == null ? null : status.name();
        var pageable = PageRequest.of(page - 1, size);
        var result = schoolGradeLevelRepository.findBySchoolIdWithFilters(schoolId, searchPattern, statusFilter, pageable);
        return new PageResult<>(
            result.getContent().stream()
                .map(SchoolGradeLevelMapper::toDomain)
                .toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<SchoolGradeLevel> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        return schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, codes)
                .stream()
                .map(SchoolGradeLevelMapper::toDomain)
                .toList();
    }

    @Override
    public List<SchoolGradeLevel> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return schoolGradeLevelRepository.findAllById(ids)
                .stream()
                .map(SchoolGradeLevelMapper::toDomain)
                .toList();
    }

    @Override
    public SchoolGradeLevel save(SchoolGradeLevel gradeLevel) {
        var entity = SchoolGradeLevelMapper.toJpa(gradeLevel);
        var savedEntity = schoolGradeLevelRepository.save(entity);
        return SchoolGradeLevelMapper.toDomain(savedEntity);
    }

    @Override
    public int updateSchoolGradeLevelAtomic(UUID id, String name, String description, Integer order,
            OffsetDateTime updatedAt, UUID updatedBy) {
        return schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(id, name, description, order, updatedAt, updatedBy);
    }

    @Override
    public boolean existsBySchoolIdAndCode(UUID schoolId, String code) {
        return schoolGradeLevelRepository.existsBySchoolIdAndCode(schoolId, code);
    }

    @Override
    public boolean existsBySchoolIdAndOrder(UUID schoolId, int order) {
        return schoolGradeLevelRepository.existsBySchoolIdAndOrder(schoolId, order);
    }

    @Override
    public void deleteById(UUID id) {
        schoolGradeLevelRepository.deleteById(id);
    }
}
