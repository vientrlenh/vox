-- =============================================================================
-- V13: mang tiền tự nạp sang kỳ mới ở ĐÚNG RANH GIỚI, không phải lúc trả tiền.
--
-- LỖI V12 ĐỂ HỞ. seedQuotaRecords chụp ảnh phần tiền tự nạp chưa tiêu của kỳ cũ ngay tại lúc CHỐT ĐƠN,
-- nhưng nextPeriodStart cho kỳ mới bắt đầu ở endDate của kỳ cũ. Gia hạn sớm thì hai mốc đó cách nhau
-- -- gói năm gia hạn trước một tháng là một tháng chênh -- và trong quãng đó kỳ CŨ vẫn là kỳ
-- findActiveBySchoolId trả về, ví hạn mức của nó vẫn tiêu được, vẫn nạp thêm được. Cả hai chiều đều
-- hỏng, và hỏng vào TIỀN THẬT:
--
--   NHÂN ĐÔI. Trường nạp 5tr tháng 6, gia hạn sớm tháng 11 -> kỳ mới được seed sẵn 5tr mang sang.
--   Tháng 12 họ tiêu hết 5tr đó trên kỳ cũ. Ngày 1/1 kỳ mới vẫn mở ra với 5tr. Một lần nạp, hai lần
--   tiêu được.
--
--   BỐC HƠI. Cũng trường đó, tháng 12 nạp thêm 3tr thay vì tiêu. Khoản này rơi vào ví của KỲ CŨ (kỳ
--   duy nhất đang hiệu lực), không có trong ảnh chụp đã lấy từ tháng 11, và chết theo kỳ cũ ở ranh
--   giới -- đúng lỗi mà V12 sinh ra để chặn, chỉ thu hẹp vào quãng gia hạn sớm.
--
-- KHÔNG vá được bằng cách chụp ảnh kỹ hơn: mọi con số tính TRƯỚC lúc kỳ cũ ngừng tiêu được đều có thể
-- bị chính kỳ cũ làm sai đi sau đó. Phép mang sang phải chạy ở ranh giới.
--
-- CÁCH LÀM Ở ĐÂY: lúc chốt đơn, kỳ tương lai được seed với ĐÚNG định mức gói (funded = 0) cộng một
-- CÁI HẸN -- cột dưới đây, trỏ về kỳ nguồn. SubscriptionExpiryJob (đã chạy mỗi giờ) thấy kỳ đã tới
-- ngày bắt đầu thì đọc phần chưa tiêu của kỳ nguồn LÚC ĐÓ, cộng vào, rồi xoá cái hẹn.
--
-- VÌ SAO LÀ MỘT CỘT chứ không phải "tính lại mỗi lần job chạy": tính lại là phép GÁN, mà giữa hai lần
-- job chạy trường có thể đã tự nạp thêm vào chính ví đó (FundQuotaFromBalanceUseCase cộng cả total lẫn
-- funded). Gán đè sẽ xoá mất khoản vừa nạp. Cột này biến phép mang sang thành ĐÚNG MỘT LẦN: cộng thêm
-- (addFundingFromBalance, giữ nguyên mọi thứ đã có) rồi xoá hẹn, nên job chạy lại bao nhiêu lần cũng
-- không cộng lần hai, và job chạy muộn thì chỉ là tiền hiện ra muộn chứ không sai.
--
-- Gia hạn khi kỳ cũ ĐÃ hết hạn thì mốc chốt sổ trùng mốc bắt đầu, không có quãng hở nào để mà lỡ --
-- ca đó vẫn mang sang ngay tại chỗ và KHÔNG đặt hẹn.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Cái hẹn. NULL = không có gì phải làm, và đó là trạng thái của gần như mọi dòng: chỉ kỳ sinh ra từ
-- một lần gia hạn SỚM mới mang giá trị, và chỉ mang cho tới lần job kế tiếp.
--
-- Trỏ tới KỲ NGUỒN chứ không lưu sẵn SỐ TIỀN, vì số tiền chính là thứ chưa được phép chốt: chốt sẵn
-- một con số ở đây là lặp lại đúng cái ảnh chụp quá sớm mà V13 sinh ra để bỏ.
-- -----------------------------------------------------------------------------
ALTER TABLE school_subscription_quota_records
    ADD COLUMN carry_funding_from_subscription_id uuid;

-- FK chứ không phải uuid trần: kỳ nguồn bị xoá mà cái hẹn còn trỏ tới nó thì job sẽ đọc ra rỗng và
-- lặng lẽ mang sang 0đ -- mất tiền, không có lỗi nào nổi lên.
ALTER TABLE school_subscription_quota_records
    ADD CONSTRAINT fk_school_subscription_quota_records_carry_from
        FOREIGN KEY (carry_funding_from_subscription_id) REFERENCES school_subscriptions(id);

-- Tự trỏ vào chính mình nghĩa là job sẽ cộng phần chưa tiêu của một ví vào chính nó, mỗi lần chạy một
-- lần, cho tới khi chạm trần CHECK của V12. Chặn ở đây vì đó là một lỗi lập trình, không phải dữ liệu.
ALTER TABLE school_subscription_quota_records
    ADD CONSTRAINT chk_school_subscription_quota_records_carry_not_self CHECK (
        carry_funding_from_subscription_id IS NULL
        OR carry_funding_from_subscription_id <> school_subscription_id);

-- Partial index: job quét mỗi giờ và hỏi đúng một câu -- "còn cái hẹn nào chưa làm không". Gần như
-- luôn là không, nên index chỉ chứa vài dòng đang treo thay vì cả bảng.
CREATE INDEX idx_school_subscription_quota_records_pending_carry
    ON school_subscription_quota_records (carry_funding_from_subscription_id)
    WHERE carry_funding_from_subscription_id IS NOT NULL;
