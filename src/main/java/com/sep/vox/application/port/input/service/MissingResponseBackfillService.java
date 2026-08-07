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

/**
 * Lấp chỗ trống cho câu KHÔNG có bản ghi nào -- chạy lúc nộp bài.
 *
 * <p><b>Vấn đề.</b> Cả dây chuyền chấm neo vào {@code exam_item_responses}: màn chấm liệt kê câu
 * từ bảng đó, {@code GradingItemScoreResolver} tra {@code paperItemId -> response} rồi ném nếu
 * không thấy, và {@code exam_item_evaluations.response_id} là NOT NULL. Câu mà thí sinh chưa
 * kịp làm (hết giờ, mất kết nối, buộc kết thúc) không có dòng nào, nên nó VÔ HÌNH với người
 * chấm -- kể cả ở vòng phúc khảo, kể cả khi học sinh khiếu nại đúng.
 *
 * <p>Nhưng trọng số của nó thì KHÔNG vô hình: bộ tính điểm cộng theo trọng số từng câu, câu
 * thiếu không đóng góp gì trong khi trọng số của section vẫn cố định. Đo trên một phiên thật:
 * đề 3 câu, làm được 1 câu đạt 8.63/10, tổng ra 2.16. Trọng số đã bị tính vào mẫu số nhưng
 * giáo viên không có đường nào cho điểm hai câu kia.
 *
 * <p><b>Cách chữa.</b> Tạo CẶP response rỗng + bản chấm 0 điểm. Cả bốn tầng trên tự chạy đúng
 * mà không phải sửa cái nào -- đó là lý do chọn hướng này thay vì vá từng query.
 *
 * <p><b>Phải là cặp, không được chỉ tạo response.</b>
 * {@code RecordExamAttemptEvaluationUseCase.allResponsesHaveEvaluations} đòi MỌI response có
 * bản chấm mới cho bài chốt. Tạo response trần là điều kiện đó vĩnh viễn sai, bài không bao giờ
 * đạt GRADED, không sinh kết quả, không mở được phân công chấm. Hỏng nặng hơn cả trước khi sửa.
 *
 * <p><b>Không bắn sự kiện AI.</b> Bản chấm ghi thẳng ở đây. Đẩy một transcript rỗng sang LLM là
 * mời nó bịa ra một con điểm nhìn không phân biệt được với điểm thật.
 */
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
