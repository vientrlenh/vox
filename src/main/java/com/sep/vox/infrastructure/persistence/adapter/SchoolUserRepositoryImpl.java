package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolUserMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolUserRepository;

@Repository
public class SchoolUserRepositoryImpl implements SchoolUserRepository {

    private final SpringDataSchoolUserRepository springDataSchoolUserRepository;

    public SchoolUserRepositoryImpl(SpringDataSchoolUserRepository springDataSchoolUserRepository) {
        this.springDataSchoolUserRepository = springDataSchoolUserRepository;
    }

    @Override
    public Optional<SchoolUser> findByUserId(UUID userId) {
        return springDataSchoolUserRepository.findByUserId(userId)
            .map(SchoolUserMapper::toDomain);
    }

    @Override
    public List<SchoolUser> findByUserIdIn(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return springDataSchoolUserRepository.findByUserIdIn(userIds)
            .stream()
            .map(SchoolUserMapper::toDomain)
            .toList();
    }

    @Override
    public SchoolUser save(SchoolUser schoolUser) {
        var entity = SchoolUserMapper.toJpa(schoolUser);
        var saved = springDataSchoolUserRepository.save(entity);
        return SchoolUserMapper.toDomain(saved);
    }

    @Override
    public List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size) {
        var fromRow = (page - 1) * size + 1;
        var toRow = page * size;
        return springDataSchoolUserRepository.findBySchoolIdIn(schoolIds, fromRow, toRow)
            .stream()
            .map(SchoolUserMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SchoolUser> findBySchoolId(UUID schoolId, int page, int size) {
        var pageRequest = PageRequest.of(page - 1, size);
        var pageable = springDataSchoolUserRepository.findBySchoolId(schoolId, pageRequest);
        return new PageResult<>(
            pageable.getContent()
                .stream()
                .map(SchoolUserMapper::toDomain)
                .toList(), 
            page, 
            size, 
            pageable.getTotalElements(), 
            pageable.getTotalPages()
        );
    }

    @Override
    public Optional<SchoolUser> findBySchoolIdAndUserId(UUID schoolId, UUID userId) {
        return springDataSchoolUserRepository.findBySchoolIdAndUserId(schoolId, userId)
            .map(SchoolUserMapper::toDomain);
    }

    @Override
    public Optional<UUID> findSchoolIdByUserId(UUID userId) {
        return springDataSchoolUserRepository.findSchoolIdByUserId(userId);
    }

}
