package com.sep.vox.application.port.input.usecase.balance;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.repository.SchoolBalanceRepository;

/**
 * Số dư ví của một trường.
 *
 * <p>Không bao giờ trả null: trường chưa từng chạm vào ví nhận một bản 0 đồng dựng tại chỗ -- xem
 * {@link SchoolBalanceDto#emptyFor}.
 */
@Service
public class ViewSchoolBalanceUseCase implements IUseCase<UUID, SchoolBalanceDto> {

    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    public ViewSchoolBalanceUseCase(
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolScopedReadGuard schoolScopedReadGuard) {
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolScopedReadGuard = schoolScopedReadGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolBalanceDto execute(UUID schoolId) {
        schoolScopedReadGuard.requireCanRead(schoolId);

        // findBySchoolId, KHÔNG phải findBySchoolIdForUpdateOrCreate: bản kia khoá dòng và TẠO nếu
        // chưa có, nên dùng ở đường đọc sẽ đẻ ra một dòng số dư rỗng cho mọi trường chỉ vì có người
        // mở trang -- và làm thế trong một transaction readOnly.
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(SchoolBalanceDto::toDto)
            .orElseGet(() -> SchoolBalanceDto.emptyFor(schoolId));
    }
}
