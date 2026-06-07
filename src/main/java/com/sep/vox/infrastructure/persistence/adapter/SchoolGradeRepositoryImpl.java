package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
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
    public boolean existsBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolGradeRepository.existsBySchoolIdAndCode(schoolId, code);
    }

    @Override
    public Optional<SchoolGrade> findByIdForUpdate(UUID schoolGradeId, UUID schoolId) {
        return springDataSchoolGradeRepository.findByIdAndSchoolIdForUpdate(schoolGradeId,schoolId)
            .map(SchoolGradeMapper::toDomain);
    }

    @Override
    public PageResult<SchoolGrade> findAllBySchoolId(UUID schoolId, PageRequest pageRequest) {

        // 1. Lấy thông tin từ Domain PageRequest và đổi thành Spring Pageable
        int actualPage = pageRequest.page() - 1; // Spring đếm trang từ 0
        org.springframework.data.domain.Pageable springPageable =
                org.springframework.data.domain.PageRequest.of(actualPage, pageRequest.size());

        // 2. Gọi DB (Nó sẽ trả về Page<SchoolGradeJpaEntity> của Spring)
        org.springframework.data.domain.Page<com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity> pageEntity =
                springDataSchoolGradeRepository.findAllBySchoolId(schoolId, springPageable);

        // 3. Đóng gói lại thành PageResult của Domain để trả về cho UseCase
        return new PageResult<>(
                pageEntity.getContent().stream()
                        .map(SchoolGradeMapper::toDomain) // Map Entity sang Domain
                        .toList(),

                pageEntity.getNumber() + 1, // Trả lại số trang đếm từ 1 cho Client
                pageEntity.getSize(),
                pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    @Override
    public void deleteById(UUID id) {
        springDataSchoolGradeRepository.deleteById(id);
    }

    @Override
    public Optional<SchoolGrade> findByIdForDelete(UUID schoolGradeId) {
        return springDataSchoolGradeRepository.findByIdForDelete(schoolGradeId)
            .map(SchoolGradeMapper::toDomain);
    }

}
