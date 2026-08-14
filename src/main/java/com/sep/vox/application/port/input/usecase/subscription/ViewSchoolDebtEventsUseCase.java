package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolDebtEventQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDebtEventDto;

// Cho system admin (hoặc admin của đúng trường) tra lại lịch sử "nguyên nhân nợ hạn mức AI" -- xem
// SchoolDebtEvent/SchoolDebtNotificationService.
@Service
public class ViewSchoolDebtEventsUseCase implements IUseCase<ViewSchoolDebtEventsQuery, PageResult<SchoolDebtEventDto>> {

    private final SchoolDebtEventQueryRepository schoolDebtEventQueryRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolDebtEventsUseCase(
            SchoolDebtEventQueryRepository schoolDebtEventQueryRepository, UserContextPort userContextPort) {
        this.schoolDebtEventQueryRepository = schoolDebtEventQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolDebtEventDto> execute(ViewSchoolDebtEventsQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var page = schoolDebtEventQueryRepository.findAllBySchoolId(input.schoolId(), PageRequest.of(input.page(), input.size()));

        return new PageResult<>(page.getContent(), input.page(), input.size(), page.getTotalElements(), page.getTotalPages());
    }
}
