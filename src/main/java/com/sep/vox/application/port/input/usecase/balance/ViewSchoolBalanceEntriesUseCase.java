package com.sep.vox.application.port.input.usecase.balance;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolBalanceEntriesQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/** Sao kê ví của một trường, mới nhất trước. */
@Service
public class ViewSchoolBalanceEntriesUseCase
        implements IUseCase<ViewSchoolBalanceEntriesQuery, PageResult<SchoolBalanceEntryDto>> {

    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    public ViewSchoolBalanceEntriesUseCase(
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolScopedReadGuard schoolScopedReadGuard) {
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolScopedReadGuard = schoolScopedReadGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolBalanceEntryDto> execute(ViewSchoolBalanceEntriesQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        var page = schoolBalanceEntryRepository.findBySchoolId(
            input.schoolId(),
            parseEntryType(input.entryType()),
            // Repository đòi hai mốc khác null (xem javadoc của nó). "Không chặn đầu nào" quy về cả
            // dải lịch sử ở đây, thay vì đẩy một tham số null xuống JPQL.
            input.from() == null ? Instant.EPOCH : input.from(),
            input.to() == null ? Instant.now() : input.to(),
            input.page(),
            input.size()
        );

        return new PageResult<>(
            page.content().stream().map(SchoolBalanceEntryDto::toDto).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    /**
     * GraphQL đã chặn giá trị lạ ở tầng schema (enum SchoolBalanceEntryType), nên tới đây chỉ còn
     * null hoặc một tên hợp lệ. Vẫn bắt IllegalArgumentException để đường gọi khác -- test, hay một
     * adapter REST sau này -- không nổ ra một lỗi khó đọc từ tận trong enum.
     */
    private static SchoolBalanceEntryType parseEntryType(String entryType) {
        if (entryType == null || entryType.isBlank()) {
            return null;
        }
        try {
            return SchoolBalanceEntryType.valueOf(entryType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại bút toán không hợp lệ: " + entryType);
        }
    }
}
