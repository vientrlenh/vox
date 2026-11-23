package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.domain.common.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchSchoolGradingFailuresQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailurePageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailureResponse;

/**
 * Chỗ đáp của dòng "AI chấm lỗi, chưa ai xử lý" trên trang tổng quan.
 *
 * <p>Phạm vi lấy từ NGƯỜI ĐANG ĐĂNG NHẬP, không nhận schoolId từ client — cùng cách như
 * {@code ViewSchoolAdminDashboardUseCase}. Nhận từ client là mở đường cho quản trị trường này đọc
 * bài của trường khác.
 *
 * <p>KHÔNG có hành động hàng loạt ở đây. Chấm lại bằng AI và chuyển người chấm đều là quyết định
 * theo TỪNG bài — chúng phụ thuộc vào lịch chấm, người rảnh, và định mức một lượt của chính phiên
 * đó. Hai mutation lẻ đã có sẵn và màn này chỉ dẫn tới chúng.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolGradingFailuresUseCase
        implements IUseCase<SearchSchoolGradingFailuresQuery, SchoolGradingFailurePageResponse> {

    private final UserContextPort userContextPort;
    private final SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolGradingFailurePageResponse execute(SearchSchoolGradingFailuresQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        PageResult<SchoolGradingFailureDto> page = schoolWorkloadQueryRepository.findUnhandledAiFailures(
            schoolId, input.assessmentId(), input.retryLeft(), input.page(), input.size());
        int[] counts = schoolWorkloadQueryRepository.countAiFailuresByAllowance(schoolId, input.assessmentId());

        return new SchoolGradingFailurePageResponse(
            page.content().stream().map(SchoolGradingFailureResponse::of).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            counts[0],
            counts[1]
        );
    }
}
