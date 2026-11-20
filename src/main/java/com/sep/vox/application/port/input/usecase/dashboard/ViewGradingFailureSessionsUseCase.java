package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewGradingFailureSessionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureOverviewUseCase.GradingFailureWindow;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.domain.common.PageResult;

/** Danh sách phiên trong MỘT nhóm nguyên nhân trên trang phân loại phiên chấm lỗi. */
@Service
public class ViewGradingFailureSessionsUseCase
        implements IUseCase<ViewGradingFailureSessionsQuery, PageResult<GradingFailureSessionDto>> {

    private final GradingFailureQueryRepository gradingFailureQueryRepository;

    public ViewGradingFailureSessionsUseCase(GradingFailureQueryRepository gradingFailureQueryRepository) {
        this.gradingFailureQueryRepository = gradingFailureQueryRepository;
    }

    /**
     * KHÔNG kẹp lại page/size ở đây: biên đã gọi {@code PageArguments.validate}, và kẹp lần nữa là
     * đúng thói quen mà javadoc của lớp đó cấm — nó biến một yêu cầu sai thành 200 OK kèm dữ liệu
     * của trang khác.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingFailureSessionDto> execute(ViewGradingFailureSessionsQuery input) {
        var window = GradingFailureWindow.resolve(input.dateFrom(), input.dateTo());

        // Khoảng rỗng/ngược: trang rỗng chứ không phải lỗi, cùng cách xử lý với các query dashboard
        // khác -- người dùng chọn nhầm khoảng thì thấy "không có phiên nào", không thấy màn lỗi.
        if (window.isEmpty()) {
            return new PageResult<>(List.of(), input.page(), input.size(), 0, 0);
        }

        return gradingFailureQueryRepository.findSessions(
            window.from(), window.to(), input.signature(), input.page(), input.size());
    }
}
