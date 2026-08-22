package com.sep.vox.domain.service.exam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;

/**
 * Chỗ DUY NHẤT phát biểu "một câu hỏi chiếm bao nhiêu giây của mã đề".
 *
 * <p>Hai con số, hai nghĩa khác nhau, đừng dùng lẫn:
 *
 * <ul>
 *   <li>{@code billableSeconds} = prep + maxResponse. Đây là phần thí sinh NÓI, tức phần duy nhất có
 *       thể sinh chi phí AI. Chỉ ước tính tiền dùng số này
 *       ({@code ClassTestTokenQuotaGuardService}), vì rate ở đó có đơn vị USD trên mỗi giây thí sinh
 *       THẬT SỰ nói (xem {@code QuotaPricingCalibrationService} -- mẫu số calibrate là
 *       {@code exam_item_responses.duration_seconds}).
 *   <li>{@code totalSeconds} = billable + thời lượng phát AUDIO/VIDEO. Đây là thời gian thật thí sinh
 *       ngồi trong phòng, nên đồng hồ đếm ngược, độ dài ca thi và trần {@code maxTimePerAttemptMin}
 *       đều phải dùng số này.
 * </ul>
 *
 * <p>Nhét media vào một con số duy nhất thì ước tính tiền bị phồng lên vì giây phát media là giây
 * KHÔNG ai nói và AI không chạy -- nhân nó với rate kia là bịa ra chi phí không thể tồn tại.
 *
 * <p>Hàm thuần, không tự query: bên gọi nạp sẵn {@link Question} (và {@link QuestionAsset} nếu cần
 * {@code totalSeconds}) rồi truyền vào, để mỗi nơi giữ nguyên cách gom dữ liệu của mình -- có nơi lấy
 * từ DB sau khi lưu, có nơi chiếu từ command TRƯỚC khi lưu nên chưa có gì trong DB để đọc.
 *
 * <h2>⚠️ Giới hạn đã biết: sửa câu hỏi/tài nguyên KHÔNG tự tính lại mã đề</h2>
 *
 * <p>{@code RecalculateExamTimeDurationService} chỉ được gọi từ các thao tác trên KỲ THI / MÃ ĐỀ
 * (tạo/xoá mã đề, sửa item, sửa phần của bài trên lớp, đổi blueprint). Không đường nào đi từ
 * {@code usecase/question/}. Nên nếu mã đề ĐÃ tạo rồi mới:
 *
 * <ul>
 *   <li>upload tệp audio/video thật (thời lượng đổi từ số khai trong Excel sang số đo thật), hoặc
 *   <li>sửa {@code preparationTimeSeconds} / {@code maxResponseSeconds} của câu hỏi,
 * </ul>
 *
 * <p>thì {@code paper.timeDurationSeconds} và {@code exam.examTimeDurationSecond} GIỮ SỐ CŨ cho tới
 * lần kế tiếp có ai đó đụng vào cấu trúc mã đề.
 *
 * <p>Đây là hành vi CÓ SẴN từ trước (hai trường thời gian của câu hỏi vốn đã như vậy), việc cộng
 * thêm thời lượng media chỉ làm nó dễ chạm hơn -- vì upload tệp là việc rất hay làm ngay sau khi
 * nhập câu hỏi hàng loạt.
 *
 * <p><b>Cách tránh:</b> hoàn thiện câu hỏi (kể cả upload media) TRƯỚC, rồi mới tạo mã đề. Lúc
 * {@code CreateExamPaperUseCase} chạy nó gọi recalculate và đọc đúng số. Quyết định 2026-08-21:
 * ghi tài liệu chứ chưa tự động hoá, vì sửa đúng cần truy vấn ngược câu hỏi → item → mã đề → kỳ thi
 * ở mọi đường sửa câu hỏi và tài nguyên.
 */
public final class PaperTimeCalculator {

    private PaperTimeCalculator() {
    }

    public record PaperTimeBreakdown(int totalSeconds, int billableSeconds) {
    }

    /**
     * Chỉ phần sinh chi phí AI, KHÔNG gồm thời lượng media.
     *
     * <p>Tách hẳn thành entry riêng thay vì cho gọi {@link #breakdownOf} với map rỗng: map rỗng sẽ
     * dựng ra một breakdown mà {@code totalSeconds} nói dối (bằng billable, thiếu media), và người
     * gọi nhầm sau này không có cách nào biết.
     */
    public static int billableSecondsOf(Collection<Question> questions) {
        var seconds = 0;
        for (var question : questions) {
            seconds += billableSecondsOfOne(question);
        }
        return seconds;
    }

    /** Công thức gốc, viết đúng MỘT lần trong toàn repo. */
    private static int billableSecondsOfOne(Question question) {
        return question.getPreparationTimeSeconds() + question.getMaxResponseSeconds();
    }

    /**
     * Gom kết quả {@code QuestionAssetRepository#findByQuestionIdIn} thành map để truyền vào
     * {@link #breakdownOf}. Mỗi câu hỏi có tối đa một asset
     * ({@code uk_question_assets_question_id} unique trên riêng {@code question_id}) nên map một-một
     * là đủ; hàm gộp chỉ để phòng dữ liệu cũ lỡ có hai dòng, lấy dòng đầu thay vì ném lỗi.
     */
    public static Map<UUID, QuestionAsset> indexByQuestionId(Collection<QuestionAsset> assets) {
        var byQuestionId = new HashMap<UUID, QuestionAsset>();
        for (var asset : assets) {
            byQuestionId.putIfAbsent(asset.getQuestionId(), asset);
        }
        return byQuestionId;
    }

    /**
     * Cả hai số. {@code assetByQuestionId} dựng bằng {@link #indexByQuestionId}.
     */
    public static PaperTimeBreakdown breakdownOf(
            Collection<Question> questions,
            Map<UUID, QuestionAsset> assetByQuestionId) {
        var billableSeconds = 0;
        var mediaSeconds = 0;
        for (var question : questions) {
            billableSeconds += billableSecondsOfOne(question);
            mediaSeconds += mediaSecondsOf(assetByQuestionId.get(question.getId()));
        }
        return new PaperTimeBreakdown(billableSeconds + mediaSeconds, billableSeconds);
    }

    /**
     * Lọc theo {@code type}, KHÔNG theo "durationSeconds khác null": {@code duration_seconds} là cột
     * dùng chung cho mọi loại asset, nên một con số lạc trên tấm ảnh sẽ kéo dài cả ca thi. Ảnh và
     * đoạn văn hiện suốt lúc chuẩn bị nên đã nằm trong {@code preparationTimeSeconds} rồi.
     */
    public static int mediaSecondsOf(QuestionAsset asset) {
        if (asset == null || asset.getDurationSeconds() == null) {
            return 0;
        }
        if (asset.getType() != QuestionAssetType.AUDIO && asset.getType() != QuestionAssetType.VIDEO) {
            return 0;
        }
        return Math.max(0, asset.getDurationSeconds());
    }
}
