-- Mô tả tài nguyên câu hỏi: varchar(2048) -> TEXT, cho khớp với transcript vốn đã là TEXT.
--
-- Vì sao cần: description là THỨ DUY NHẤT AI biết về tài nguyên (nó không nhìn được ảnh, không nghe
-- được tệp), nên mô tả một đoạn nghe dài hay một bức ảnh nhiều chi tiết dễ vượt 2048 ký tự. Mà phía
-- request KHÔNG có @Size chặn trước, nên vượt hạn mức là nổ thẳng ở tầng DB với thông báo khó hiểu,
-- không phải một lỗi kiểm tra dữ liệu tử tế.
--
-- Nới rộng cột nên an toàn với dữ liệu sẵn có: mọi giá trị đang lưu đều ngắn hơn 2048 ký tự.
-- Postgres đổi varchar(n) -> text không phải viết lại bảng.
ALTER TABLE question_assets
    ALTER COLUMN description TYPE TEXT;
