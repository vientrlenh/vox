package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassMapper;
import com.sep.vox.infrastructure.persistence.audit.AuditStampProvider;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassRepository;

@Repository
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private static final Sort NEWEST_FIRST =
        Sort.by(Sort.Direction.DESC, (SchoolClassJpaEntity entity) -> entity.getCreatedAt())
            .and(Sort.by(Sort.Direction.ASC, (SchoolClassJpaEntity entity) -> entity.getId()));

    private final SpringDataSchoolClassRepository springDataSchoolClassRepository;
    private final AuditStampProvider auditStampProvider;

    @Override
    public Optional<SchoolClass> findById(UUID id) {
        return springDataSchoolClassRepository.findById(id)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassJpaEntity entity = SchoolClassMapper.toJpa(schoolClass);
        SchoolClassJpaEntity saved = springDataSchoolClassRepository.saveAndFlush(entity);
        return SchoolClassMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataSchoolClassRepository.deleteById(id);
    }

    @Override
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, int pageNumber, int size) {
        return findBySchoolId(schoolId, null, null, null, null, pageNumber, size);
    }

    @Override
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, String search, SchoolClassStatus status,
            UUID languageId, UUID schoolGradeId, int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size, NEWEST_FIRST);
        String normalizedSearch = blankToNull(search);
        Page<SchoolClassJpaEntity> page = normalizedSearch == null
            ? springDataSchoolClassRepository.findBySchoolIdWithFilters(
                schoolId, valueOf(status), languageId, schoolGradeId, pageable)
            : springDataSchoolClassRepository.findBySchoolIdWithSearchAndFilters(
                schoolId, "%" + normalizedSearch.toLowerCase() + "%", valueOf(status), languageId, schoolGradeId,
                pageable);
        return toPageResult(page, pageNumber, size);
    }

    @Override
    public PageResult<SchoolClass> findByUserId(UUID schoolId, UUID userId, SchoolClassStatus status, int pageNumber, int size) {
        return findByUserId(schoolId, userId, null, status, pageNumber, size);
    }

    @Override
    public PageResult<SchoolClass> findByUserId(UUID schoolId, UUID userId, String search, SchoolClassStatus status,
            int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size, NEWEST_FIRST);
        String normalizedSearch = blankToNull(search);
        Page<SchoolClassJpaEntity> page = normalizedSearch == null
            ? springDataSchoolClassRepository.findByUserId(schoolId, userId, valueOf(status), pageable)
            : springDataSchoolClassRepository.findByUserIdWithSearch(
                schoolId, userId, "%" + normalizedSearch.toLowerCase() + "%", valueOf(status), pageable);
        return toPageResult(page, pageNumber, size);
    }

    @Override
    public Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolClassRepository.findBySchoolIdAndCode(schoolId, code)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public Optional<SchoolClass> findBySchoolIdAndName(UUID schoolId, String name) {
        return springDataSchoolClassRepository.findBySchoolIdAndName(schoolId, name)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<String> upperCodes = codes.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return springDataSchoolClassRepository.findBySchoolIdAndCodeIn(schoolId, upperCodes)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names) {
        if (schoolId == null || names == null || names.isEmpty()) {
            return List.of();
        }
        return springDataSchoolClassRepository.findBySchoolIdAndNameIn(schoolId, names)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public int updateMutableFields(UUID id, UUID schoolId, String name, boolean nameProvided,
            String description, boolean descriptionProvided, SchoolClassStatus status, boolean statusProvided) {
        return springDataSchoolClassRepository.updateMutableFields(
            id,
            schoolId,
            name,
            nameProvided,
            description,
            descriptionProvided,
            valueOf(status),
            statusProvided,
            auditStampProvider.currentUpdatedAt(),
            auditStampProvider.currentUpdatedBy()
        );
    }

    private static PageResult<SchoolClass> toPageResult(Page<SchoolClassJpaEntity> page, int pageNumber, int size) {
        return new PageResult<>(
            page.getContent().stream()
                .map(SchoolClassMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    private static String valueOf(SchoolClassStatus status) {
        return status == null ? null : status.name();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public List<SchoolClass> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size) {
        int fromRow = OneBasedPaging.toFirstRow(page, size);
        int toRow = OneBasedPaging.toLastRow(page, size);
        return springDataSchoolClassRepository.findBySchoolIdIn(schoolIds, fromRow, toRow)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsBySchoolGradeId(UUID schoolGradeId) {
        return springDataSchoolClassRepository.existsBySchoolGradeId(schoolGradeId);
    }

    @Override
    public boolean existsBySchoolIdAndStatus(UUID schoolId, String status) {
        return springDataSchoolClassRepository.existsBySchoolIdAndStatus(schoolId, status);
    }

    @Override
    public int archiveByGradeId(UUID schoolGradeId) {
        return springDataSchoolClassRepository.archiveByGradeId(
            schoolGradeId, auditStampProvider.currentUpdatedAt(), auditStampProvider.currentUpdatedBy());
    }


    @Override
    public List<SchoolClass> findAllById(List<UUID> schoolIds) {
        return springDataSchoolClassRepository.findAllById(schoolIds)
                .stream()
                .map(SchoolClassMapper::toDomain)
                .toList();
    }

}
