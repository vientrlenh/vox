package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
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
    public List<SchoolClassUser> findByUserIdIn(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return springDataSchoolClassUserRepository.findByUserIdIn(userIds)
            .stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, int page, int size) {
        var pageRequest = PageRequest.of(page - 1, size);
        var pageable = springDataSchoolClassUserRepository.findBySchoolClassId(schoolClassId, pageRequest);
        return new PageResult<>(
            pageable.getContent().stream().map(SchoolClassUserMapper::toDomain).toList(), 
            page, 
            size, 
            pageable.getTotalElements(), 
            pageable.getTotalPages()
        );
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

    @Override
    public int deactivateByGradeId(UUID schoolGradeId, Instant leftAt) {
        return springDataSchoolClassUserRepository.deactivateByGradeId(schoolGradeId, leftAt);
    }

    @Override
    public int deactivateBySchoolClassId(UUID schoolClassId, Instant leftAt) {
        return springDataSchoolClassUserRepository.deactivateBySchoolClassId(schoolClassId, leftAt);
    }

    @Override
    public List<SchoolClassUser> saveAll(Collection<SchoolClassUser> schoolClassUsers) {
        if (schoolClassUsers == null || schoolClassUsers.isEmpty()) {
            return List.of();
        }
        var entities = schoolClassUsers.stream()
            .map(SchoolClassUserMapper::toJpa)
            .toList();
        var saved = springDataSchoolClassUserRepository.saveAll(entities);
        return saved.stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }
}
