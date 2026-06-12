package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassUserMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassUserRepository;

@Repository
public class SchoolClassUserRepositoryImpl implements SchoolClassUserRepository {
    private final SpringDataSchoolClassUserRepository springDataSchoolClassUserRepository;

    public SchoolClassUserRepositoryImpl(SpringDataSchoolClassUserRepository springDataSchoolClassUserRepository) {
        this.springDataSchoolClassUserRepository = springDataSchoolClassUserRepository;
    }

    @Override
    public Optional<SchoolClassUser> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId) {
        return springDataSchoolClassUserRepository.findByUserIdAndSchoolClassId(userId, schoolClassId)
            .map(SchoolClassUserMapper::toDomain);
    }

    @Override
    public List<SchoolClassUser> findByUserIdInAndSchoolClassIdIn(Collection<UUID> userIds, Collection<UUID> schoolClassIds) {
        if (userIds == null || userIds.isEmpty() || schoolClassIds == null || schoolClassIds.isEmpty()) {
            return List.of();
        }
        return springDataSchoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(userIds, schoolClassIds)
            .stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClassUser> findByUserId(UUID userId) {
        return springDataSchoolClassUserRepository.findByUserId(userId)
            .stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolClassUser> findBySchoolClassId(UUID schoolClassId) {
        return springDataSchoolClassUserRepository.findBySchoolClassId(schoolClassId)
            .stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsBySchoolClassId(UUID schoolClassId) {
        return springDataSchoolClassUserRepository.existsBySchoolClassId(schoolClassId);
    }

    @Override
    public SchoolClassUser save(SchoolClassUser schoolClassUser) {
        var entity = SchoolClassUserMapper.toJpa(schoolClassUser);
        var saved = springDataSchoolClassUserRepository.save(entity);
        return SchoolClassUserMapper.toDomain(saved);
    }
}
