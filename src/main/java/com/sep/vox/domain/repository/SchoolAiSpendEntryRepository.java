package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.school.SchoolAiSpendEntry;

/**
 * Sổ chỉ-ghi-thêm: chỉ có {@code save}, không có sửa, không có xoá.
 *
 * <p>Đường ĐỌC không đi qua đây mà qua {@code SchoolAiCostQueryRepository} — mọi màn hình đều hỏi số
 * đã gom nhóm (theo ngày, theo người), không bao giờ hỏi từng dòng lẻ, nên nạp aggregate về Java rồi
 * cộng ở đó là kéo hàng vạn dòng để in ra ba mươi con số.
 */
public interface SchoolAiSpendEntryRepository {

    SchoolAiSpendEntry save(SchoolAiSpendEntry entry);
}
