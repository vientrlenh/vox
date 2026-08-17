-- Nâng lại mức cho hai loại cảnh báo giám sát từng bị xếp nhầm xuống INFO.
--
-- INFO ở đó KHÔNG phải một đánh giá có chủ ý: `DefaultAlertLevel` bên vox-streaming xếp mức bằng một
-- switch theo tên loại, và hai tên này chưa từng có mặt trong switch đó -- phía AI phát ra một bộ
-- tên, phía streaming lại định nghĩa một bộ khác. Chúng rơi vào nhánh `default`, tức là "không nhận
-- ra loại này", rồi được ghi xuống như thể đã được cân nhắc. Sửa lại là chữa lỗi ghi chép, không
-- phải viết lại phán đoán -- đó là lý do việc này chấp nhận được với một sổ bằng chứng.
--
-- Chỉ đụng vào `level`. TÊN loại thì giữ nguyên, kể cả `CRITICAL_VIOLATION` nay đã đổi thành
-- `UNCOOPERATIVE_CANDIDATE` ở phía phát: tên là thứ hệ thống thật sự đã ghi lúc đó, và giao diện đã
-- biết đọc cả tên cũ lẫn tên mới. Đổi tên trong bản ghi cũ mới đúng là viết lại lịch sử.
--
-- Điều kiện `level = 'INFO'` cố ý hẹp: chỉ sửa đúng những dòng mang dấu vết của lỗi trên. Dòng nào
-- đã có mức khác thì ai đó hoặc phiên bản nào đó đã cố ý đặt, và không được đụng vào.

-- Không thấy người trong camera. Cùng mức với WINDOW_FOCUS_LOST: đáng xem lại lúc chấm, không đáng
-- dừng bài -- rời khung hình vài giây có rất nhiều lý do vô hại.
update exam_proctoring_alerts
   set level = 'WARNING'
 where alert_type = 'PERSON_MISSING'
   and level = 'INFO';

-- Thí sinh không hợp tác khi trả lời, AI đã nhắc một lần rồi mới bỏ qua câu hỏi. Vẫn là WARNING chứ
-- không phải CRITICAL dù tên cũ nghe như vậy: đây là phán đoán của LLM về thái độ, không phải bằng
-- chứng gian lận, và bài thi đã tự xử lý xong.
update exam_proctoring_alerts
   set level = 'WARNING'
 where alert_type in ('CRITICAL_VIOLATION', 'UNCOOPERATIVE_CANDIDATE')
   and level = 'INFO';

-- OBJECT_DETECTED cố ý KHÔNG được nâng mức. Loại này gộp cả sách/laptop lẫn bàn phím/chuột vào một
-- tên duy nhất, mà bàn phím với chuột thì luôn có mặt trên bàn thi. Từ đây không còn phân biệt được
-- dòng nào là vật thể cấm thật, nên nâng cả cụm lên sẽ tạo ra một loạt cảnh báo nặng không có căn
-- cứ. Để nguyên INFO là mô tả đúng thứ ta biết về chúng: không đủ thông tin để kết luận. Phía phát
-- đã tách thành PHONE_DETECTED / PROHIBITED_OBJECT nên dữ liệu mới không còn dính vấn đề này.
