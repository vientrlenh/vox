package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassRepository;

@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private final SpringDataSchoolClassRepository springDataSchoolClassRepository;

    public SchoolClassRepositoryImpl(SpringDataSchoolClassRepository springDataSchoolClassRepository) {
        this.springDataSchoolClassRepository = springDataSchoolClassRepository;
    }

    @Override
    public Optional<SchoolClass> findById(UUID id) {
        return springDataSchoolClassRepository.findById(id)
            .map(SchoolClassMapper::toDomain);
    }

    

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        var entity = SchoolClassMapper.toJpa(schoolClass);
        var saved = springDataSchoolClassRepository.save(entity);
        return SchoolClassMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataSchoolClassRepository.deleteById(id);
    }

    @Override
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, PageRequest pageRequest) {
        return findBySchoolId(schoolId, null, null, null, null, pageRequest);
    }

    @Override
    public PageResult<SchoolClass> findBySchoolId(UUID schoolId, String search, SchoolClassStatus status,
            UUID languageId, UUID schoolGradeId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page() - 1,
            pageRequest.size(),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        var normalizedSearch = blankToNull(search);
        var page = normalizedSearch == null
            ? springDataSchoolClassRepository.findBySchoolIdWithFilters(
                schoolId,
                valueOf(status),
                languageId,
                schoolGradeId,
                pageable
            )
            : springDataSchoolClassRepository.findBySchoolIdWithSearchAndFilters(
                schoolId,
                "%" + normalizedSearch.toLowerCase() + "%",
                valueOf(status),
                languageId,
                schoolGradeId,
                pageable
            );
        return new PageResult<>(
            page.getContent().stream()
                .map(SchoolClassMapper::toDomain)
                .toList(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public Optional<SchoolClass> findBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolClassRepository.findBySchoolIdAndCode(schoolId, code)
            .map(SchoolClassMapper::toDomain);
    }

    @Override
    public List<SchoolClass> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        return springDataSchoolClassRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .stream()
            .map(SchoolClassMapper::toDomain)
            .toList();
    }

    @Override
    public int updateMutableFields(UUID id, UUID schoolId, String name, boolean nameProvided,
            String description, boolean descriptionProvided, SchoolClassStatus status, boolean statusProvided,
            OffsetDateTime updatedAt, UUID updatedBy) {
        return springDataSchoolClassRepository.updateMutableFields(
            id,
            schoolId,
            name,
            nameProvided,
            description,
            descriptionProvided,
            valueOf(status),
            statusProvided,
            updatedAt,
            updatedBy
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
        var fromRow = (page - 1) * size + 1;
        var toRow = page * size;
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
    public List<SchoolClass> findAllById(List<UUID> schoolIds) {
        return springDataSchoolClassRepository.findAllById(schoolIds)
                .stream()
                .map(SchoolClassMapper::toDomain)
                .toList();
    }

}
