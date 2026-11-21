package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;


@Service
public class MissingResponseBackfillService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MissingResponseBackfillService.class);

    /**
     * Nhãn ghi vào {@code termination_reason} -- phân biệt với câu thí sinh có làm nhưng bị cắt
     * giữa chừng ({@code redirect_offtopic}, {@code timeout}...). Đây là câu chưa từng bắt đầu.
     */
    public static final String NO_RECORDING = "no_recording";

    /**
     * {@code engine_type} mượn AI_SINGLE vì cột có CHECK chỉ nhận AI_SINGLE/AI_ENSEMBLE/HUMAN,
     * mà {@code ddl-auto} không alter được CHECK nên thêm giá trị mới đòi một migration riêng.
     * Giá trị thật nằm ở {@code graded_by_model} bên dưới.
     *
     * <p>Ghi HUMAN sẽ tệ hơn: màn chi tiết rẽ nhánh theo {@code engineType == HUMAN} nên câu này
     * sẽ hiện như "giáo viên đã chấm" trong khi chưa ai chấm cả.
     */
    private static final String GRADED_BY = "SYSTEM_NO_RECORDING";

    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;

    public MissingResponseBackfillService(
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository,
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository) {
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
    }

    /**
     * Câu này có thật sự KHÔNG có nội dung nào không.
     *
     * <p>Xét cả transcript của TỪNG LƯỢT chứ không chỉ ô tổng của response: có đường ghi chỉ
     * điền transcript ở mức lượt, nên chỉ nhìn response sẽ kết luận nhầm là rỗng.
     *
     * <p>Định nghĩa nằm ở đây thay vì viết riêng tại từng nơi gọi, vì trước đây đúng chuyện đó
     * đã xảy ra: đường nộp bài có rào này, còn {@code UpholdResultUseCase} thì không -- và một
     * câu học sinh đã nói 2 lượt (41 giây + 36 giây, đủ audio lẫn transcript) bị ghi 0 điểm kèm
     * câu "Thí sinh không đưa ra câu trả lời nào cho câu hỏi này" (đo được 2026-08-17, phiên
     * 01a0101d). Một vị từ, một nơi định nghĩa.
     */
    public boolean isSilentAnswer(AssessmentItemResponse response) {
        if (response.getTranscript() != null && !response.getTranscript().isBlank()) {
            return false;
        }
        return assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(response.getId()).stream()
            .noneMatch(turn -> turn.getTranscript() != null && !turn.getTranscript().isBlank());
    }

    /**
     * Ghi bản chấm 0 điểm cho một response ĐÃ TỒN TẠI nhưng thí sinh không nói gì.
     *
     * <p>Khác {@link #backfill}: ở đó câu chưa từng được đưa ra nên không có dòng nào; ở đây thí
     * sinh ĐÃ được hỏi, AI đã hỏi lại tới trần rồi cắt câu, nhưng transcript vẫn rỗng.
     *
     * <p>Vì sao không để LLM chấm: nó nhận đề bài đầy đủ nhưng phần trả lời rỗng, và vẫn sẽ trả
     * về một con điểm -- suy ra từ hư không, nhìn không phân biệt được với điểm thật. Không nói
     * gì thì 0 là câu trả lời duy nhất đúng, và ghi thẳng ở đây vừa chắc chắn vừa khỏi tốn một
     * lượt gọi model.
     *
     * <p>Tự kiểm {@link #isSilentAnswer} chứ không tin người gọi: đây là hàm ghi thẳng 0 điểm
     * kèm câu "thí sinh không trả lời", tức nó phát ngôn một kết luận về học sinh. Kết luận đó
     * phải do chính dữ liệu quyết định, không do nơi gọi nhớ hay quên kiểm.
     *
     * @return true nếu đã ghi (tức response này thật sự rỗng); false khi câu có nội dung và
     *         không được phép cho 0
     */
    public boolean recordSilentAnswer(UUID responseId, UUID paperItemId) {
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(responseId).orElse(null);
        if (response != null && !isSilentAnswer(response)) {
            LOGGER.warn(
                "Từ chối ghi 0 điểm cho câu {}: câu này CÓ nội dung trả lời, phải để người chấm xử lý.",
                responseId
            );
            return false;
        }
        assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.builder()
                .responseId(responseId)
                .paperItemId(paperItemId)
                .engineType(AssessmentEvaluationEngineType.AI_SINGLE)
                .gradedByModel(GRADED_BY)
                .sampleCount(null)
                .reviewerId(null)
                .rawItemScore(BigDecimal.ZERO)
                .itemScore(BigDecimal.ZERO)
                .overallConfidence(null)
                .requiresHumanReview(false)
                .reviewReasonCode(NO_RECORDING)
                .markedInvalid(false)
                .requiresRetake(false)
                .signals(null)
                .validityJson(null)
                .feedbackSummary("Thí sinh không đưa ra câu trả lời nào cho câu hỏi này.")
                .suggestionsJson(null)
                .promptVersion(null)
                .status(AssessmentItemEvaluationStatus.FINALIZED)
                .evaluatedAt(Instant.now())
                .build());
        LOGGER.info("Câu {} không có nội dung trả lời -- ghi 0 điểm, không gọi LLM.", responseId);
        return true;
    }

    /**
     * @return số câu đã lấp; 0 khi thí sinh làm đủ đề (đường thường gặp nhất)
     */
    public int backfill(UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return 0;
        }
        Set<UUID> answeredPaperItemIds = assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getPaperItemId())
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        Instant now = Instant.now();
        int created = 0;
        for (AssessmentPaperItem paperItem : assessmentPaperItemRepository.findByPaperId(paperId)) {
            if (answeredPaperItemIds.contains(paperItem.getId())) {
                continue;
            }
            // The id must be generated here. {@code assessment_item_responses.id} is a plain @Id, NOT
            // @GeneratedValue -- unlike {@code assessment_item_evaluations.id} right below it, which has
            // @Generated(INSERT) + DEFAULT uuidv7(). This is deliberate: the id of a response is the answerId generated by
            // the Python side (deterministic UUID v5) and RecordAnswerTurnUseCase writes that value
            // straight in, so this column must not be left for the DB to decide -- if the id diverges, every findById(answerId)
            // misses.
            //
            // Using the 7-parameter constructor (without id) makes Hibernate throw
            // IdentifierGenerationException on persist(). Because SubmitAssessmentSessionUseCase.execute()
            // is @Transactional and calls backfill RIGHT BEFORE the loop that fires AssessmentAttemptEvaluationRequested,
            // the exception here rolls back the whole transaction and swallows the event-firing loop too: the Python side
            // publishes AnswerTurnsRecorded and then waits forever with no grading request coming back. The symptom
            // is "the attempt is not graded", not "backfill is broken", so it is very easy to blame Kafka by mistake.
            AssessmentItemResponse response = assessmentItemResponseRepository.save(AssessmentItemResponse.builder()
                    .id(UUID.randomUUID())
                    .sessionId(sessionId)
                    .paperItemId(paperItem.getId())
                    .audioUrl(null)
                    .durationSeconds(0)
                    .transcript(null)
                    .terminationReason(NO_RECORDING)
                    .submittedAt(now)
                    .build());
            // requiresHumanReview = FALSE. With no recording a score of 0 is correct and nobody needs to
            // review it again.
            //
            // Setting it to true was tried, with the argument "the student may appeal that the machine failed to
            // record", and dropped: the data CANNOT tell "the student gave no answer" from "the machine broke" -- both
            // are just a missing response row. Raising the flag would assert something the system does not
            // know, and flagging every blank question would flood the review queue with results where 0 is
            // already right, making the flag meaningless.
            //
            // The appeal path already exists and is in the right place: when the student files an appeal the
            // APPEAL round opens, and thanks to this backfill the teacher HAS a question to regrade. That is
            // the value of the backfill -- it makes the question EXIST, not make someone go and review it.
            assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.builder()
                    .responseId(response.getId())
                    .paperItemId(paperItem.getId())
                    .engineType(AssessmentEvaluationEngineType.AI_SINGLE)
                    .gradedByModel(GRADED_BY)
                    .sampleCount(null)
                    .reviewerId(null)
                    .rawItemScore(BigDecimal.ZERO)
                    .itemScore(BigDecimal.ZERO)
                    .overallConfidence(null)
                    .requiresHumanReview(false)
                    .reviewReasonCode(NO_RECORDING)
                    .markedInvalid(false)
                    .requiresRetake(false)
                    .signals(null)
                    .validityJson(null)
                    .feedbackSummary("Không thu được bản ghi cho câu này.")
                    .suggestionsJson(null)
                    .promptVersion(null)
                    .status(AssessmentItemEvaluationStatus.FINALIZED)
                    .evaluatedAt(now)
                    .build());
            created++;
        }
        if (created > 0) {
            LOGGER.info("Phiên {}: lấp {} câu không có bản ghi (0 điểm, chờ giáo viên xem lại).",
                sessionId, created);
        }
        return created;
    }
}
