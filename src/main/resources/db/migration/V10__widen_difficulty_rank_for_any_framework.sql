-- practice_question.difficulty_rank đang bị khoá vào thang 6 bậc kiểu VSTEP
-- (CHECK BETWEEN 1 AND 6). Bảng này dùng chung cho MỌI trường, mà mỗi trường có thể áp
-- framework khác nhau (CEFR 6 bậc, IELTS 9 bậc...) -- nên không thể ràng theo một N cụ thể.
--
-- Nới thành 1..20 thay vì bỏ hẳn: vẫn chặn rác (0, âm, 999 do lỗi ánh xạ) mà không khoá vào
-- một thang cụ thể. Trần đúng theo từng học sinh do tầng ứng dụng lo -- xem
-- PracticeTopicOfferEnrichmentService.frameworkBandCount, đọc từ framework_result_bands.
ALTER TABLE practice_question
    DROP CONSTRAINT IF EXISTS chk_practice_question_difficulty_rank;

ALTER TABLE practice_question
    ADD CONSTRAINT chk_practice_question_difficulty_rank
    CHECK (difficulty_rank BETWEEN 1 AND 20);
