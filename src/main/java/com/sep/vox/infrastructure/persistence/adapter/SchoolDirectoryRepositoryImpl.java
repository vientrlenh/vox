package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolDirectoryMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolDirectoryRepository;

@Repository
public class SchoolDirectoryRepositoryImpl implements SchoolDirectoryRepository {

    private final SpringDataSchoolDirectoryRepository springDataSchoolDirectoryRepository;

    public SchoolDirectoryRepositoryImpl(SpringDataSchoolDirectoryRepository springDataSchoolDirectoryRepository) {
        this.springDataSchoolDirectoryRepository = springDataSchoolDirectoryRepository;
    }

    @Override
    public Optional<SchoolDirectory> findById(UUID id) {
        return springDataSchoolDirectoryRepository.findById(id)
            .map(SchoolDirectoryMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataSchoolDirectoryRepository.existsById(id);
    }

    @Override
    public SchoolDirectory save(SchoolDirectory sd) {
        var entity = SchoolDirectoryMapper.toJpa(sd);
        var saved = springDataSchoolDirectoryRepository.save(entity);
        return SchoolDirectoryMapper.toDomain(saved);
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataSchoolDirectoryRepository.existsByCode(code);
    }

    @Override
    public List<SchoolDirectory> findByCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return springDataSchoolDirectoryRepository.findByCodeIn(codes).stream()
            .map(SchoolDirectoryMapper::toDomain)
            .toList();
    }

}
