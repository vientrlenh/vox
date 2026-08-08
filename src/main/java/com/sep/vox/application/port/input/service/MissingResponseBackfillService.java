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
    private final ExamItemEvaluationRepository examItemEvaluationRepository;

    public MissingResponseBackfillService(
            ExamPaperItemRepository examPaperItemRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository) {
        this.examPaperItemRepository = examPaperItemRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
    }

    /**
     * @return số câu đã lấp; 0 khi thí sinh làm đủ đề (đường thường gặp nhất)
     */
    public int backfill(UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return 0;
        }
        var answeredPaperItemIds = examItemResponseRepository.findBySessionId(sessionId).stream()
            .map(ExamItemResponse::getPaperItemId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        var now = Instant.now();
        var created = 0;
        for (var paperItem : examPaperItemRepository.findByPaperId(paperId)) {
            if (answeredPaperItemIds.contains(paperItem.getId())) {
                continue;
            }
            var response = examItemResponseRepository.save(new ExamItemResponse(
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
