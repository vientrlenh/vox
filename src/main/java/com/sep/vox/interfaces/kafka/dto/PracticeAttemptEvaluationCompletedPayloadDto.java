package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

/**
 * Python chấm bài luyện bằng cùng đồ thị chấm bài thi, nên payload phát ra là cùng một
 * hình dạng với {@link ExamAttemptEvaluationCompletedPayloadDto}. Ở đây chỉ khai những
 * trường thật sự có nơi dùng -- Jackson bỏ qua phần còn lại.
 */
public record PracticeAttemptEvaluationCompletedPayloadDto(
    ValidityResultDto validity,
    /** Cần cho việc suy điểm yếu phát âm: word_feedback đi tới từng âm vị. */
    List<TurnDetailDto> turns,
    Map<String, CriterionScoreDto> criteria,
    EvaluationSignalsDto signals,
    String evaluatedAt
) {
}
