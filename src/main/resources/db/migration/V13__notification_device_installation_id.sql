-- Đổi notification_devices.token -> installation_id.
--
-- Lý do: firebase-admin 9.10.0 đã @Deprecated Message.Builder.setToken /
-- MulticastMessage.Builder.addToken|addAllTokens và thay bằng setFid / addFid /
-- addAllFids. Giá trị phía client cũng đổi theo: không còn là FCM registration
-- token (FCM SDK getToken()) mà là Firebase installation ID (Installations SDK
-- getId()). Hai giá trị khác nhau hoàn toàn, nên giữ tên cột cũ sẽ gây hiểu nhầm
-- ở mọi chỗ đọc bảng này về sau.
--
-- Ngữ nghĩa UNIQUE không đổi: FID cũng gắn với một bản cài app trên một máy, nên
-- ràng buộc chống rò rỉ thông báo giữa các học sinh dùng chung máy phòng lab
-- (ExamDeliveryMode.LAB) vẫn giữ nguyên tác dụng. Xem V9__notification.sql.

alter table notification_devices
    rename column token to installation_id;

-- RENAME CONSTRAINT đổi luôn tên index nền bên dưới, không cần drop/create.
alter table notification_devices
    rename constraint uk_notification_devices_token to uk_notification_devices_installation_id;

-- FID là chuỗi ~22 ký tự, không phải registration token dài ~160+. Thu varchar(512)
-- -> varchar(50) để cột phản ánh đúng thứ nó chứa.
-- Lệnh này sẽ lỗi (chứ không cắt cụt âm thầm) nếu còn dòng dài hơn 50 ký tự -- đó
-- là hành vi mong muốn: dòng như vậy là registration token cũ, cần xử lý có ý thức
-- chứ không nên để mất dữ liệu.
alter table notification_devices
    alter column installation_id type varchar(50);

-- Postgres 17+ đặt tên cho cả ràng buộc NOT NULL, và RENAME COLUMN không đổi theo,
-- nên còn sót lại cái tên ..._token_not_null trỏ vào cột đã đổi tên. Không ảnh hưởng
-- chức năng, chỉ gây khó hiểu khi đọc \d về sau.
-- Bọc trong DO để migration không gãy nếu ràng buộc không tồn tại dưới đúng tên đó.
do $$
begin
    if exists (
        select 1 from pg_constraint
        where conrelid = 'notification_devices'::regclass
          and conname = 'notification_devices_token_not_null'
    ) then
        alter table notification_devices
            rename constraint notification_devices_token_not_null
                to notification_devices_installation_id_not_null;
    end if;
end $$;
