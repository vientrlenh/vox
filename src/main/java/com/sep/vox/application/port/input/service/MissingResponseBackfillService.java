package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;


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

    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;

    public MissingResponseBackfillService(
            ExamPaperItemRepository examPaperItemRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository) {
        this.examPaperItemRepository = examPaperItemRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
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
    public boolean isSilentAnswer(ExamItemResponse response) {
        if (response.getTranscript() != null && !response.getTranscript().isBlank()) {
            return false;
        }
        return examItemResponseTurnRepository.findByExamItemResponseId(response.getId()).stream()
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
        var response = examItemResponseRepository.findById(responseId).orElse(null);
        if (response != null && !isSilentAnswer(response)) {
            LOGGER.warn(
                "Từ chối ghi 0 điểm cho câu {}: câu này CÓ nội dung trả lời, phải để người chấm xử lý.",
                responseId
            );
            return false;
        }
        examItemEvaluationRepository.save(new ExamItemEvaluation(
            responseId,
            paperItemId,
            ExamEvaluationEngineType.AI_SINGLE,
            GRADED_BY,
            null,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            false,
            NO_RECORDING,
            false,
            false,
            null,
            null,
            "Thí sinh không đưa ra câu trả lời nào cho câu hỏi này.",
            null,
            null,
            ExamItemEvaluationStatus.FINALIZED,
            Instant.now()
        ));
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
        var answeredPaperItemIds = examItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getPaperItemId())
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        var now = Instant.now();
        var created = 0;
        for (var paperItem : examPaperItemRepository.findByPaperId(paperId)) {
            if (answeredPaperItemIds.contains(paperItem.getId())) {
                continue;
            }
            // Id phải tự sinh ở đây. {@code exam_item_responses.id} là @Id trần, KHÔNG
            // @GeneratedValue -- khác với {@code exam_item_evaluations.id} ngay bên dưới vốn có
            // @Generated(INSERT) + DEFAULT uuidv7(). Cố ý: id của response chính là answerId do
            // phía Python sinh (UUID v5 deterministic) và RecordAnswerTurnUseCase ghi thẳng giá
            // trị đó vào, nên cột này không được để DB quyết -- id lệch thì mọi findById(answerId)
            // đều trượt.
            //
            // Dùng constructor 7 tham số (bỏ id) khiến Hibernate ném
            // IdentifierGenerationException lúc persist(). Vì SubmitExamSessionUseCase.execute()
            // là @Transactional và gọi backfill NGAY TRƯỚC vòng bắn ExamAttemptEvaluationRequested,
            // exception ở đây rollback cả transaction và nuốt luôn vòng bắn sự kiện: phía Python
            // publish AnswerTurnsRecorded xong chờ mãi không có request chấm quay lại. Triệu chứng
            // là "bài không được chấm", không phải "backfill hỏng", nên rất dễ đổ nhầm cho Kafka.
            var response = examItemResponseRepository.save(new ExamItemResponse(
                UUID.randomUUID(),
                sessionId, paperItem.getId(), null, 0, null, NO_RECORDING, now
            ));
            examItemEvaluationRepository.save(new ExamItemEvaluation(
                response.getId(),
                paperItem.getId(),
                ExamEvaluationEngineType.AI_SINGLE,
                GRADED_BY,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                // requiresHumanReview = FALSE. Không có bản ghi thì 0 điểm là đúng, không cần
                // ai xem lại.
                //
                // Đã thử đặt true với lý lẽ "học sinh có thể khiếu nại máy không ghi được" rồi
                // bỏ: dữ liệu KHÔNG phân biệt được "em không trả lời" với "máy hỏng" -- cả hai
                // đều chỉ là thiếu dòng response. Bật cờ là khẳng định một điều hệ thống không
                // biết, và gắn cờ cho mọi câu trắng thì hàng chờ duyệt ngập những bài mà 0 điểm
                // vốn đã đúng, khiến cờ mất luôn ý nghĩa.
                //
                // Đường khiếu nại đã có sẵn và đúng chỗ: học sinh nộp đơn phúc khảo thì vòng
                // APPEAL mở ra, và nhờ backfill này giáo viên MỚI có câu để chấm lại. Đó mới là
                // giá trị của việc lấp -- làm cho câu TỒN TẠI, không phải bắt ai đó đi duyệt.
                false,
                NO_RECORDING,
                false,
                false,
                null,
                null,
                "Không thu được bản ghi cho câu này.",
                null,
                null,
                ExamItemEvaluationStatus.FINALIZED,
                now
            ));
            created++;
        }
        if (created > 0) {
            LOGGER.info("Phiên {}: lấp {} câu không có bản ghi (0 điểm, chờ giáo viên xem lại).",
                sessionId, created);
        }
        return created;
    }
}
