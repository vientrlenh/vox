package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;

/**
 * Trần phân phối hạn mức theo TRƯỜNG và theo từng loại hạn mức.
 *
 * <p>Không có dòng nào = chia được toàn bộ ví. Đường đọc vì thế không bao giờ trả Optional rỗng ra
 * ngoài: nó dựng bản mặc định tại chỗ, y như cách số dư ví dựng bản 0 đồng cho trường chưa từng nạp.
 */
public interface SchoolQuotaPolicyRepository {

    /** Không bao giờ null -- trường chưa đặt gì nhận bản mặc định 1.0, KHÔNG ghi xuống DB. */
    SchoolQuotaPolicy findBySchoolIdAndQuotaType(UUID schoolId, QuotaType quotaType);

    List<SchoolQuotaPolicy> findBySchoolId(UUID schoolId);

    /** Tạo dòng nếu chưa có, ghi đè tỷ lệ nếu đã có. */
    SchoolQuotaPolicy upsertRatio(UUID schoolId, QuotaType quotaType, BigDecimal distributableRatio);
}
