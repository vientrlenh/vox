package com.sep.vox.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

import com.sep.vox.domain.common.PageResult;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

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
    public SchoolGrade save(SchoolGrade grade) {
        var entity = SchoolGradeMapper.toJpa(grade);
        var saved = springDataSchoolGradeRepository.save(entity);
        return SchoolGradeMapper.toDomain(saved);
    }


    @Override
    public boolean existsBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code) {
        return springDataSchoolGradeRepository.existsBySchoolGradeLevelIdAndCode(schoolGradeLevelId, code);
    }

    @Override
    public Optional<SchoolGrade> findBySchoolGradeLevelIdAndCode(UUID schoolGradeLevelId, String code) {
        return springDataSchoolGradeRepository.findBySchoolGradeLevelIdAndCode(schoolGradeLevelId, code)
                .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public List<SchoolGrade> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes) {
        if (schoolId == null || codes == null || codes.isEmpty()) {
            return List.of();
        }
        return springDataSchoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, codes)
                .stream()
                .map(SchoolGradeMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySchoolGradeLevelId(UUID schoolGradeLevelId) {
        return springDataSchoolGradeRepository.existsBySchoolGradeLevelId(schoolGradeLevelId);
    }

    @Override
    public PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, int pageNumber, int size) {

        // 1. Lấy thông tin từ Domain PageRequest và đổi thành Spring Pageable
        int actualPage = pageNumber - 1; // Spring đếm trang từ 0
        var pageable = PageRequest.of(actualPage, size);

        // 2. Gọi DB (Nó sẽ trả về Page<SchoolGradeJpaEntity> của Spring)
        var page = springDataSchoolGradeRepository.findAllBySchoolId(schoolId, pageable);

        // 3. Đóng gói lại thành PageResult của Domain để trả về cho UseCase
        return new PageResult<>(
                page.getContent().stream()
                        .map(SchoolGradeMapper::toDomain) // Map Entity sang Domain
                        .toList(),

                pageNumber, // Trả lại số trang đếm từ 1 cho Client
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
    public int updateSchoolGradeAtomic(UUID id, String name, String description, LocalDate startDate, LocalDate endDate, OffsetDateTime now, UUID updatedBy) {
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
