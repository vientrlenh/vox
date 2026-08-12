-- exam_item_criterion_scores.rationale: varchar(512) -> text
--
-- Cột này nhận NHẬN XÉT DO AI SINH cho từng tiêu chí, mà độ dài nhận xét tỉ lệ với số lượt nói
-- của câu -- thứ không có trần nào. Đo trên phiên thi 2026-08-06:
--
--   câu 1 (6 lượt nói)  -> nhận xét vượt 512  -> INSERT ném
--                          "value too long for type character varying(512)"
--   câu 2 (1 lượt nói)  -> vừa                -> ghi bình thường
--
-- Hậu quả không chỉ là cụt chữ mà là MẤT CẢ BÀI CHẤM: consumer không ack -> retry 4 lần ->
-- vào DLT -> dòng evaluation còn lại là bản cũ 0.00 điểm, 0 tiêu chí. Nhìn từ giao diện thì
-- giống hệt "AI không chấm câu này", dù AI đã chấm xong và publish thành công.
--
-- Vì sao nới chứ không cắt bớt: 512 không dựa trên cơ sở nào, và phần bị cắt chính là lời giải
-- thích dành cho học sinh -- đúng thứ cột này sinh ra để chở. rationale cũng không được tra
-- cứu, lọc hay đánh chỉ mục ở đâu, nên text không tốn thêm gì (Postgres lưu varchar và text
-- giống hệt nhau, khác biệt duy nhất là phép kiểm độ dài).

ALTER TABLE exam_item_criterion_scores
    ALTER COLUMN rationale TYPE text;
