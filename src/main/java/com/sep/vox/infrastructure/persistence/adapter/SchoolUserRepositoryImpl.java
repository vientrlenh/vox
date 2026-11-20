package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolUserMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
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
        SchoolUserJpaEntity entity = SchoolUserMapper.toJpa(schoolUser);
        SchoolUserJpaEntity saved = springDataSchoolUserRepository.save(entity);
        return SchoolUserMapper.toDomain(saved);
    }

    @Override
    public List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int pageNumber, int size) {
        int fromRow = OneBasedPaging.toFirstRow(pageNumber, size);
        int toRow = OneBasedPaging.toLastRow(pageNumber, size);
        return springDataSchoolUserRepository.findBySchoolIdIn(schoolIds, fromRow, toRow)
            .stream()
            .map(SchoolUserMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SchoolUser> findBySchoolId(UUID schoolId, String search, UUID roleId, String status,
            UUID excludeClassId, boolean excludeAdminRoles, int page, int size) {
        String searchPattern = (search == null || search.isBlank())
            ? null
            : "%" + search.strip().toLowerCase() + "%";
        String statusFilter = (status == null || status.isBlank()) ? null : status;
        PageRequest pageRequest = OneBasedPaging.toPageRequest(page, size);
        Page<SchoolUserJpaEntity> pageable = springDataSchoolUserRepository.searchBySchoolId(
            schoolId, null, searchPattern, roleId, statusFilter, excludeClassId, excludeAdminRoles, pageRequest);
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
    public PageResult<SchoolUser> searchBySchoolId(UUID schoolId, UUID excludeUserId, String search,
            UUID roleId, String status, int page, int size) {
        String searchPattern = (search == null || search.isBlank())
            ? null
            : "%" + search.strip().toLowerCase() + "%";
        String statusFilter = (status == null || status.isBlank()) ? null : status;
        PageRequest pageRequest = OneBasedPaging.toPageRequest(page, size);
        Page<SchoolUserJpaEntity> pageable = springDataSchoolUserRepository.searchBySchoolId(
            schoolId, excludeUserId, searchPattern, roleId, statusFilter, null, false, pageRequest);
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


    @Override
    public boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId) {
        return springDataSchoolUserRepository.existsBySchoolIdAndUserId(schoolId, userId);
    }

    @Override
    public List<SchoolUser> findBySchoolIdWithRole(UUID schoolId, String roleCode) {
        return springDataSchoolUserRepository.findBySchoolIdWithRole(schoolId, roleCode)
            .stream()
            .map(SchoolUserMapper::toDomain)
            .toList();
    }

}
