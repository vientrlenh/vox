-- Đưa danh mục "chiều sở thích" từ hằng số cứng trong code ra thành dữ liệu, để
-- SYSTEM_ADMIN thêm/sửa được mà không phải deploy lại cả Java lẫn Python.
--
-- Trước migration này danh sách bị lặp cứng ở 5 chỗ (InterestQuizScorer.DIMENSIONS,
-- 3 Literal bên Python, prompt sinh quiz) -- thiếu đồng bộ một chỗ là dimension mới
-- âm thầm vô hiệu chứ không báo lỗi: đúng thứ đã xảy ra với ACADEMIC_EXAM, vốn được
-- ViewPracticeTopicOffersUseCase gán cho topic lấy từ ngân hàng đề nhưng chưa bao giờ
-- có mặt trong danh sách chuẩn hoá nên không bao giờ có dòng dimension_interest_score.
CREATE TABLE interest_dimension (
    code            VARCHAR(32)  PRIMARY KEY,
    label           VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    -- Tắt mềm: giữ nguyên dữ liệu lịch sử (điểm số, topic đã gán) thay vì xoá cứng.
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Tách riêng với `active` vì có chiều KHÔNG phải sở thích: ACADEMIC_EXAM do hệ thống
    -- tự gán cho topic từ ngân hàng đề, không được đem ra hỏi học sinh "cái nào giống em
    -- nhất". Gộp chung một cờ là sẽ lẫn hai khái niệm khác hẳn nhau.
    quiz_eligible   BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interest_dimension_active_quiz
    ON interest_dimension (active, quiz_eligible);

INSERT INTO interest_dimension (code, label, description, quiz_eligible, display_order) VALUES
    ('ENTERTAINMENT_MEDIA', 'Giải trí & Truyền thông',
     'Phim ảnh, âm nhạc, nội dung số, sáng tạo hình ảnh/âm thanh.', TRUE, 1),
    ('TECH_GAMING', 'Công nghệ & Trò chơi',
     'Thiết bị, phần mềm, trò chơi điện tử, thiết kế giao diện/luật chơi.', TRUE, 2),
    ('SPORTS_HEALTH', 'Thể thao & Sức khoẻ',
     'Vận động, rèn luyện thể chất, dinh dưỡng, thói quen lành mạnh.', TRUE, 3),
    ('PEOPLE_SOCIETY', 'Con người & Xã hội',
     'Giao tiếp, làm việc nhóm, quan sát hành vi, hoạt động cộng đồng.', TRUE, 4),
    ('TRAVEL_PLACES', 'Du lịch & Địa điểm',
     'Khám phá nơi chốn, bản đồ, lộ trình, văn hoá vùng miền.', TRUE, 5),
    ('FUTURE_SCIENCE', 'Khoa học & Tương lai',
     'Hiện tượng tự nhiên, phát minh, môi trường, xu hướng tương lai.', TRUE, 6),
    -- quiz_eligible = FALSE: xem giải thích ở định nghĩa cột.
    ('ACADEMIC_EXAM', 'Ôn thi theo chương trình',
     'Chủ đề lấy từ ngân hàng câu hỏi của trường; hệ thống tự gán, không hỏi qua quiz.',
     FALSE, 99);
