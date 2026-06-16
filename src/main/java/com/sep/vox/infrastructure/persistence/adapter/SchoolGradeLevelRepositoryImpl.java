package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolGradeLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolGradeLevelRepository;
import org.springframework.stereotype.Repository; // Quan trọng: Phải có dòng này

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
        // Tui viết luôn ruột để bạn gọi sau này không bị lỗi NullPointerException
        return schoolGradeLevelRepository.findBySchoolIdAndCode(schoolId, code)
                .map(SchoolGradeLevelMapper::toDomain);
    }

    @Override
    public SchoolGradeLevel save(SchoolGradeLevel gradeLevel) {
        var entity = SchoolGradeLevelMapper.toJpa(gradeLevel);
        var savedEntity = schoolGradeLevelRepository.save(entity);
        return SchoolGradeLevelMapper.toDomain(savedEntity);
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
