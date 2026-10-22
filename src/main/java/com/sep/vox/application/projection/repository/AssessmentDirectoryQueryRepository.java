package com.sep.vox.application.projection.repository;

import java.util.Collection;
import java.util.UUID;

import com.sep.vox.application.projection.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.projection.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.domain.shared.PageResult;

/**
 * Read model của danh bạ nguồn thí sinh / giám thị cho một kỳ thi.
 *
 * <p>Tách khỏi các repository ghi vì đây là projection phẳng (đã join sẵn users) —
 * bề mặt GraphQL của danh bạ không được lồng type `User`/`SchoolUser` (những type đó
 * có field resolver gated SCHOOL_ADMIN).
 *
 * <p>`page` là **0-based**: use case trừ 1 khỏi số trang 1-based của client trước khi gọi.
 */
public interface AssessmentDirectoryQueryRepository {

    /** Niên khóa của một trường, lọc theo mã/tên. */
    PageResult<AssessmentDirectoryGradeInfo> findGradesBySchoolId(UUID schoolId, String search, int page, int size);

    /**
     * Người dùng của một trường theo mã vai trò (STUDENT / TEACHER).
     *
     * <p>{@code excludeUserIds} loại người ngay trong SQL để {@code content} và
     * {@code totalElements}/{@code totalPages} khớp nhau — lọc sau khi phân trang sẽ cho ra trang
     * ngắn (thậm chí rỗng) kèm số đếm vẫn tính cả người đã bị loại.
     */
    PageResult<AssessmentDirectoryUserInfo> findUsersBySchoolId(UUID schoolId, String roleCode, String search,
        Collection<UUID> excludeUserIds, int page, int size);

    /**
     * Người dùng đang active trong một tập lớp, theo mã vai trò. Tập lớp rỗng trả về
     * trang rỗng — không được suy thành "toàn trường".
     */
    PageResult<AssessmentDirectoryUserInfo> findUsersByClassIds(Collection<UUID> schoolClassIds, String roleCode,
        String search, Collection<UUID> excludeUserIds, int page, int size);
}
