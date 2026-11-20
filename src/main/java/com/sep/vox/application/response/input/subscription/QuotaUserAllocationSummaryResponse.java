package com.sep.vox.application.response.input.subscription;

import java.util.List;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;

/**
 * Màn quản trị trường chia hạn mức cá nhân: ví cấp trường đang có gì, và trường đã chia cho từng
 * người bao nhiêu.
 *
 * <p>Luôn trả CẢ HAI vì một mình danh sách allocation không đọc được: "giáo viên A còn 200.000đ"
 * chỉ có nghĩa khi biết ví EXAM của trường còn bao nhiêu -- tổng phần chia cho từng người KHÔNG bị
 * ép bằng dung lượng ví (xem DistributeQuotaToUsersService), nên hai con số này lệch nhau là
 * chuyện bình thường và chính chỗ lệch mới là thứ người dùng cần nhìn.
 *
 * @param pool        ví cấp trường -- EXAM khi chia cho giáo viên, PRACTICE khi chia cho học sinh
 * @param allocations chỉ những người ĐÃ được cấp hạn mức riêng; không có tên trong đây nghĩa là
 *                    không bị chặn theo cá nhân, chỉ ví của trường áp dụng
 */
public record QuotaUserAllocationSummaryResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    List<SchoolSubscriptionQuotaUserAllocationDto> allocations
) { 
}
