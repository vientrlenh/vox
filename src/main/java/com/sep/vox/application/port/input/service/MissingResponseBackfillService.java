package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;


@Service
@RequiredArgsConstructor
@Slf4j
public class MissingResponseBackfillService {

    /**
     * Label written to {@code termination_reason} -- it tells this case apart from a question the candidate did attempt but that was cut
     * off midway ({@code redirect_offtopic}, {@code timeout}...). This question never started.
     */
    public static final String NO_RECORDING = "no_recording";

    /**
     * {@code engine_type} borrows AI_SINGLE because the column has a CHECK that only accepts AI_SINGLE/AI_ENSEMBLE/HUMAN,
     * and {@code ddl-auto} cannot alter a CHECK, so a new value would need its own migration.
     * The real value is in {@code graded_by_model} below.
     *
     * <p>Writing HUMAN would be worse: the detail screen branches on {@code engineType == HUMAN}, so this question
     * would show as "graded by a teacher" although nobody graded it.
     */
    private static final String GRADED_BY = "SYSTEM_NO_RECORDING";

    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;

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
            log.warn(
                "Từ chối ghi 0 điểm cho câu {}: câu này CÓ nội dung trả lời, phải để người chấm xử lý.",
                responseId
            );
            return false;
        }
        assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.zeroScoredWithoutRecording(
                responseId,
                paperItemId,
                GRADED_BY,
                NO_RECORDING,
                "Thí sinh không đưa ra câu trả lời nào cho câu hỏi này.",
                Instant.now()));
        log.info("Câu {} không có nội dung trả lời -- ghi 0 điểm, không gọi LLM.", responseId);
        return true;
    }

    /**
     * @return the number of questions backfilled; 0 when the candidate answered the whole paper (the most common path)
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
            // The id is left null on purpose: assessment_item_responses.id keeps the answerId the AI service sends
            // for recorded answers, and generates a version 7 id for a row that has none, like this one. The evaluation
            // below must use the id of the SAVED response, never one chosen before the save.
            AssessmentItemResponse response = assessmentItemResponseRepository.save(AssessmentItemResponse.withoutRecording(
                    sessionId, paperItem.getId(), NO_RECORDING, now));
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
            assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.zeroScoredWithoutRecording(
                    response.getId(),
                    paperItem.getId(),
                    GRADED_BY,
                    NO_RECORDING,
                    "Không thu được bản ghi cho câu này.",
                    now));
            created++;
        }
        if (created > 0) {
            log.info("Phiên {}: lấp {} câu không có bản ghi (0 điểm, chờ giáo viên xem lại).",
                sessionId, created);
        }
        return created;
    }
}
