-- Nguồn gốc của một phiên bản rubric được tạo bằng cách sao chép.
--
-- Trường sao một bản mẫu của hệ thống về rồi sửa; nếu không ghi lại nó sao từ đâu thì bản sao mồ côi
-- ngay khoảnh khắc vừa tạo. Có cột này thì: hiện được "Bản sao từ Hệ thống · <mã> v<n>" trên trang
-- phiên bản, và về sau phát hiện được khi hệ thống ban hành bản mới hơn bản mà trường đang dùng.
--
-- NULL nghĩa là bản này do chính chủ sở hữu soạn, không sao từ đâu -- đó là trường hợp của mọi dòng
-- đang có, và của mọi rubric hệ thống.
--
-- KHÔNG có khoá ngoại. Bản mẫu ở phía hệ thống có vòng đời riêng và có thể bị lưu trữ hoặc xoá; khi
-- đó bản sao của trường vẫn phải sống tiếp bình thường, vì nó đã là tài sản độc lập. Đây là con trỏ
-- xuất xứ để tra cứu, không phải quan hệ sở hữu.
alter table rubric_versions
    add column source_rubric_version_id uuid;

-- Trả lời "những trường nào đang dùng bản mẫu này", và là đường đọc cho việc đối chiếu bản mới.
-- Partial index vì đại đa số dòng sẽ là NULL.
create index idx_rubric_versions_source
    on rubric_versions (source_rubric_version_id)
    where source_rubric_version_id is not null;
