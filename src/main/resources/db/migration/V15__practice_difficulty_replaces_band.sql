-- ---------------------------------------------------------------------------
-- Hoc sinh tu chon bac luyen tap
--
-- Cot target_framework_band_id KHONG doi ten, khong doi kieu -- doi duy nhat MOT
-- thu: AI dien no. Truoc day he thong suy ra bac cua hoc sinh tu ba nguon (bac do
-- duoc tu bai cham, EMA hieu nang, lan bo do gan nhat theo chu de). Tu day hoc
-- sinh chon truc tiep truoc moi phien, va bac chi con nghia "do kho toi muon hom
-- nay" chu khong phai mot tuyen bo ve trinh do.
--
-- practice_session da co cot nay. practice_paper thi chua -- ma de duoc dung
-- TRUOC khi phien bat dau, nen lua chon phai duoc luu ngay tu luc dung de, khong
-- thi cau dau tien khong biet nham bac nao.
--
-- Duong THI khong doi: framework_result_bands va assessment_policies giu nguyen.
-- ---------------------------------------------------------------------------

ALTER TABLE practice_paper ADD COLUMN target_framework_band_id UUID;

-- De cu da co phien thi lay lai lua chon tu phien do.
UPDATE practice_paper paper
SET target_framework_band_id = session.target_framework_band_id
FROM practice_session session
WHERE session.practice_paper_id = paper.id
  AND paper.target_framework_band_id IS NULL;

-- Co y de NULLABLE: de dung truoc lan doi nay khong mang lua chon nao cua hoc sinh,
-- va bia ra mot bac cho chung la ghi du lieu sai. StartPracticeSessionPersistenceService
-- gap NULL thi lui ve bac muc tieu cua chinh sach cham -- dung hanh vi cu.
CREATE INDEX idx_practice_paper_target_band ON practice_paper (target_framework_band_id);
