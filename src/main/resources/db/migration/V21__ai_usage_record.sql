-- Lịch sử sử dụng AI (LLM token + dịch vụ tính theo duration như STT/TTS/avatar
-- render) do service Agentic AI báo cáo qua Kafka sau mỗi turn. Đây là bảng audit/
-- COGS: ghi lại giá đã áp dụng tại THỜI ĐIỂM gọi (unit_price_json), không tra giá
-- hiện tại -- vì giá vendor (Anthropic/OpenAI/OpenRouter/STT/TTS) đổi theo thời
-- gian, và một dòng lịch sử phải phản ánh đúng chi phí thực tế lúc phát sinh.
--
-- Tách biệt với token_usage_event: token_usage_event là đơn vị NGHIỆP VỤ (giây quy
-- đổi, trừ vào quota bán cho trường). ai_usage_record là chi phí THẬT trả cho nhà
-- cung cấp AI (USD), phục vụ theo dõi COGS/đối chiếu biên lợi nhuận, không phải
-- nguồn trừ quota.
--
-- usage_type phân biệt hai dạng payload không cùng field:
--   LLM_TOKEN: model_name, input/output/cache tokens có giá trị; duration_ms NULL.
--   DURATION : duration_ms có giá trị (vd: giây audio đưa qua STT); các cột token
--              NULL, model_name có thể NULL nếu dịch vụ không gắn với 1 model cụ thể.
-- unit_price_json lưu JSON thô vì hình dạng giá khác nhau giữa hai loại (per-token
-- theo mtok vs per-second) -- ép về vài cột numeric cố định sẽ mất thông tin hoặc
-- tạo nhiều cột NULL không cần thiết.
create table ai_usage_record (
    id                            UUID DEFAULT uuidv7() not null,
    exam_session_id               uuid not null,
    turn_id                       uuid not null,
    -- Khoá idempotency do Agentic AI sinh cho từng usage_event trong envelope --
    -- một turn có thể có nhiều dòng (STT + LLM eval + LLM follow-up + TTS), nên
    -- không thể dùng turn_id làm khoá duy nhất.
    usage_event_id                uuid not null,
    usage_type                    varchar(20) not null
        constraint chk_ai_usage_record_usage_type_valid
        check (usage_type IN ('LLM_TOKEN', 'DURATION')),
    provider                      varchar(50) not null,
    model_name                    varchar(100),
    input_tokens                  integer,
    output_tokens                 integer,
    cache_creation_input_tokens   integer,
    cache_read_input_tokens       integer,
    duration_ms                   bigint,
    unit_price_json               TEXT not null,
    cost_usd                      numeric(12,6) not null,
    occurred_at                   timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_ai_usage_record_usage_event_id unique (usage_event_id)
);

create index idx_ai_usage_record_exam_session on ai_usage_record (exam_session_id);
create index idx_ai_usage_record_turn on ai_usage_record (turn_id);