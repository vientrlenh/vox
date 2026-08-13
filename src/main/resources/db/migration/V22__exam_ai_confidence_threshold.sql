-- Ngưỡng tin cậy AI do NHÀ TRƯỜNG đặt cho từng bài thi / bài kiểm tra trên lớp.
--
-- Bản chấm nào có overall_confidence THẤP HƠN ngưỡng này thì không được tự động công bố mà
-- chuyển sang PENDING_REVIEW để giáo viên soi lại. Trước đó việc đưa bài sang duyệt hoàn toàn do
-- luật ngưỡng cứng trong ConfidenceReviewCalculator quyết định, nhà trường không có cách nào nói
-- "môn này/kỳ này tôi muốn chặt hơn".
--
-- NULL = không đặt ngưỡng, giữ nguyên hành vi cũ (chỉ luật cứng quyết định). Cố ý cho phép NULL
-- thay vì gán mặc định: mọi bài thi đã tạo trước migration này chưa từng được nhà trường cân
-- nhắc ngưỡng, nên tự áp một con số cho chúng là bịa ra ý định không có thật -- và với bài đang
-- mở thi thì nó đổi luật giữa chừng.
--
-- Đơn vị PHẦN TRĂM (0-100), và tên cột nói rõ điều đó.
--
-- exam_item_evaluation.overall_confidence là tỉ lệ 0.00-1.00. Hai cột cùng nói về "độ tin cậy"
-- mà khác đơn vị là chỗ dễ sinh lỗi im lặng nhất -- so thẳng 0.82 với 75 thì luôn ra "dưới
-- ngưỡng" và mọi bài đổ sang duyệt. Hậu tố _percent để bất kỳ ai đọc câu SQL sau này đều thấy
-- ngay là phải quy đổi; phép quy đổi nằm ở RecordExamAttemptEvaluationUseCase.
--
-- Chọn lưu phần trăm thay vì quy về 0-1 lúc nhận: giá trị trong DB đúng bằng con số nhà trường
-- đã gõ, nên đối chiếu với họ không cần phiên dịch.
ALTER TABLE exams
    ADD COLUMN ai_confidence_threshold_percent NUMERIC(5, 2);

ALTER TABLE exams
    ADD CONSTRAINT chk_exams_ai_confidence_threshold_percent
    CHECK (ai_confidence_threshold_percent IS NULL
           OR (ai_confidence_threshold_percent >= 0 AND ai_confidence_threshold_percent <= 100));
