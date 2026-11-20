package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentAwaitingPublishDto;
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
    List<AssessmentAwaitingPublishDto> findAssessmentsAwaitingPublish(UUID schoolId, Instant now, int limit);

    /**
     * The list of AI-failed attempts that nobody has handled yet -- the landing place of the line of the same name on the overview page.
     *
     * <p>Runs on EXACTLY the two AI groups of {@link #countUnscored}, so the number on the card and the number of rows here always
     * match. It does not take {@code now}: the two AI groups do not depend on any moment in time (only the assignment
     * group considers overdue), so adding a parameter here would wrongly imply that it has an effect.
     *
     * @param assessmentId narrows to one assessment; null is every assessment.
     * @param retryLeft    true = only attempts with AI retries left, false = only attempts that have run out, null = no filter.
     *                     This is the most valuable filter on this screen: the two groups lead to two very different
     *                     actions, and mixing them means every row has to be read separately to know which
     *                     button can be pressed.
     */
    PageResult<SchoolGradingFailureDto> findUnhandledAiFailures(
        UUID schoolId, UUID assessmentId, Boolean retryLeft, int page, int size);

    /**
     * Số bài của hai nhóm định mức, ĐÃ áp bộ lọc kỳ thi nhưng CHƯA áp bộ lọc định mức.
     *
     * <p>Hai con số này đứng trên chính hai nút lọc, nên chúng phải dự đoán đúng kết quả của cú bấm:
     * lấy tổng toàn trường trong khi danh sách đang lọc theo một kỳ sẽ cho ra nút ghi 9 mà bấm vào
     * chỉ có 3 dòng.
     *
     * @return {@code [số bài còn lượt AI, số bài hết lượt AI]}
     */
    int[] countAiFailuresByAllowance(UUID schoolId, UUID assessmentId);
}
