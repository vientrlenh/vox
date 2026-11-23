package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import java.time.Duration;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.GeneratedQuestion;
import com.sep.vox.application.port.output.PracticeQuestionGenerationPort.TopicDetails;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.service.personalization.SubAttributePolicy;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeQuestionGenerationService {

    private final PracticeTopicRepository practiceTopicRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final PracticeQuestionGenerationPort questionGenerationPort;

    // Deliberately NOT @Transactional -- questionGenerationPort.generate() is a slow synchronous
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
        List<PracticeQuestionGenerationPort.GeneratedQuestion> generated = questionGenerationPort.generate(
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
            log.warn(
                "Pipeline sinh cau tra ve 0 cau (HTTP 2xx) cho topic={} criterion={} "
                    + "subAttribute={} targetRank={} bandCount={} -- khong co gi de luu.",
                topicId,
                criterionCode,
                subAttribute,
                targetRank,
                bandCount
            );
        }
        for (PracticeQuestionGenerationPort.GeneratedQuestion question : generated) {
            practiceQuestionRepository.saveGenerated(toPracticeQuestion(question));
        }
        for (PracticeQuestionGenerationPort.GeneratedQuestion question : generated) {
            try {
                questionGenerationPort.index(question);
            } catch (RuntimeException exception) {
                log.warn(
                    "Đã lưu câu {} nhưng chưa cập nhật được Chroma.",
                    question.id(),
                    exception
                );
            }
        }
        return generated.size();
    }

    private TopicDetails topicDetails(UUID topicId) {
        PracticeTopic topic = practiceTopicRepository.findTopicById(topicId)
            .filter(candidate -> candidate.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề luyện tập."));
        return new TopicDetails(
            topic.getId(),
            topic.getName(),
            topic.getInterestDimension(),
            topic.getCurriculumGroup()
        );
    }

    private PracticeQuestion toPracticeQuestion(GeneratedQuestion question) {
        return PracticeQuestion.builder()
                .id(question.id())
                .practiceTopicId(question.topicId())
                .questionText(question.questionText())
                .targetCriterionCode(question.criterionCode())
                .targetSubAttribute(question.subAttribute())
                .targetTense(question.targetTense())
                .difficultyRank(question.difficultyRank())
                .difficultyFeaturesJson(question.difficultyFeaturesJson())
                .evaluationGuideJson(question.evaluationGuideJson())
                .suggestedIdeasJson(question.suggestedIdeasJson())
                .questionType(question.questionType())
                .maxResponseSeconds(question.maxResponseSeconds())
                .minResponseSeconds(question.minResponseSeconds())
                .vstepPart(question.vstepPart())
                .source(null)
                .usageCount(0)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
