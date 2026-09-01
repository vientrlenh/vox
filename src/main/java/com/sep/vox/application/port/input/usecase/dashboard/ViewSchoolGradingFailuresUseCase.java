package com.sep.vox.application.port.input.usecase.dashboard;

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
public class ViewSchoolGradingFailuresUseCase
        implements IUseCase<SearchSchoolGradingFailuresQuery, SchoolGradingFailurePageResponse> {

    private final UserContextPort userContextPort;
    private final SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;

    public ViewSchoolGradingFailuresUseCase(UserContextPort userContextPort,
            SchoolWorkloadQueryRepository schoolWorkloadQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolWorkloadQueryRepository = schoolWorkloadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolGradingFailurePageResponse execute(SearchSchoolGradingFailuresQuery input) {
        var schoolId = userContextPort.getCurrentSchoolId();

        var page = schoolWorkloadQueryRepository.findUnhandledAiFailures(
            schoolId, input.examId(), input.retryLeft(), input.page(), input.size());
        var counts = schoolWorkloadQueryRepository.countAiFailuresByAllowance(schoolId, input.examId());

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
