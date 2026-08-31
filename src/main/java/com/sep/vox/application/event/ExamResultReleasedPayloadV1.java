package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Điểm được công bố lần đầu cho học sinh (vòng {@code INITIAL} kết thúc).
 *
 * <p>Tách khỏi {@link ExamResultRegradedPayloadV1}: lần đầu là tin "đã có điểm", còn đổi
 * điểm sau công bố là tin "điểm của em vừa thay đổi" — hai mail khác hẳn nhau.
 *
 * @param sessionId màn hình kết quả của học sinh nhận sessionId chứ không phải
 *        candidateResultId, nên thiếu nó thì thông báo chỉ mở được danh sách bài thi
 * @param examKind bài tập trung và bài kiểm tra lớp có hai màn hình kết quả riêng
 */
public record ExamResultReleasedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    BigDecimal totalScore,
    UUID sessionId,
    ExamKind examKind
) {

}
