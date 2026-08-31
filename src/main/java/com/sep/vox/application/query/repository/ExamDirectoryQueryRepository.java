package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.UUID;

import com.sep.vox.application.query.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read model của danh bạ nguồn thí sinh / giám thị cho một kỳ thi.
 *
 * <p>Tách khỏi các repository ghi vì đây là projection phẳng (đã join sẵn users) —
 * bề mặt GraphQL của danh bạ không được lồng type `User`/`SchoolUser` (những type đó
 * có field resolver gated SCHOOL_ADMIN).
 *
 * <p>`page` là **1-based**, khớp với mọi repository phân trang khác trong repo này.
 */
public interface ExamDirectoryQueryRepository {

    /** Niên khóa của một trường, lọc theo mã/tên. */
    PageResult<ExamDirectoryGradeInfo> findGradesBySchoolId(UUID schoolId, String search, int page, int size);

    /**
     * Người dùng của một trường theo mã vai trò (STUDENT / TEACHER).
     *
     * <p>{@code excludeUserIds} loại người ngay trong SQL để {@code content} và
     * {@code totalElements}/{@code totalPages} khớp nhau — lọc sau khi phân trang sẽ cho ra trang
     * ngắn (thậm chí rỗng) kèm số đếm vẫn tính cả người đã bị loại.
     */
    PageResult<ExamDirectoryUserInfo> findUsersBySchoolId(UUID schoolId, String roleCode, String search,
        Collection<UUID> excludeUserIds, int page, int size);

    /**
     * Người dùng đang active trong một tập lớp, theo mã vai trò. Tập lớp rỗng trả về
     * trang rỗng — không được suy thành "toàn trường".
     */
    PageResult<ExamDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
        String search, Collection<UUID> excludeUserIds, int page, int size);
}
