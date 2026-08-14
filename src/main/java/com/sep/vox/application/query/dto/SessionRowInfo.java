package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public interface SessionRowInfo {

    UUID getId();

    UUID getPracticePaperId();

    UUID getChosenPracticeTopicId();

    String getTopicName();

    String getOrigin();

    String getStatus();

    String getAbandonDiagnosis();

    Double getOverallScore();

    int getGradedSeconds();

    String getOfferedTopicIdsJson();

    // Instant chu khong OffsetDateTime: driver tra TIMESTAMPTZ ve duoi dang Instant, va
    // projection cua Spring Data KHONG co converter Instant -> OffsetDateTime nen se nem
    // UnsupportedOperationException ngay khi co dong du lieu dau tien.
    Instant getStartedAt();

    Instant getEndedAt();

    /**
     * Số câu học sinh ĐÃ NÓI mà chưa có bản chấm.
     *
     * <p>Chấm là bất đồng bộ nên {@code overall_score} lúc đóng phiên chỉ gộp những câu đã
     * chấm xong. Con số này cho client biết điểm đang cầm là TẠM hay đã chốt -- thiếu nó thì
     * danh sách hiện một con số sẽ đổi sau vài chục giây mà không báo hiệu gì.
     */
    int getPendingEvaluations();

    /**
     * Thang chấm của chính phiên này. Phiên từ V13 luôn 0-100; phiên CŨ lấy thang của rubric
     * đã dùng lúc đó. Suy theo phiên chứ không theo cấu hình toàn cục -- hai loại cùng tồn tại
     * lâu dài và màn tổng kết phải hiện đúng cho cả hai.
     */
    double getScoreScaleMin();

    double getScoreScaleMax();
}
