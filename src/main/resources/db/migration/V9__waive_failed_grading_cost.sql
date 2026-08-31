-- =============================================================================
-- V9: chi phí AI của một lượt chấm HỎNG không bao giờ được thu của trường, và giới hạn số lần
--     nhà trường được nhờ AI chấm lại.
--
-- NGUYÊN TẮC (viết ra đây vì nó KHÔNG phải "lượt hỏng thì miễn phí"):
--
--   Trường bị thu tiền cho phần việc AI đã tạo ra KẾT QUẢ DÙNG ĐƯỢC cho họ.
--
-- Phân biệt này quan trọng vì hai đường tiêu tiền trong hệ thống có triết lý trái ngược nhau, và
-- một quy tắc viết theo kiểu "hỏng thì không thu" sẽ bị áp nhầm sang đường kia:
--
--   * Đường THI thu ở bước GRADING -> GRADED, tức thu theo KẾT QUẢ. Lượt chấm hỏng không có dòng
--     exam_candidate_results nào (đo được: phiên 01a015a8 ngày 18/08/2026 -- 2 câu trả lời, 0 bản
--     chấm, result_id null), nên trường không nhận được gì và không có gì để thu.
--   * Đường LUYỆN NÓI thu NGAY trong request submit_turn, theo CHI PHÍ ĐÃ PHÁT SINH, vì lúc đó
--     Azure đã tính tiền xong -- xem SubmitPracticeTurnUseCase. Một lượt nói đã trả lời học sinh
--     xong thì vẫn là kết quả dùng được, nên vẫn thu. ĐỪNG áp cột waived_at bên dưới sang đó.
--
-- VÌ SAO PHẢI SỬA: hôm nay lượt hỏng gần như đã miễn phí sẵn -- các dòng ai_usage_records của nó ở
-- lại với charged_at NULL và không ai thu. Nhưng lần chấm lại THÀNH CÔNG gọi
-- markChargedByExamSessionId(), câu này giành MỌI dòng chưa thu của phiên, tức nuốt luôn phần token
-- đã đốt ở lượt hỏng, rồi trừ tất cả với allowDebt = true. Kết quả là trường chỉ bị tính tiền cho
-- sự cố của nền tảng KHI HỌ CHỊU ĐI KHẮC PHỤC nó -- đúng hành vi ta muốn khuyến khích lại là hành
-- vi duy nhất bị phạt.
--
-- Sổ sách cũng đang mâu thuẫn: biên lợi nhuận gộp (PlatformBusinessHealthQueryRepository.sumAiCostVnd)
-- cộng cost_vnd mà KHÔNG lọc charged_at, tức đã ghi khoản này vào chi phí của NỀN TẢNG. Cùng một
-- token đang có hai chủ. V9 chốt chủ là nền tảng, khớp với cách biên lợi nhuận vẫn tính.
-- =============================================================================

-- charged_at IS NULL trước V9 mang HAI nghĩa lẫn nhau: "chưa thu" và "sẽ không bao giờ thu". Cần một
-- cột riêng để nói vế thứ hai, đối xứng với charged_at chứ không phải một cờ boolean: giữ MỐC giúp
-- trả lời được "sự cố hôm 23/08 đã tốn nền tảng bao nhiêu", con số hiện không chỗ nào có.
ALTER TABLE ai_usage_records ADD COLUMN waived_at timestamp(6) with time zone;

-- Một dòng chi phí chỉ có đúng một kết cục. Thu rồi thì không miễn được nữa, và ngược lại -- nếu
-- không, một lượt chấm lại có thể miễn đúng những dòng vừa thu tiền xong ở lượt trước.
ALTER TABLE ai_usage_records
    ADD CONSTRAINT chk_ai_usage_records_charged_xor_waived CHECK (
        charged_at IS NULL OR waived_at IS NULL);

-- Đường thu tiền quét theo (exam_session_id, charged_at IS NULL); giờ nó phải bỏ qua cả dòng đã
-- miễn, nên index một phần theo đúng vị từ mới.
CREATE INDEX idx_ai_usage_records_claimable
    ON ai_usage_records USING btree (exam_session_id)
    WHERE charged_at IS NULL AND waived_at IS NULL;

-- =============================================================================
-- Số lần NHÀ TRƯỜNG đã nhờ AI chấm lại phiên này.
--
-- Đếm riêng chứ không dùng lại grading_retry_count (V6): cột đó là số lần DỊCH VỤ CHẤM tự thử,
-- lấy từ payload.retryCount của sự kiện Kafka. Nó nói về một lượt chấm duy nhất, không nói gì về số
-- lượt mà người dùng đã yêu cầu.
--
-- CHỈ đếm lượt do phía trường bấm. Lượt do quản trị hệ thống kích (và sau này là job tự chạy lại khi
-- có sự cố diện rộng) KHÔNG được tiêu vào định mức của trường: bằng không, một lần AI hỏng hàng loạt
-- sẽ âm thầm đốt sạch lượt chấm lại của mọi trường, rồi ép hàng trăm bài sang chấm tay vì một sự cố
-- của chính nền tảng.
-- =============================================================================
ALTER TABLE exam_sessions ADD COLUMN school_regrade_count integer DEFAULT 0 NOT NULL;

ALTER TABLE exam_sessions
    ADD CONSTRAINT chk_exam_sessions_school_regrade_count_not_negative CHECK (
        school_regrade_count >= 0);
