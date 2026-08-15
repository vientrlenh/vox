-- Sổ cảnh báo giám sát thi -- mỗi dòng là MỘT đợt vi phạm được vox-streaming phát ra trên topic
-- `exam.alert.raised` (AI phát hiện qua YOLO, hoặc chính streaming phát hiện mất luồng).
--
-- Trước bảng này, cảnh báo chỉ sống trong bộ nhớ tab trình duyệt của giám thị, đúng bằng tuổi thọ
-- một kết nối WebSocket: vào ca muộn là không thấy gì, F5 là mất sạch, hai giám thị cùng ca nhìn hai
-- lịch sử khác nhau, và sau kỳ thi thì không còn bản ghi nào để giải thích vì sao một phiên bị đánh
-- dấu nghi vấn. Nhánh durable đã được phát sẵn từ lâu; đây là đầu đọc còn thiếu.
--
-- Hai người tiêu dùng: màn giám sát trực tiếp (phát lại lịch sử khi giám thị kết nối) và màn chấm
-- bài sau thi (bằng chứng cho quyết định uphold/regrade/invalidate).
create table exam_proctoring_alerts (
    id UUID DEFAULT uuidv7() not null,
    -- Khoá chống ghi trùng. Kafka gửi lại là chuyện bình thường, và một sổ audit đếm sai số lần vi
    -- phạm thì còn tệ hơn là không có sổ.
    event_id varchar(64) not null,
    exam_session_id uuid not null,
    -- Nullable: bên phát không phải lúc nào cũng biết thí sinh nào (đường AI nối thẳng chỉ cầm exam
    -- attempt id). vox-streaming tra bù được phần lớn, nhưng để rỗng vẫn tốt hơn là điền id sai --
    -- một candidate_id sai sẽ gắn vi phạm này sang hồ sơ của người khác.
    candidate_id uuid,
    stream_id varchar(64),
    stream_type varchar(20),
    -- Ba cột dưới đây CỐ Ý không có check constraint. Từ vựng của chúng do một service khác định
    -- nghĩa, nên một giá trị mới ở phía vox-streaming mà bị check chặn sẽ làm consumer ném lỗi, retry
    -- rồi rơi vào DLT -- tức là mất đúng cảnh báo mà ta đang cố lưu. Với một sổ nhận dữ liệu từ
    -- thượng nguồn, chấp nhận giá trị lạ rồi chuẩn hoá lúc đọc là an toàn hơn.
    alert_type varchar(64) not null,
    level varchar(16),
    source varchar(32),
    detail varchar(1024),
    confidence numeric(5,4),
    sequence_no bigint,
    -- captured_at là thời điểm SỰ VIỆC xảy ra (theo đồng hồ nơi phát hiện), raised_at là thời điểm
    -- nó được phát đi. Giữ cả hai vì cái đầu mới là thứ dùng để tua video, còn cái sau giải thích độ
    -- trễ khi hai mốc lệch nhau.
    captured_at timestamp(6) with time zone not null,
    raised_at timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_exam_proctoring_alert_event_id unique (event_id)
);

-- Đường đọc chính của cả hai màn: mọi cảnh báo của một phiên thi, theo thứ tự thời gian.
create index idx_exam_proctoring_alert_session on exam_proctoring_alerts (exam_session_id, captured_at);
-- Tra theo thí sinh, cho trường hợp một thí sinh có nhiều phiên (thi lại, nối lại sau sự cố).
create index idx_exam_proctoring_alert_candidate on exam_proctoring_alerts (candidate_id, captured_at);
