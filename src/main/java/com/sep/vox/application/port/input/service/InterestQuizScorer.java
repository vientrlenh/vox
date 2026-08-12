package com.sep.vox.application.port.input.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Chuẩn hoá điểm thô của quiz sở thích thành vector [0,1] theo từng chiều.
 *
 * Danh sách chiều đọc từ {@link InterestDimensionRepository} chứ không còn là hằng số cứng:
 * SYSTEM_ADMIN thêm chiều mới là hệ thống nhận ngay, không phải deploy lại (xem
 * V15__personalize.sql, mục 19. interest_dimension, để biết vì sao phải bỏ hằng số cứng).
 *
 * Chấm theo kiểu ipsative (so sánh nội bộ trong chính học sinh đó, đúng bản chất forced-choice):
 * min-max hoá nên LUÔN có một chiều = 0.0 và một chiều = 1.0. Nó trả lời "trong các chiều này em
 * nghiêng về đâu", không trả lời "em thích mạnh tới mức nào". Khi mọi chiều bằng nhau thì trả 0.5
 * hết thay vì chia cho 0.
 */
@Service
public class InterestQuizScorer {

    private final InterestDimensionRepository dimensionRepository;

    public InterestQuizScorer(InterestDimensionRepository dimensionRepository) {
        this.dimensionRepository = dimensionRepository;
    }

    /** Các chiều được đem ra hỏi trong quiz -- không gồm chiều hệ thống như ACADEMIC_EXAM. */
    public List<String> quizDimensionCodes() {
        return dimensionRepository.findQuizEligible().stream()
            .map(dimension -> dimension.getCode())
            .toList();
    }

    public Map<String, Double> normalize(Map<String, Integer> rawScores) {
        var dimensions = quizDimensionCodes();
        if (dimensions.isEmpty()) {
            return Map.of();
        }
        var min = dimensions.stream()
            .mapToInt(dimension -> rawScores.getOrDefault(dimension, 0))
            .min()
            .orElse(0);
        var max = dimensions.stream()
            .mapToInt(dimension -> rawScores.getOrDefault(dimension, 0))
            .max()
            .orElse(0);
        var result = new LinkedHashMap<String, Double>();
        for (var dimension : dimensions) {
            var raw = rawScores.getOrDefault(dimension, 0);
            result.put(dimension, min == max ? 0.5 : (double) (raw - min) / (max - min));
        }
        return result;
    }
}
