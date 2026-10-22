package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolDebtEvent;
import com.sep.vox.domain.shared.PageResult;

/**
 * Sổ audit "nguyên nhân nợ hạn mức AI" -- append-only, chỉ ghi lúc trạng thái nợ ĐỔI. Mirror của
 * {@link SchoolBalanceEntryRepository}: cùng một cái ví, một bên kể tiền đi đâu, một bên kể vì sao
 * trường bị khoá.
 */
public interface SchoolDebtEventRepository {
    SchoolDebtEvent save(SchoolDebtEvent event);

    /**
     * Nhật ký của trường, mới nhất trước. {@code page} đếm TỪ 0, khớp {@code PageRequest.of}; use case đổi từ số trang 1-based của client.
     *
     * <p>Thay cho bản trả {@code List} không phân trang trước đây: sổ này thưa với phần lớn trường
     * nhưng một trường liên tục vượt hạn mức thì không, và đường đọc duy nhất của nó là một trang
     * giao diện có phân trang.
     */
    PageResult<SchoolDebtEvent> findBySchoolId(UUID schoolId, int page, int size);
}
