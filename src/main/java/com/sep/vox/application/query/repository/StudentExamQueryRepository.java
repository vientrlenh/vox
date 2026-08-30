package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.query.dto.StudentExamRowInfo;
import com.sep.vox.domain.common.PageResult;

/**
 * Read side cho danh sách bài thi của học sinh. Join xuyên aggregate
 * (candidate × exam × schedule) nên đi lối query repository thay vì domain repository.
 *
 * <p>Vì sao phải phân trang ở đây chứ không ở use case: bản trước nạp TOÀN BỘ candidate của học
 * sinh, nạp tiếp mọi exam và schedule liên quan, lọc/sắp trong bộ nhớ rồi mới {@code skip/limit}.
 * Chi phí một lần mở màn tỉ lệ với số bài thi cả đời của em đó, trong khi màn hình chỉ hiện 20
 * dòng. Đẩy xuống SQL thì cả bộ lọc, khoá sắp xếp lẫn {@code COUNT} chạy trên chỉ mục.
 *
 * <p>Phân trang 1-BASED: trang đầu là 1, khớp quy ước chung của dự án (xem
 * {@code PagingConventionTests}). Chỗ quy đổi sang lối đếm từ 0 của JPA nằm trong adapter.
 */
public interface StudentExamQueryRepository {

    /**
     * @param examKind      {@code CENTRALIZED} / {@code CLASS_TEST}; {@code null} = cả hai.
     * @param derivedStatus {@code upcoming} / {@code in_progress} / {@code completed};
     *                      {@code null} = không lọc. Đây là trạng thái SUY RA từ giờ thi, không
     *                      phải cột {@code status} của kỳ thi.
     * @param now           mốc thời gian dùng để suy ra trạng thái -- truyền vào để một lần gọi
     *                      chỉ có đúng một "bây giờ", tránh lệch giữa câu đếm và câu lấy dòng.
     */
    PageResult<StudentExamRowInfo> findMyExams(
        UUID studentId,
        String examKind,
        String derivedStatus,
        boolean sortDescending,
        int page,
        int size,
        Instant now);
}
