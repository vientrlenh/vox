package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassUserJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolClassUserMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Repository
@RequiredArgsConstructor
public class SchoolClassUserRepositoryImpl implements SchoolClassUserRepository {
    private final SpringDataSchoolClassUserRepository springDataSchoolClassUserRepository;

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
        PageRequest pageRequest = OneBasedPaging.toPageRequest(page, size);
        Page<SchoolClassUserJpaEntity> pageable = springDataSchoolClassUserRepository.findBySchoolClassId(schoolClassId, pageRequest);
        return new PageResult<>(
            pageable.getContent().stream().map(SchoolClassUserMapper::toDomain).toList(),
            page,
            size,
            pageable.getTotalElements(),
            pageable.getTotalPages()
        );
    }

    @Override
    public PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, String roleCode, String search,
            int page, int size) {
        // The sort is already in the ORDER BY of the @Query, so the PageRequest here is left unsorted.
        PageRequest pageRequest = OneBasedPaging.toPageRequest(page, size);
        String normalizedSearch = blankToNull(search);
        Page<SchoolClassUserJpaEntity> pageable = springDataSchoolClassUserRepository.findBySchoolClassIdWithFilters(
            schoolClassId,
            blankToNull(roleCode),
            normalizedSearch == null ? null : "%" + normalizedSearch.toLowerCase() + "%",
            pageRequest
        );
        return new PageResult<>(
            pageable.getContent().stream().map(SchoolClassUserMapper::toDomain).toList(),
            page,
            size,
            pageable.getTotalElements(),
            pageable.getTotalPages()
        );
    }

    @Override
    public Map<UUID, Integer> countActiveBySchoolClassIdIn(Collection<UUID> schoolClassIds) {
        if (schoolClassIds == null || schoolClassIds.isEmpty()) {
            return Map.of();
        }
        return springDataSchoolClassUserRepository.countActiveBySchoolClassIdIn(schoolClassIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> ((Number) row[1]).intValue()
            ));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public boolean existsBySchoolClassId(UUID schoolClassId) {
        return springDataSchoolClassUserRepository.existsBySchoolClassId(schoolClassId);
    }

    @Override
    public SchoolClassUser save(SchoolClassUser schoolClassUser) {
        SchoolClassUserJpaEntity entity = SchoolClassUserMapper.toJpa(schoolClassUser);
        SchoolClassUserJpaEntity saved = DataConflictTranslation.translating(() -> springDataSchoolClassUserRepository.save(entity));
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
        List<SchoolClassUserJpaEntity> entities = schoolClassUsers.stream()
            .map(SchoolClassUserMapper::toJpa)
            .toList();
        List<SchoolClassUserJpaEntity> saved = DataConflictTranslation.translating(() -> springDataSchoolClassUserRepository.saveAll(entities));
        return saved.stream()
            .map(SchoolClassUserMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<UUID> findCurrentSchoolGradeId(UUID studentId) {
        return springDataSchoolClassUserRepository.findCurrentSchoolGradeId(studentId);
    }
}
