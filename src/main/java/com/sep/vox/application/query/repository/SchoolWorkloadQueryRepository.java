package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ExamAwaitingPublishDto;
import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Hai câu hỏi của cùng MỘT tập bài: "toàn trường còn bao nhiêu bài chưa có điểm" và "kỳ nào sắp công
 * bố mà còn bài trống".
 *
 * <p>Đặt chung một cổng vì hai câu phải chạy trên cùng một vị từ. Tách ra hai nơi thì thẻ tổng nói 37
 * còn thẻ kỳ thi cộng lại ra 41, và không ai biết bên nào đúng.
 *
 * <p>Phạm vi là kỳ thi TẬP TRUNG. Bài kiểm tra trên lớp do chính giáo viên ra đề điều phối và có màn
 * riêng ({@code classTestGradingStats}); gộp vào đây sẽ khiến con số trên trang tổng quan không khớp
 * với bảng điều phối mà nó dẫn tới.
 */
public interface SchoolWorkloadQueryRepository {

    /**
     * @param now mốc để xét quá hạn. Truyền vào chứ không tự lấy {@code Instant.now()}: hai câu của
     *            cổng này phải dùng CÙNG một mốc, nếu không một bài có thể vừa "đang trong hạn" ở
     *            thẻ tổng vừa "quá hạn" ở thẻ kỳ thi trong cùng một lần tải trang.
     */
    SchoolUnscoredWorkloadDto countUnscored(UUID schoolId, Instant now);

    /** Kỳ nào còn nhiều bài trống nhất xếp trước — đó là kỳ dễ gây thiệt hại nhất khi bấm công bố. */
    List<ExamAwaitingPublishDto> findExamsAwaitingPublish(UUID schoolId, Instant now, int limit);

    /**
     * Danh sách bài AI chấm lỗi mà chưa ai xử lý — chỗ đáp của dòng cùng tên trên trang tổng quan.
     *
     * <p>Chạy trên ĐÚNG hai nhóm AI của {@link #countUnscored}, nên số trên thẻ và số dòng ở đây luôn
     * khớp. Không nhận {@code now}: hai nhóm AI không phụ thuộc vào mốc thời gian nào (chỉ nhóm phân
     * công mới xét quá hạn), nên thêm tham số vào đây chỉ ngụ ý sai rằng nó có ảnh hưởng.
     *
     * @param examId       thu hẹp về một kỳ; null là mọi kỳ.
     * @param retryLeft    true = chỉ bài còn lượt AI, false = chỉ bài đã hết lượt, null = không lọc.
     *                     Đây là bộ lọc đáng giá nhất trên màn này: hai nhóm dẫn tới hai hành động
     *                     khác hẳn nhau, và trộn chúng lại thì mỗi dòng phải đọc riêng mới biết bấm
     *                     được nút nào.
     */
    PageResult<SchoolGradingFailureDto> findUnhandledAiFailures(
        UUID schoolId, UUID examId, Boolean retryLeft, int page, int size);

    /**
     * Số bài của hai nhóm định mức, ĐÃ áp bộ lọc kỳ thi nhưng CHƯA áp bộ lọc định mức.
     *
     * <p>Hai con số này đứng trên chính hai nút lọc, nên chúng phải dự đoán đúng kết quả của cú bấm:
     * lấy tổng toàn trường trong khi danh sách đang lọc theo một kỳ sẽ cho ra nút ghi 9 mà bấm vào
     * chỉ có 3 dòng.
     *
     * @return {@code [số bài còn lượt AI, số bài hết lượt AI]}
     */
    int[] countAiFailuresByAllowance(UUID schoolId, UUID examId);
}
