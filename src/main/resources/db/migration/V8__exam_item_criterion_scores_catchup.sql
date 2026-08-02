-- exam_item_criterion_scores thieu cot matched_band_code (da co trong entity
-- ExamItemCriterionScoreJpaEntity, dung boi luong AI cham bai va truy van uoc luong
-- CEFR/VSTEP band trong JpaLearnerProfileQueryRepository.findEstimatedBandCode).
ALTER TABLE exam_item_criterion_scores
    ADD COLUMN matched_band_code VARCHAR(64);
