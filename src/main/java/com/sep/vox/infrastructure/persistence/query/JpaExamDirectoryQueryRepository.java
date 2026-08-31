package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.application.query.repository.ExamDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.VnSearchKey;

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
              AND (:pattern IS NULL
                  OR vn_search_key(sg.code) LIKE :pattern
                  OR vn_search_key(sg.name) LIKE :pattern)
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
            Collection<UUID> excludeUserIds, int page, int size) {
        var pattern = likePattern(search);
        var excluded = normalizeExclusions(excludeUserIds);
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
              AND (:pattern IS NULL
                  OR vn_search_key(u.fullName) LIKE :pattern
                  OR vn_search_key(u.email) LIKE :pattern)
            """ + excludeClause(excluded);

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

        bindExclusions(contentQuery, countQuery, excluded);
        return paginate(contentQuery, countQuery, page, size);
    }

    @Override
    public PageResult<ExamDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
            String search, Collection<UUID> excludeUserIds, int page, int size) {
        // IN () không hợp lệ ở JPQL, và ngữ nghĩa đúng của "không phụ trách lớp nào" là
        // "không thấy ai" — tuyệt đối không được rơi về phạm vi toàn trường.
        if (schoolClassIds == null || schoolClassIds.isEmpty()) {
            return new PageResult<>(List.of(), Math.max(page, 1), normalizeSize(size), 0, 0);
        }

        var pattern = likePattern(search);
        var excluded = normalizeExclusions(excludeUserIds);
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
              AND (:pattern IS NULL
                  OR vn_search_key(u.fullName) LIKE :pattern
                  OR vn_search_key(u.email) LIKE :pattern)
            """ + excludeClause(excluded);

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

        bindExclusions(contentQuery, countQuery, excluded);
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
