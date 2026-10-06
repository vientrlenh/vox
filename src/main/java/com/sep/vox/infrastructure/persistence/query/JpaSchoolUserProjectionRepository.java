package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.projection.dto.SchoolUserInfo;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.domain.common.PageResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSchoolUserProjectionRepository implements SchoolUserProjectionRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<SchoolUserInfo> findBySchoolIdAndRoleCodes(UUID schoolId, List<String> roleCodes, int page, int size) {

        var content = em.createQuery("""
            SELECT DISTINCT new com.sep.vox.application.query.dto.SchoolUserInfo(
                su.id,
                u.email,
                u.phone,
                u.fullName,
                r.code,
                u.status,
                su.schoolId,
                u.createdAt,
                su.userId,
                su.startDate,
                su.endDate
            )
            FROM SchoolUserJpaEntity su
            JOIN UserJpaEntity u
                ON u.id = su.userId
            JOIN UserRoleJpaEntity ur
                ON ur.userId = u.id
            JOIN RoleJpaEntity r
                ON r.id = ur.roleId
            WHERE su.schoolId = :schoolId
              AND r.code IN :roleCodes
            ORDER BY u.createdAt DESC
        """, SchoolUserInfo.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCodes", roleCodes)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size)
            .getResultList();

        var totalElements = em.createQuery("""
            SELECT COUNT(DISTINCT su.id)
            FROM SchoolUserJpaEntity su
            JOIN UserRoleJpaEntity ur
                ON ur.userId = su.userId
            JOIN RoleJpaEntity r
                ON r.id = ur.roleId
            WHERE su.schoolId = :schoolId
              AND r.code IN :roleCodes
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCodes", roleCodes)
            .getSingleResult();

        var totalPages = totalElements == 0
            ? 0
            : (int) Math.ceil((double) totalElements / size);

        return new PageResult<>(content, page, size, totalElements, totalPages);
    }

    @Override
    public Optional<UUID> findSchoolIdByUserId(UUID userId) {
        UUID schoolId = em.createQuery("""
            SELECT su.schoolId FROM SchoolUserJpaEntity su 
            WHERE su.userId = :userId 
        """, UUID.class)
            .setParameter("userId", userId)
            .getSingleResult();
        if (schoolId == null) {
            return Optional.empty();
        }
        return Optional.of(schoolId);
    }
}
