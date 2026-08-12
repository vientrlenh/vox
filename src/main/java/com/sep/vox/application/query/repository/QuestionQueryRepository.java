package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionStatusCountInfo;

/**
 * Read-side cho câu hỏi. Tách khỏi {@code QuestionRepository} (write-side) vì các truy vấn
 * ở đây trả về hình dạng phục vụ màn hình chứ không phải aggregate.
 */
public interface QuestionQueryRepository {

    /**
     * Đếm câu hỏi theo status, áp dụng ĐÚNG bộ điều kiện truy cập của
     * {@code SpringDataQuestionRepository.findAccessible}. Hai bên phải khớp tuyệt đối:
     * người dùng bấm vào một con số rồi lọc danh sách theo status đó thì phải ra đúng
     * chừng ấy dòng.
     *
     * <p>Không có tham số {@code status}: nó chính là chiều đang được nhóm.
     *
     * <p>Chỉ trả về status THỰC SỰ có câu hỏi -- {@code GROUP BY} không sinh dòng rỗng.
     * Phía gọi tự bù cho đủ tập status nếu cần một trục ổn định.
     *
     * @param topicNamePattern đã ở dạng LIKE pattern (xem StringNormalization.toLikePattern)
     * @param keywordPattern   đã ở dạng LIKE pattern
     */
    List<QuestionStatusCountInfo> countAccessibleByStatus(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID questionBankId,
        UUID questionTopicId,
        String topicNamePattern,
        String type,
        String sharing,
        String scope,
        String keywordPattern
    );
}
