package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.application.query.repository.ExamDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaExamDirectoryQueryRepository implements ExamDirectoryQueryRepository {

    /** Trần size để một client gõ `size: 100000` không kéo cả trường về. */
    private static final int MAX_PAGE_SIZE = 100;

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<ExamDirectoryGradeInfo> findGradesBySchoolId(UUID schoolId, String search, int page, int size) {
        var pattern = likePattern(search);
        var filter = """
            FROM SchoolGradeJpaEntity sg
            WHERE sg.schoolId = :schoolId
              AND sg.status = 'ACTIVE'
              AND (:pattern IS NULL OR LOWER(sg.code) LIKE :pattern OR LOWER(sg.name) LIKE :pattern)
            """;

        TypedQuery<ExamDirectoryGradeInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.ExamDirectoryGradeInfo(
                sg.id, sg.code, sg.name, sg.status)
            """ + filter + " ORDER BY sg.startDate DESC, sg.code ASC", ExamDirectoryGradeInfo.class)
            .setParameter("schoolId", schoolId)
            .setParameter("pattern", pattern);

        var countQuery = em.createQuery("SELECT COUNT(sg) " + filter, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("pattern", pattern);

        return paginate(contentQuery, countQuery, page, size);
    }

    @Override
    public PageResult<ExamDirectoryUserInfo> findUsersBySchoolId(UUID schoolId, String roleCode, String search,
            int page, int size) {
        var pattern = likePattern(search);
        var filter = """
            FROM SchoolUserJpaEntity su
            JOIN UserJpaEntity u
                ON u.id = su.userId
            WHERE su.schoolId = :schoolId
              AND u.status = 'ACTIVE'
              AND EXISTS (
                  SELECT 1 FROM UserRoleJpaEntity ur
                  JOIN RoleJpaEntity r ON r.id = ur.roleId
                  WHERE ur.userId = u.id AND r.code = :roleCode)
              AND (:pattern IS NULL OR LOWER(u.fullName) LIKE :pattern OR LOWER(u.email) LIKE :pattern)
            """;

        TypedQuery<ExamDirectoryUserInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.ExamDirectoryUserInfo(
                su.userId, u.fullName, u.email, u.status)
            """ + filter + " ORDER BY u.fullName ASC, u.email ASC", ExamDirectoryUserInfo.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        var countQuery = em.createQuery("SELECT COUNT(su) " + filter, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        return paginate(contentQuery, countQuery, page, size);
    }

    @Override
    public PageResult<ExamDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
            String search, int page, int size) {
        // IN () không hợp lệ ở JPQL, và ngữ nghĩa đúng của "không phụ trách lớp nào" là
        // "không thấy ai" — tuyệt đối không được rơi về phạm vi toàn trường.
        if (schoolClassIds == null || schoolClassIds.isEmpty()) {
            return new PageResult<>(List.of(), Math.max(page, 1), normalizeSize(size), 0, 0);
        }

        var pattern = likePattern(search);
        // Lọc lớp bằng EXISTS chứ không JOIN: học sinh học nhiều lớp trong tập sẽ bị JOIN
        // nhân dòng, làm sai cả nội dung trang lẫn totalElements.
        var filter = """
            FROM UserJpaEntity u
            WHERE u.status = 'ACTIVE'
              AND EXISTS (
                  SELECT 1 FROM SchoolClassUserJpaEntity scu
                  WHERE scu.userId = u.id
                    AND scu.schoolClassId IN :schoolClassIds
                    AND scu.isActive = true)
              AND EXISTS (
                  SELECT 1 FROM UserRoleJpaEntity ur
                  JOIN RoleJpaEntity r ON r.id = ur.roleId
                  WHERE ur.userId = u.id AND r.code = :roleCode)
              AND (:pattern IS NULL OR LOWER(u.fullName) LIKE :pattern OR LOWER(u.email) LIKE :pattern)
            """;

        TypedQuery<ExamDirectoryUserInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.ExamDirectoryUserInfo(
                u.id, u.fullName, u.email, u.status)
            """ + filter + " ORDER BY u.fullName ASC, u.email ASC", ExamDirectoryUserInfo.class)
            .setParameter("schoolClassIds", schoolClassIds)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        var countQuery = em.createQuery("SELECT COUNT(u) " + filter, Long.class)
            .setParameter("schoolClassIds", schoolClassIds)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        return paginate(contentQuery, countQuery, page, size);
    }

    /** `page` vào đây là 1-based; setFirstResult mới là 0-based. */
    private <T> PageResult<T> paginate(TypedQuery<T> contentQuery, TypedQuery<Long> countQuery, int page, int size) {
        var normalizedPage = Math.max(page, 1);
        var normalizedSize = normalizeSize(size);

        var content = contentQuery
            .setFirstResult((normalizedPage - 1) * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();
        var totalElements = countQuery.getSingleResult();
        var totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);

        return new PageResult<>(content, normalizedPage, normalizedSize, totalElements, totalPages);
    }

    private static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
    }
}
