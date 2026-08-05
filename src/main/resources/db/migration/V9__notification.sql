-- Ba bảng cho tính năng thông báo, tách theo ba vòng đời độc lập:
--   notifications           : nội dung gửi tới một người, có trạng thái đã đọc
--   notification_devices    : token FCM của từng thiết bị
--   notification_preferences: tuỳ chọn kênh nhận theo nhóm

-- notifications ---------------------------------------------------------------
-- payload để JSONB (không phải TEXT) vì đây là bảng sẽ phải soi lúc hỗ trợ người
-- dùng: "vì sao bấm thông báo này không mở đúng màn hình" cần query xuyên vào
-- payload->>'screen'. Cùng lý do với outboxes.payload.
create table notifications (
    id          UUID DEFAULT uuidv7() not null,
    user_id     uuid not null,
    event_id    uuid,
    event_type  varchar(150) not null,
    title       varchar(255) not null,
    body        TEXT,
    payload     JSONB,
    read_at     timestamp(6) with time zone,
    created_at  timestamp(6) with time zone not null,
    primary key (id),
    -- Chặn tạo trùng khi consumer bị giao lại message: việc tạo notification trở
    -- thành idempotent theo cấu trúc, không phụ thuộc processed_events.
    -- event_id cho phép NULL (thông báo tạo tay, không đến từ outbox) và NULL
    -- không xung đột trong unique index của Postgres -- đúng ý muốn ở đây.
    constraint uk_notifications_user_event unique (user_id, event_id)
);

create index idx_notifications_user_created_at on notifications (user_id, created_at);

-- Index riêng cho badge "số thông báo chưa đọc": partial index chỉ chứa dòng chưa
-- đọc nên nhỏ hơn nhiều lần so với index đầy đủ, và không phình theo lịch sử.
create index idx_notifications_user_unread on notifications (user_id) where read_at is null;

-- notification_devices --------------------------------------------------------
-- UNIQUE (token) là ràng buộc quan trọng nhất ở đây, không phải để tối ưu mà để
-- chặn rò rỉ: token FCM gắn với bản cài app trên máy, KHÔNG gắn với tài khoản.
-- Máy phòng lab (ExamDeliveryMode.LAB) được nhiều học sinh dùng chung, nên nếu
-- không có ràng buộc này thì một token có thể trỏ về nhiều user, và thông báo
-- điểm thi của người trước sẽ bay tới màn hình người sau.
-- Luồng đăng ký phải là upsert chuyển chủ: ON CONFLICT (token) DO UPDATE SET user_id = ...
create table notification_devices (
    id            UUID DEFAULT uuidv7() not null,
    user_id       uuid not null,
    device_id     varchar(255) not null,
    platform      varchar(20) not null
        constraint chk_notification_devices_platform_valid
        check (platform IN ('WEB', 'ANDROID', 'IOS')),
    token         varchar(512) not null,
    created_at    timestamp(6) with time zone not null,
    last_seen_at  timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_notification_devices_token unique (token)
);

create index idx_notification_devices_user on notification_devices (user_id);

-- notification_preferences ----------------------------------------------------
-- Chỉ lưu phần KHÁC mặc định. Không có dòng nghĩa là "dùng mặc định" (bật cả hai
-- kênh). Nhờ vậy thêm category mới không cần backfill, user mới không cần tạo dòng,
-- và bảng chỉ chứa những người thực sự đã đổi thiết lập.
-- Câu lọc người nhận: COALESCE(np.push_enabled, TRUE) -- xem NotificationPreference
-- .DEFAULT_PUSH_ENABLED để hai bên không lệch nhau.
--
-- in_app không có ở đây một cách có chủ ý: in-app là sổ ghi chép, push/email là sự
-- ngắt quãng. Chỉ cái ngắt quãng mới cần xin phép.
create table notification_preferences (
    id             UUID DEFAULT uuidv7() not null,
    user_id        uuid not null,
    category       varchar(50) not null
        constraint chk_notification_preferences_category_valid
        check (category IN ('EXAM_RESULT', 'EXAM_APPEAL', 'GRADING', 'EXAM_SCHEDULE', 'SYSTEM')),
    push_enabled   boolean not null,
    email_enabled  boolean not null,
    updated_at     timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_notification_preferences_user_category unique (user_id, category)
);
