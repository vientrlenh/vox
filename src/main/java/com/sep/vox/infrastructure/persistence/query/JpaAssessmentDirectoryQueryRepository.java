package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.application.query.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.VnSearchKey;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaAssessmentDirectoryQueryRepository implements AssessmentDirectoryQueryRepository {

    /** Trần size để một client gõ `size: 100000` không kéo cả trường về. */
    private static final int MAX_PAGE_SIZE = 100;

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<AssessmentDirectoryGradeInfo> findGradesBySchoolId(UUID schoolId, String search, int page, int size) {
        String pattern = likePattern(search);
        String filter = """
            FROM SchoolGradeJpaEntity sg
            WHERE sg.schoolId = :schoolId
              AND sg.status = 'ACTIVE'
              AND (:pattern IS NULL
                  OR vn_search_key(sg.code) LIKE :pattern
                  OR vn_search_key(sg.name) LIKE :pattern)
            """;

        TypedQuery<AssessmentDirectoryGradeInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AssessmentDirectoryGradeInfo(
                sg.id, sg.code, sg.name, sg.status)
            """ + filter + " ORDER BY sg.startDate DESC, sg.code ASC", AssessmentDirectoryGradeInfo.class)
            .setParameter("schoolId", schoolId)
            .setParameter("pattern", pattern);

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(sg) " + filter, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("pattern", pattern);

        return paginate(contentQuery, countQuery, page, size);
    }

    @Override
    public PageResult<AssessmentDirectoryUserInfo> findUsersBySchoolId(UUID schoolId, String roleCode, String search,
            Collection<UUID> excludeUserIds, int page, int size) {
        String pattern = likePattern(search);
        List<UUID> excluded = normalizeExclusions(excludeUserIds);
        String filter = """
            FROM SchoolUserJpaEntity su
            JOIN UserJpaEntity u
                ON u.id = su.userId
            WHERE su.schoolId = :schoolId
              AND u.status = 'ACTIVE'
              AND EXISTS (
                  SELECT 1 FROM UserRoleJpaEntity ur
                  JOIN RoleJpaEntity r ON r.id = ur.roleId
                  WHERE ur.userId = u.id AND r.code = :roleCode)
              AND (:pattern IS NULL
                  OR vn_search_key(u.fullName) LIKE :pattern
                  OR vn_search_key(u.email) LIKE :pattern)
            """ + excludeClause(excluded);

        TypedQuery<AssessmentDirectoryUserInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo(
                su.userId, u.fullName, u.email, u.status)
            """ + filter + " ORDER BY u.fullName ASC, u.email ASC", AssessmentDirectoryUserInfo.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(su) " + filter, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        bindExclusions(contentQuery, countQuery, excluded);
        return paginate(contentQuery, countQuery, page, size);
    }

    @Override
    public PageResult<AssessmentDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
            String search, Collection<UUID> excludeUserIds, int page, int size) {
        // IN () is not valid in JPQL, and the correct meaning of "in charge of no class" is
        // "sees nobody" -- it must never fall back to the whole-school scope.
        if (schoolClassIds == null || schoolClassIds.isEmpty()) {
            return new PageResult<>(List.of(), Math.max(page, 1), normalizeSize(size), 0, 0);
        }

        String pattern = likePattern(search);
        List<UUID> excluded = normalizeExclusions(excludeUserIds);
        // Filter classes with EXISTS rather than JOIN: a student enrolled in several classes of the set would be
        // multiplied by a JOIN, corrupting both the page content and totalElements.
        String filter = """
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
              AND (:pattern IS NULL
                  OR vn_search_key(u.fullName) LIKE :pattern
                  OR vn_search_key(u.email) LIKE :pattern)
            """ + excludeClause(excluded);

        TypedQuery<AssessmentDirectoryUserInfo> contentQuery = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo(
                u.id, u.fullName, u.email, u.status)
            """ + filter + " ORDER BY u.fullName ASC, u.email ASC", AssessmentDirectoryUserInfo.class)
            .setParameter("schoolClassIds", schoolClassIds)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(u) " + filter, Long.class)
            .setParameter("schoolClassIds", schoolClassIds)
            .setParameter("roleCode", roleCode)
            .setParameter("pattern", pattern);

        bindExclusions(contentQuery, countQuery, excluded);
        return paginate(contentQuery, countQuery, page, size);
    }

    /** `page` arrives here 1-based; setFirstResult is the 0-based one. */
    private <T> PageResult<T> paginate(TypedQuery<T> contentQuery, TypedQuery<Long> countQuery, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = normalizeSize(size);

        List<T> content = contentQuery
            .setFirstResult(OneBasedPaging.toFirstResult(normalizedPage, normalizedSize))
            .setMaxResults(normalizedSize)
            .getResultList();
        Long totalElements = countQuery.getSingleResult();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);

        return new PageResult<>(content, normalizedPage, normalizedSize, totalElements, totalPages);
    }

    private static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static List<UUID> normalizeExclusions(Collection<UUID> excludeUserIds) {
        if (excludeUserIds == null) {
            return List.of();
        }
        return excludeUserIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    /**
     * Ghép mệnh đề loại trừ vào ĐÚNG một chuỗi filter mà cả câu lấy dữ liệu lẫn câu COUNT dùng
     * chung — hai câu lệch điều kiện là nguồn gốc của "trang trống mà số đếm khác 0".
     *
     * <p>Tập rỗng thì không ghép gì: {@code IN ()} không hợp lệ ở JPQL.
     */
    private static String excludeClause(List<UUID> excluded) {
        return excluded.isEmpty() ? "" : "  AND u.id NOT IN :excludeUserIds\n";
    }

    private static void bindExclusions(TypedQuery<?> contentQuery, TypedQuery<?> countQuery, List<UUID> excluded) {
        if (excluded.isEmpty()) {
            return;
        }
        contentQuery.setParameter("excludeUserIds", excluded);
        countQuery.setParameter("excludeUserIds", excluded);
    }

    /**
     * Từ khoá phải bỏ dấu đúng như cột đang được so (hàm SQL {@code vn_search_key}), nếu không thì
     * gõ không dấu "nguyen van an" sẽ không khớp "Nguyễn Văn An" — xem {@link VnSearchKey}.
     */
    private static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + VnSearchKey.of(search) + "%";
    }
}
