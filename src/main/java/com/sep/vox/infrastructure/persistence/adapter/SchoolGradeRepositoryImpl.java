package com.sep.vox.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolGradeMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolGradeRepository;

@Repository
public class SchoolGradeRepositoryImpl implements SchoolGradeRepository {

    private final SpringDataSchoolGradeRepository springDataSchoolGradeRepository;

    public SchoolGradeRepositoryImpl(SpringDataSchoolGradeRepository springDataSchoolGradeRepository) {
        this.springDataSchoolGradeRepository = springDataSchoolGradeRepository;
    }

    @Override
    public Optional<SchoolGrade> findById(UUID id) {
        return springDataSchoolGradeRepository.findById(id)
                .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public Optional<SchoolGrade> findBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolGradeRepository.findBySchoolIdAndCode(schoolId, code)
                .map(SchoolGradeMapper::toDomain);
    }


    @Override
    public Optional<SchoolGrade> findBySchoolIdAndName(UUID schoolId, String name) {
        return springDataSchoolGradeRepository.findBySchoolIdAndName(schoolId, name)
                .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public SchoolGrade save(SchoolGrade grade) {
        var entity = SchoolGradeMapper.toJpa(grade);
        var saved = springDataSchoolGradeRepository.save(entity);
        return SchoolGradeMapper.toDomain(saved);
    }


    @Override
    public boolean existsBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code) {
        return springDataSchoolGradeRepository.existsBySchoolIdAndGradeLevelIdAndCode(schoolId, gradeLevelId, code);
    }

    @Override
    public Optional<SchoolGrade> findBySchoolIdAndGradeLevelIdAndCode(UUID schoolId, UUID gradeLevelId, String code) {
        return springDataSchoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(schoolId, gradeLevelId, code)
                .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        var upperCodes = codes.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return springDataSchoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, upperCodes)
                .stream()
                .map(SchoolGradeMapper::toDomain)
                .toList();
    }

    @Override
    public List<SchoolGrade> findBySchoolIdAndNameIn(UUID schoolId, Collection<String> names) {
        if (schoolId == null || names == null || names.isEmpty()) {
            return List.of();
        }
        return springDataSchoolGradeRepository.findBySchoolIdAndNameIn(schoolId, names)
                .stream()
                .map(SchoolGradeMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByGradeLevelId(UUID gradeLevelId) {
        return springDataSchoolGradeRepository.existsByGradeLevelId(gradeLevelId);
    }

    @Override
    public boolean existsByGradeLevelIdAndStatusNot(UUID gradeLevelId, String status) {
        return springDataSchoolGradeRepository.existsByGradeLevelIdAndStatusNot(gradeLevelId, status);
    }

    @Override
    public PageResult<SchoolGrade> findBySchoolId(UUID schoolId, UUID gradeLevelId, String status, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataSchoolGradeRepository.findAllBySchoolId(schoolId, gradeLevelId, status, pageable);
        return new PageResult<>(
                page.getContent().stream()
                        .map(SchoolGradeMapper::toDomain)
                        .toList(),
                pageNumber,
                size,
                page.getTotalElements(),
                page.getTotalPages()
        );
    }


    @Override
    public boolean existsBySchoolIdAndStatus(UUID schoolId, String status) {
        return springDataSchoolGradeRepository.existsBySchoolIdAndStatus(schoolId, status);
    }

    @Override
    public int updateSchoolGradeAtomic(UUID id, String name, String description, LocalDate startDate, LocalDate endDate, Instant now, UUID updatedBy) {
        return springDataSchoolGradeRepository.updateSchoolGradeAtomic(id, name, description, startDate, endDate, now, updatedBy);
    }

    @Override
    public void deleteById(UUID schoolGradeId) {
        springDataSchoolGradeRepository.deleteById(schoolGradeId);
    }

    @Override
    public List<SchoolGrade> findByIdIn(Collection<UUID> ids) {
        return springDataSchoolGradeRepository.findByIdIn(ids)
            .stream()
            .map(SchoolGradeMapper::toDomain)
            .toList();
    }

}
