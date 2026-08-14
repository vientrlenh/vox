package com.sep.vox.application.port.output;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;

import tools.jackson.databind.JsonNode;

/**
 * Sinh câu luyện mới bằng AI và đánh chỉ mục câu vừa sinh. Implement bởi
 * PracticeQuestionGenerationClient.
 *
 * <p>KHÁC các port AI còn lại: port này KHÔNG suy giảm êm -- lỗi gọi dịch vụ ném
 * IllegalStateException, vì không sinh được câu thì không có gì để lưu và phía gọi cần biết.
 */
public interface PracticeQuestionGenerationPort {

    /**
     * @param bandCount  số bậc của thang đang áp -- Python dùng để ánh xạ difficulty_rank ra đúng
     *                   thang, thay vì mặc định 6 bậc kiểu VSTEP.
     * @param bandLadder mô tả từng bậc, để prompt chấm nói đúng thang của trường. Rỗng thì Python
     *                   tự lùi về ladder mặc định của nó.
     */
    List<GeneratedQuestion> generate(
        TopicDetails topic,
        String criterionCode,
        String subAttribute,
        String targetTense,
        int targetRank,
        int count,
        Duration timeout,
        int bandCount,
        List<FrameworkResultBand> bandLadder,
        List<UUID> excludeQuestionIds);

    void index(GeneratedQuestion question);

    record TopicDetails(
        UUID id,
        String name,
        String interestDimension,
        String curriculumGroup
    ) {
    }

    /**
     * sourceJson giữ nguyên cây JSON thô Python trả về -- {@link #index} gửi lại đúng cây đó cho
     * Chroma, nên không đổi được sang String (sẽ thành chuỗi lồng chuỗi).
     */
    record GeneratedQuestion(
        UUID id,
        UUID topicId,
        String questionText,
        String criterionCode,
        String subAttribute,
        String targetTense,
        int difficultyRank,
        String difficultyFeaturesJson,
        String evaluationGuideJson,
        String suggestedIdeasJson,
        String questionType,
        int maxResponseSeconds,
        int minResponseSeconds,
        int vstepPart,
        JsonNode sourceJson
    ) {
    }
}
