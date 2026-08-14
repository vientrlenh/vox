package com.sep.vox.application.port.output;

import java.util.List;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Sinh câu hỏi quiz sở thích theo tình huống bằng AI. Implement bởi InterestQuizGenerationClient.
 *
 * <p>Suy giảm êm giống TopicGenerationPort: dịch vụ AI chết thì trả danh sách rỗng chứ không ném.
 */
public interface InterestQuizGenerationPort {

    /**
     * @param dimensions danh mục chiều sở thích hiện hành. Gửi xuống thay vì để Python gắn cứng,
     *                   nhờ vậy SYSTEM_ADMIN thêm chiều mới là có hiệu lực ngay.
     */
    List<InterestQuizSeedItem> generate(
        int maxItems,
        List<String> existingStatements,
        List<String> dimensions);
}
