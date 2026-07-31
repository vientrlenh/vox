package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Điểm được công bố lần đầu cho học sinh (vòng {@code INITIAL} kết thúc).
 *
 * <p>Tách khỏi {@link ExamResultRegradedPayloadV1}: lần đầu là tin "đã có điểm", còn đổi
 * điểm sau công bố là tin "điểm của em vừa thay đổi" — hai mail khác hẳn nhau.
 */
public record ExamResultReleasedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String examName,
    BigDecimal totalScore
) {

}
