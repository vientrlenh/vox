package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SchoolUserInfo;
import com.sep.vox.application.query.repository.SchoolUserQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSchoolUserQueryRepository implements SchoolUserQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<SchoolUserInfo> findBySchoolIdAndRoleCodes(UUID schoolId, List<String> roleCodes, PageRequest pageRequest) {
        var actualPage = Math.max(pageRequest.page() - 1, 0);
        var size = pageRequest.size();

        var items = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.SchoolUserInfo(
                u.id,
                u.email,
                u.phone,
                u.fullName,
                r.code,
                u.status,
                su.schoolId,
                u.createdAt,
                u.id,
                su.startDate,
                su.endDate
            )
            FROM UserJpaEntity u
            JOIN UserRoleJpaEntity ur
                ON u.id = ur.userId
            JOIN RoleJpaEntity r
                ON ur.roleId = r.id
            JOIN SchoolUserJpaEntity su
                ON su.userId = u.id
            WHERE su.schoolId = :schoolId
                AND r.code IN :roleCodes
            ORDER BY u.createdAt DESC
        """, SchoolUserInfo.class)
        .setParameter("schoolId", schoolId)
        .setParameter("roleCodes", roleCodes)
        .setFirstResult(actualPage * size)
        .setMaxResults(size)
        .getResultList();

        var totalElements = em.createQuery("""
            SELECT COUNT(DISTINCT u.id)
            FROM UserJpaEntity u
            JOIN UserRoleJpaEntity ur
                ON u.id = ur.userId
            JOIN RoleJpaEntity r
                ON ur.roleId = r.id
            JOIN SchoolUserJpaEntity su
                ON su.userId = u.id
            WHERE su.schoolId = :schoolId
                AND r.code IN :roleCodes
        """, Long.class)
        .setParameter("schoolId", schoolId)
        .setParameter("roleCodes", roleCodes)
        .getSingleResult();

        var totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return new PageResult<>(
            items,
            actualPage,
            size,
            totalElements,
            totalPages
        );
    }
}
