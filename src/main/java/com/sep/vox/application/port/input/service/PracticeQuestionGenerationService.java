package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient.GeneratedQuestion;
import com.sep.vox.infrastructure.service.PracticeQuestionGenerationClient.TopicDetails;

@Service
public class PracticeQuestionGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeQuestionGenerationService.class);

    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final PracticeQuestionGenerationClient generationClient;

    public PracticeQuestionGenerationService(
            PracticeTopicRepository practiceTopicRepository,
            PracticeQuestionRepository practiceQuestionRepository,
            PracticeQuestionGenerationClient generationClient) {
        this.practiceTopicRepository = practiceTopicRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.generationClient = generationClient;
    }

    // Deliberately NOT @Transactional -- generationClient.generate() is a slow synchronous
    // HTTP call to the Python agents service (LLM generation, can take 10-20s). Wrapping this
    // in a transaction holds a HikariCP connection for that whole wait, starving the pool
    // under concurrent load (confirmed: caused the leak-detector to fire on an unrelated
    // simple SELECT elsewhere while this was in flight). Each repository call below
    // (saveGenerated, findTopicById via topicDetails) still gets its own short-lived
    // transaction from Spring Data's repository proxy -- no atomicity is lost.
    /**
     * @param bandCount số bậc của thang đang áp. Gửi xuống Python để nó ánh xạ
     *                  {@code difficulty_rank} theo đúng thang đó và dựng ladder mô tả bậc từ
     *                  dữ liệu thật, thay vì mặc định thang 6 bậc kiểu VSTEP.
     */
    public int generateAndStore(
            UUID topicId,
            String criterionCode,
            String subAttribute,
            String targetTense,
            int targetRank,
            int count,
            Duration timeout,
            int bandCount,
            List<FrameworkResultBand> bandLadder,
            List<UUID> excludeQuestionIds) {
        var generated = generationClient.generate(
            topicDetails(topicId),
            criterionCode,
            SubAttributePolicy.plannedSubAttribute(criterionCode, subAttribute),
            targetTense,
            targetRank,
            count,
            timeout,
            bandCount,
            bandLadder,
            excludeQuestionIds
        );
        if (generated.isEmpty()) {
            // Python tra 200 kem mang rong la che do hong TE NHAT: vong lap ben duoi khong
            // chay, khong exception, khong log -- roi caller bao "chu de chua co cau luyen
            // phu hop" ma khong ai biet vi sao. Da mat nhieu gio truy nguoc dung ca nay
            // (sub-attribute null bi CandidateFilterNode loai sach). Log ro tai day de lan
            // sau nhin phat ra ngay.
            LOGGER.warn(
                "Pipeline sinh cau tra ve 0 cau (HTTP 2xx) cho topic={} criterion={} "
                    + "subAttribute={} targetRank={} bandCount={} -- khong co gi de luu.",
                topicId,
                criterionCode,
                subAttribute,
                targetRank,
                bandCount
            );
        }
        for (var question : generated) {
            practiceQuestionRepository.saveGenerated(toPracticeQuestion(question));
        }
        for (var question : generated) {
            try {
                generationClient.index(question);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Đã lưu câu {} nhưng chưa cập nhật được Chroma.",
                    question.id(),
                    exception
                );
            }
        }
        return generated.size();
    }

    private TopicDetails topicDetails(UUID topicId) {
        var topic = practiceTopicRepository.findTopicById(topicId)
            .filter(candidate -> candidate.isActive())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
        return new TopicDetails(
            topic.getId(),
            topic.getName(),
            topic.getInterestDimension(),
            topic.getCurriculumGroup()
        );
    }

    private PracticeQuestion toPracticeQuestion(GeneratedQuestion question) {
        return new PracticeQuestion(
            question.id(),
            question.topicId(),
            question.questionText(),
            question.criterionCode(),
            question.subAttribute(),
            question.targetTense(),
            question.difficultyRank(),
            question.difficultyFeaturesJson(),
            question.evaluationGuideJson(),
            question.suggestedIdeasJson(),
            question.questionType(),
            question.maxResponseSeconds(),
            question.minResponseSeconds(),
            question.vstepPart(),
            null,
            0,
            true,
            Instant.now()
        );
    }
}
